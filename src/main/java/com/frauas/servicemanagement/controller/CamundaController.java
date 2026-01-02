package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.CamundaProcessService;
import com.frauas.servicemanagement.service.ProviderOfferService;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Optional;

@Controller
@RequestMapping("/camunda")
public class CamundaController {

    private static final Logger LOG = LoggerFactory.getLogger(CamundaController.class);

    @Autowired private CamundaProcessService camundaProcessService;
    @Autowired private ProviderOfferService providerOfferService;
    @Autowired private ServiceRequestService serviceRequestService;
    @Autowired private RuntimeService runtimeService;

    // --- TASK LIST ---
    @GetMapping("/tasks")
    public String getAllTasks(Model model) {
        return getTasksForAssignee("all", model);
    }

    @GetMapping("/tasks/{assignee}")
    public String getTasksForAssignee(@PathVariable String assignee, Model model) {
        List<Task> tasks = assignee.equals("all") ? camundaProcessService.getAllActiveTasks() : camundaProcessService.getTasksForAssignee(assignee);

        // Dynamic Naming for clarity
        tasks.forEach(t -> {
            if ("Activity_PM_Fix".equals(t.getTaskDefinitionKey())) {
                t.setName("Action Required (Rejected)");
            }
        });

        model.addAttribute("tasks", tasks);
        model.addAttribute("viewTitle", "Inbox: " + assignee);

        if (assignee.contains("pm")) {
            model.addAttribute("drafts", serviceRequestService.getServiceRequestsByStatus(ServiceRequestStatus.DRAFT));
        }
        return "camunda-tasks";
    }

    // --- TASK DETAILS (THE FIX) ---
    @GetMapping("/task/{taskId}")
    public String getTaskDetails(@PathVariable String taskId, Model model) {
        Task task = camundaProcessService.getTaskById(taskId);
        if (task == null) return "redirect:/camunda/tasks/pm_user";

        // ✅ FIX: Define variables consistently
        String pId = task.getProcessInstanceId();
        String taskKey = task.getTaskDefinitionKey(); // Was 'key' in previous version, causing error

        // 1. Load Request
        Long requestId = getVariableSafe(pId, "requestId", Long.class);
        model.addAttribute("requestId", requestId);
        if (requestId != null) {
            serviceRequestService.getServiceRequestById(requestId).ifPresent(req -> model.addAttribute("requestDetails", req));
        }

        // 2. Load History
        model.addAttribute("commentHistory", getVariableSafe(pId, "commentHistory", String.class));

        // 3. Load Data based on Step
        if ("Activity_PM_Evaluate".equals(taskKey) && requestId != null) {
            List<ProviderOffer> offers = providerOfferService.getOffersByServiceRequest(requestId);
            model.addAttribute("offers", offers);

            // Show rejection reason if looping back
            String reason = getVariableSafe(pId, "rejectionReason", String.class);
            if (reason != null) {
                model.addAttribute("rejectionReason", reason);
                // Visual cue
                task.setName(task.getName() + " (Redo Selection)");
            }
        }

        if ("Activity_PO_Validate".equals(taskKey) || "Activity_RP_Coordination".equals(taskKey)) {
            Long offerId = getVariableSafe(pId, "selectedOfferId", Long.class);
            model.addAttribute("selectionReason", getVariableSafe(pId, "selectionReason", String.class));

            if (offerId != null) {
                providerOfferService.getOfferById(offerId).ifPresent(o -> model.addAttribute("selectedOffer", o));
            }
        }

        if ("Activity_PM_Fix".equals(taskKey)) {
            model.addAttribute("rejectionReason", getVariableSafe(pId, "rejectionReason", String.class));
            task.setName("Fix Rejection");
        }

        model.addAttribute("task", task);
        return "camunda-task-details";
    }

    // --- COMPLETE TASK ---
    @PostMapping("/task/{taskId}/complete")
    public String completeTask(@PathVariable String taskId, @RequestParam Map<String, String> formData, Authentication auth) {
        Task task = camundaProcessService.getTaskById(taskId);
        if (task == null) return "redirect:/dashboard";

        String pId = task.getProcessInstanceId();
        String taskKey = task.getTaskDefinitionKey();
        Long requestId = getVariableSafe(pId, "requestId", Long.class);

        // 1. Chat History Log
        String newComment = "";
        String role = (auth != null) ? auth.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "") : "User";

        if (formData.get("rejectionReason") != null && !formData.get("rejectionReason").isEmpty()) {
            newComment = "🔴 REJECTED by " + role + ": " + formData.get("rejectionReason");
        } else if (formData.get("pmJustification") != null && !formData.get("pmJustification").isEmpty()) {
            newComment = "🔵 RESUBMITTED by PM: " + formData.get("pmJustification");
        } else if (formData.get("selectionReason") != null && !formData.get("selectionReason").isEmpty()) {
            newComment = "🟢 SELECTED by PM: " + formData.get("selectionReason");
        }

        if (!newComment.isEmpty()) {
            String existing = getVariableSafe(pId, "commentHistory", String.class);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
            runtimeService.setVariable(pId, "commentHistory", (existing == null ? "" : existing) + "[" + timestamp + "] " + newComment + "\n");
        }

        // 2. Status Updates
        if (requestId != null) {
            if ("Activity_PO_Approval".equals(taskKey)) {
                if ("false".equals(formData.get("approved"))) serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.REJECTED);
                else serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.PUBLISHED);
            }
            else if ("Activity_PM_Fix".equals(taskKey)) {
                serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.WAITING_APPROVAL);
            }
            else if ("Activity_PM_Evaluate".equals(taskKey)) {
                serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.SELECTED);
            }
            else if ("Activity_PO_Validate".equals(taskKey) && "false".equals(formData.get("verified"))) {
                serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.REJECTED);
                // Flag to ensure PM sees "Redo Selection"
                runtimeService.setVariable(pId, "rejectedBy", "PO (Contract)");
            }
        }

        // 3. Technical Scores
        formData.forEach((k, v) -> {
            if (k.startsWith("techScore_")) providerOfferService.updateTechnicalScore(Long.parseLong(k.split("_")[1]), Double.parseDouble(v));
        });

        // 4. Submit to Camunda
        Map<String, Object> vars = new HashMap<>();
        formData.forEach((k, v) -> {
            if (!k.startsWith("techScore_") && !"_csrf".equals(k)) {
                if ("true".equals(v) || "false".equals(v)) vars.put(k, Boolean.valueOf(v));
                else if (v.matches("-?\\d+")) vars.put(k, Long.valueOf(v));
                else vars.put(k, v);
            }
        });

        camundaProcessService.completeTask(taskId, vars);
        return "redirect:/camunda/tasks/" + (auth != null ? auth.getName() : "all");
    }

    private <T> T getVariableSafe(String id, String name, Class<T> type) {
        Object val = runtimeService.getVariable(id, name);
        if (val == null) return null;
        if (type == Long.class && val instanceof Integer) return type.cast(((Integer) val).longValue());
        return type.cast(val);
    }
}