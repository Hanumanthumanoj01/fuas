package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    // Inject service to get statistics (was previously in DashboardController)
    @Autowired
    private ServiceRequestService serviceRequestService;

    @GetMapping("/login")
    public String login() {
        return "login"; // Professional Login Page
    }

    @GetMapping("/")
    public String root() {
        // Automatically route to the appropriate dashboard after login check
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        if (auth != null) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PM"))) {
                return "redirect:/service-requests";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                // ✅ FIXED
                return "redirect:/camunda/tasks/po_user";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RP"))) {
                // ✅ FIXED
                return "redirect:/camunda/tasks/rp_user";
            }
        }

        // If the user somehow lands here without a specific role redirect (fallback for generic dashboard)
        // Add statistics for a full dashboard view (Test 5: Graphics)
        model.addAttribute("totalRequests", getTotalRequests());
        model.addAttribute("pendingApproval", getCountByStatus(ServiceRequestStatus.WAITING_APPROVAL));
        model.addAttribute("inProgress", getInProgressCount());
        model.addAttribute("completed", getCountByStatus(ServiceRequestStatus.COMPLETED));
        model.addAttribute("statusData", getStatusDistribution());

        // This fallback should rarely be hit if the redirect logic above works.
        return "dashboard";
    }

    // =====================================================
    // STATISTICS METHODS (MOVED FROM DashboardController)
    // =====================================================

    private Map<String, Integer> getStatusDistribution() {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (ServiceRequestStatus status : ServiceRequestStatus.values()) {
            distribution.put(status.name(), getCountByStatus(status));
        }
        return distribution;
    }

    private int getCountByStatus(ServiceRequestStatus status) {
        // Calls the Service layer to retrieve data
        return serviceRequestService.getServiceRequestsByStatus(status).size();
    }

    private int getTotalRequests() {
        return serviceRequestService.getAllServiceRequests().size();
    }

    private int getInProgressCount() {
        return getCountByStatus(ServiceRequestStatus.UNDER_EVALUATION) +
                getCountByStatus(ServiceRequestStatus.OFFERS_RECEIVED) +
                getCountByStatus(ServiceRequestStatus.PUBLISHED);
    }
}