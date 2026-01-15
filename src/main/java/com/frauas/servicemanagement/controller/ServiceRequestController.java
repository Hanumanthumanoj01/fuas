package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.CamundaProcessService;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/service-requests")
public class ServiceRequestController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private CamundaProcessService camundaProcessService;

    // =====================================================
    // PM DASHBOARD (SINGLE ENTRY POINT)
    // =====================================================
    @GetMapping
    public String getPMDashboard(Model model) {

        // 1. DATABASE RECORDS (FULL HISTORY)
        List<ServiceRequest> allRequests =
                serviceRequestService.getAllServiceRequests();

        model.addAttribute("serviceRequests", allRequests);
        model.addAttribute("newRequest", new ServiceRequest());

        // 2. ACTIVE CAMUNDA TASKS (ACTION ITEMS)
        List<Task> pmTasks =
                camundaProcessService.getTasksForAssignee("pm_user");
        model.addAttribute("activeTasks", pmTasks);

        // 3. KPI COUNTERS
        long draftCount = allRequests.stream()
                .filter(r -> r.getStatus() == ServiceRequestStatus.DRAFT)
                .count();

        long activeCount = allRequests.stream()
                .filter(r ->
                        r.getStatus() != ServiceRequestStatus.DRAFT &&
                                r.getStatus() != ServiceRequestStatus.COMPLETED)
                .count();

        long completedCount = allRequests.stream()
                .filter(r -> r.getStatus() == ServiceRequestStatus.COMPLETED)
                .count();

        model.addAttribute("draftCount", draftCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);

        return "service-requests";
    }

    // =====================================================
    // CREATE SERVICE REQUEST (DRAFT)
    // =====================================================
    @PostMapping
    public String createServiceRequest(
            @ModelAttribute ServiceRequest serviceRequest) {

        serviceRequestService.createServiceRequest(serviceRequest);
        return "redirect:/service-requests";
    }

    // =====================================================
    // START CAMUNDA PROCESS (ONLY AFTER FINAL CHECK)
    // =====================================================
    @PostMapping("/{id}/process")
    public String startProcessForDraft(@PathVariable Long id) {

        serviceRequestService.startProcessForRequest(id);
        return "redirect:/service-requests";
    }

    // =====================================================
    // EDIT SERVICE REQUEST (DRAFT ONLY)
    // =====================================================
    @GetMapping("/{id}/edit")
    public String editRequest(
            @PathVariable Long id,
            Model model) {

        ServiceRequest req =
                serviceRequestService.getServiceRequestById(id).orElse(null);

        // SECURITY: Only editable in DRAFT
        if (req == null || req.getStatus() != ServiceRequestStatus.DRAFT) {
            return "redirect:/service-requests";
        }

        model.addAttribute("serviceRequest", req);
        return "service-request-edit";
    }

    // =====================================================
    // SAVE EDITS (DRAFT ONLY)
    // =====================================================
    @PostMapping("/{id}/update")
    public String updateRequest(
            @PathVariable Long id,
            @ModelAttribute ServiceRequest formReq) {

        ServiceRequest existing =
                serviceRequestService.getServiceRequestById(id).orElse(null);

        if (existing == null || existing.getStatus() != ServiceRequestStatus.DRAFT) {
            return "redirect:/service-requests";
        }

        // Allowed PM edits before process start
        existing.setTitle(formReq.getTitle());
        existing.setDescription(formReq.getDescription());
        existing.setInternalProjectName(formReq.getInternalProjectName());
        existing.setInternalProjectId(formReq.getInternalProjectId());
        existing.setPerformanceLocation(formReq.getPerformanceLocation());
        existing.setRequiredSkills(formReq.getRequiredSkills());
        existing.setMinExperience(formReq.getMinExperience());
        existing.setHoursPerWeek(formReq.getHoursPerWeek());
        existing.setHourlyRate(formReq.getHourlyRate());
        existing.setStartDate(formReq.getStartDate());
        existing.setEndDate(formReq.getEndDate());
        existing.setProjectContext(formReq.getProjectContext());

        serviceRequestService.createServiceRequest(existing);
        return "redirect:/service-requests";
    }

    // =====================================================
    // MANUAL STATUS UPDATE (ADMIN / DEBUG)
    // =====================================================
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam ServiceRequestStatus status) {

        serviceRequestService.updateServiceRequestStatus(id, status);
        return "redirect:/service-requests";
    }

    // =====================================================
    // REQUEST DETAILS PAGE
    // =====================================================
    @GetMapping("/{id}")
    public String getServiceRequestDetails(
            @PathVariable Long id,
            Model model) {

        ServiceRequest request =
                serviceRequestService.getServiceRequestById(id).orElse(null);

        model.addAttribute("serviceRequest", request);
        return "service-request-details";
    }

    // =====================================================
    // DELETE REQUEST (DRAFT ONLY)
    // =====================================================
    @PostMapping("/{id}/delete")
    public String deleteRequest(@PathVariable Long id) {

        ServiceRequest req =
                serviceRequestService.getServiceRequestById(id).orElse(null);

        if (req != null && req.getStatus() == ServiceRequestStatus.DRAFT) {
            serviceRequestService.deleteServiceRequest(id);
        }

        return "redirect:/service-requests";
    }

    // =====================================================
    // LIGHTWEIGHT REST APIs (OPTIONAL)
    // =====================================================
    @GetMapping("/api")
    @ResponseBody
    public List<ServiceRequest> getAllServiceRequestsApi() {
        return serviceRequestService.getAllServiceRequests();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ServiceRequest getServiceRequestApi(@PathVariable Long id) {
        return serviceRequestService
                .getServiceRequestById(id)
                .orElse(null);
    }
}
