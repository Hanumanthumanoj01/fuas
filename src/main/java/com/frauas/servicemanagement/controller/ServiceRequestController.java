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
    // ✅ PROFESSIONAL PM DASHBOARD (SINGLE ENTRY POINT)
    // =====================================================
    @GetMapping
    public String getPMDashboard(Model model) {

        // -------------------------------------------------
        // 1. DATABASE RECORDS (FULL HISTORY)
        // -------------------------------------------------
        List<ServiceRequest> allRequests =
                serviceRequestService.getAllServiceRequests();

        model.addAttribute("serviceRequests", allRequests);
        model.addAttribute("newRequest", new ServiceRequest());

        // -------------------------------------------------
        // 2. ACTIVE CAMUNDA TASKS (ACTION ITEMS)
        // -------------------------------------------------
        List<Task> pmTasks =
                camundaProcessService.getTasksForAssignee("pm_user");

        model.addAttribute("activeTasks", pmTasks);

        // -------------------------------------------------
        // 3. KPI CARDS
        // -------------------------------------------------
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

        // -------------------------------------------------
        // 4. SINGLE DASHBOARD VIEW
        // -------------------------------------------------
        return "service-requests"; // Will be rewritten as PM Dashboard
    }

    // =====================================================
    // CREATE SERVICE REQUEST (DRAFT ONLY)
    // =====================================================
    @PostMapping
    public String createServiceRequest(
            @ModelAttribute ServiceRequest serviceRequest) {

        serviceRequestService.createServiceRequest(serviceRequest);
        return "redirect:/service-requests";
    }

    // =====================================================
    // START CAMUNDA PROCESS EXPLICITLY
    // =====================================================
    @PostMapping("/{id}/process")
    public String startProcessForDraft(@PathVariable Long id) {

        serviceRequestService.startProcessForRequest(id);
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
    // DELETE REQUEST
    // =====================================================
    @PostMapping("/{id}/delete")
    public String deleteRequest(@PathVariable Long id) {

        serviceRequestService.deleteServiceRequest(id);
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
