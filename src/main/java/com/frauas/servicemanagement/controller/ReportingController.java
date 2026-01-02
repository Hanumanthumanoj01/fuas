package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reporting")
@CrossOrigin(origins = "*")
public class ReportingController {

    @Autowired
    private ReportingService reportingService;

    @GetMapping("/requests/summary")
    public Map<String, Object> getRequestSummary() {
        Map<String, Object> data = reportingService.getServiceRequestSummary();
        // --- CONSOLE DEMO FOR PROFESSOR ---
        System.out.println("\n========== [API DEMO] GROUP 5B REQUESTING DATA ==========");
        System.out.println("Endpoint: GET /api/reporting/requests/summary");
        System.out.println("Payload Sent: " + data);
        System.out.println("=========================================================\n");
        return data;
    }

    @GetMapping("/offers/statistics")
    public Map<String, Object> getOfferStats() {
        Map<String, Object> data = reportingService.getOfferStatistics();
        System.out.println("\n========== [API DEMO] GROUP 5B REQUESTING STATS ==========");
        System.out.println("Payload Sent: " + data);
        System.out.println("==========================================================\n");
        return data;
    }

    @GetMapping("/providers/rankings")
    public List<Map<String, Object>> getRankings() {
        return reportingService.getProviderRankings();
    }
}