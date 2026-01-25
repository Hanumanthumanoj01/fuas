package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.CamundaProcessService;
import com.frauas.servicemanagement.service.ProviderOfferService;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.task.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/camunda")
public class CamundaController {

    @Autowired private CamundaProcessService camundaProcessService;
    @Autowired private ProviderOfferService providerOfferService;
    @Autowired private ServiceRequestService serviceRequestService;
    @Autowired private RuntimeService runtimeService;

    // =====================================================
    // TASK LIST — HUMAN READABLE NAMES
    // =====================================================
    @GetMapping("/tasks")
    public String getAllTasks(Model model) {
        return getTasksForAssignee("all", model);
    }

    @GetMapping("/tasks/{assignee}")
    public String getTasksForAssignee(@PathVariable String assignee, Model model) {

        List<Task> tasks = assignee.equals("all")
                ? camundaProcessService.getAllActiveTasks()
                : camundaProcessService.getTasksForAssignee(assignee);

        tasks.forEach(task -> {
            String pid = task.getProcessInstanceId();
            String title = (String) runtimeService.getVariable(pid, "title");

            if (title == null || title.isBlank()) {
                Long rid = getVariableSafe(pid, "requestId", Long.class);
                title = rid != null ? "Request " + rid : "Service Request";
            }

            String action;
            switch (task.getTaskDefinitionKey()) {
                case "Activity_PM_Fix": action = "Fix Rejection"; break;
                case "Activity_PM_Evaluate": action = "Evaluate Offers"; break;
                case "Activity_PO_Approval": action = "Approve Request"; break;
                case "Activity_PO_Validate": action = "Validate Selection"; break;
                case "Activity_RP_Coordination": action = "Confirm Logistics"; break;
                default: action = task.getName();
            }

            task.setName(action + " – " + title);
        });

        model.addAttribute("tasks", tasks);
        model.addAttribute("viewTitle", "Inbox: " + assignee);
        return "camunda-tasks";
    }

    // =====================================================
    // TASK DETAILS
    // =====================================================
    @GetMapping("/task/{taskId}")
    public String getTaskDetails(@PathVariable String taskId, Model model) {

        Task task = camundaProcessService.getTaskById(taskId);
        if (task == null) return "redirect:/camunda/tasks";

        String pid = task.getProcessInstanceId();
        String key = task.getTaskDefinitionKey();
        Long requestId = getVariableSafe(pid, "requestId", Long.class);

        model.addAttribute("task", task);
        model.addAttribute("requestId", requestId);

        if (requestId != null) {
            serviceRequestService.getServiceRequestById(requestId)
                    .ifPresent(req -> model.addAttribute("requestDetails", req));
        }

        model.addAttribute(
                "commentHistory",
                getVariableSafe(pid, "commentHistory", String.class)
        );

        if ("Activity_PM_Evaluate".equals(key) && requestId != null) {
            model.addAttribute(
                    "offers",
                    providerOfferService.getOffersByServiceRequest(requestId)
            );
        }

        if ("Activity_PO_Validate".equals(key) || "Activity_RP_Coordination".equals(key)) {
            Long offerId = getVariableSafe(pid, "selectedOfferId", Long.class);
            if (offerId != null) {
                providerOfferService.getOfferById(offerId)
                        .ifPresent(o -> model.addAttribute("selectedOffer", o));
            }
        }

        model.addAttribute(
                "rejectionReason",
                getVariableSafe(pid, "rejectionReason", String.class)
        );

        return "camunda-task-details";
    }

    // =====================================================
    // COMPLETE TASK (CRITICAL FIX HERE)
    // =====================================================
    @PostMapping("/task/{taskId}/complete")
    public String completeTask(
            @PathVariable String taskId,
            @RequestParam Map<String, String> formData,
            Authentication auth) {

        Task task = camundaProcessService.getTaskById(taskId);
        if (task == null) return "redirect:/camunda/tasks";

        String pid = task.getProcessInstanceId();
        String key = task.getTaskDefinitionKey();
        Long requestId = getVariableSafe(pid, "requestId", Long.class);

        // ---------- COMMENT HISTORY ----------
        String role = auth != null
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "USER";

        // Call the updated helper
        String comment = buildComment(formData, role);

        if (!comment.isEmpty()) {
            String history = getVariableSafe(pid, "commentHistory", String.class);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));

            runtimeService.setVariable(
                    pid,
                    "commentHistory",
                    (history == null ? "" : history) + "[" + ts + "] " + comment + "\n"
            );
        }

        Map<String, Object> vars = new HashMap<>();

        if (requestId != null) {

            // ---- PO APPROVAL ----
            if ("Activity_PO_Approval".equals(key)) {
                boolean approved = Boolean.parseBoolean(formData.get("approved"));
                runtimeService.setVariable(pid, "approved", approved);
                vars.put("approved", approved);

                if (!approved) {
                    String reason = formData.get("rejectionReason");
                    runtimeService.setVariable(pid, "rejectionReason", reason);
                    serviceRequestService.updateServiceRequestStatus(
                            requestId, ServiceRequestStatus.NEEDS_CORRECTION);
                } else {
                    serviceRequestService.updateServiceRequestStatus(
                            requestId, ServiceRequestStatus.PUBLISHED);
                }
            }

            // ---- PM EVALUATE ----
            if ("Activity_PM_Evaluate".equals(key)) {
                Long selectedOfferId = Long.valueOf(formData.get("selectedOfferId"));
                runtimeService.setVariable(pid, "selectedOfferId", selectedOfferId);
                vars.put("selectedOfferId", selectedOfferId);

                serviceRequestService.updateServiceRequestStatus(
                        requestId, ServiceRequestStatus.UNDER_EVALUATION);
            }

            // ---- PO VALIDATE ----
            if ("Activity_PO_Validate".equals(key)) {
                boolean verified = Boolean.parseBoolean(formData.get("verified"));

                runtimeService.setVariable(pid, "selectionApproved", verified);
                vars.put("selectionApproved", verified);

                if (!verified) {
                    String reason = formData.get("rejectionReason");
                    runtimeService.setVariable(pid, "rejectionReason", reason);

                    serviceRequestService.updateServiceRequestStatus(
                            requestId, ServiceRequestStatus.OFFERS_RECEIVED);
                } else {
                    serviceRequestService.updateServiceRequestStatus(
                            requestId, ServiceRequestStatus.SELECTED_UNDER_VERIFICATION);
                }
            }

            // ---- PM FIX ----
            if ("Activity_PM_Fix".equals(key)) {
                serviceRequestService.updateServiceRequestStatus(
                        requestId, ServiceRequestStatus.WAITING_APPROVAL);
            }
        }

        // ---- TECHNICAL SCORES ----
        formData.forEach((k, v) -> {
            if (k.startsWith("techScore_")) {
                providerOfferService.updateTechnicalScore(
                        Long.parseLong(k.split("_")[1]),
                        Double.parseDouble(v)
                );
            }
        });

        camundaProcessService.completeTask(taskId, vars);

        return "redirect:/camunda/tasks/" +
                (auth != null ? auth.getName() : "all");
    }

    // =====================================================
    // HELPERS (FIXED LOGIC)
    // =====================================================
    private String buildComment(Map<String, String> data, String role) {

        // 1. REJECTION (Prioritize this)
        if (data.containsKey("rejectionReason") && !data.get("rejectionReason").isBlank()) {
            return "🔴 REJECTED by " + role + ": " + data.get("rejectionReason");
        }

        // 2. RESUBMISSION
        if (data.containsKey("pmJustification") && !data.get("pmJustification").isBlank()) {
            return "🔵 RESUBMITTED by PM: " + data.get("pmJustification");
        }

        // 3. SELECTION
        if (data.containsKey("selectionReason") && !data.get("selectionReason").isBlank()) {
            return "🟢 SELECTED by PM: " + data.get("selectionReason");
        }

        // 4. APPROVAL (This was missing!)
        if ("true".equalsIgnoreCase(data.get("approved"))) {
            return "🟢 APPROVED by " + role;
        }

        // 5. VERIFICATION (PO Validation step)
        if ("true".equalsIgnoreCase(data.get("verified"))) {
            return "🟢 VERIFIED by " + role;
        }

        return ""; // Default no comment
    }

    private <T> T getVariableSafe(String pid, String name, Class<T> type) {
        Object val = runtimeService.getVariable(pid, name);
        if (val == null) return null;

        if (type == Long.class && val instanceof Integer) {
            return type.cast(((Integer) val).longValue());
        }

        return type.cast(val);
    }
}