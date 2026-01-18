package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.dto.ReportingOfferDTO;
import com.frauas.servicemanagement.dto.ReportingRequestDTO;
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

    // --- 1. CLEAN REQUESTS (Issue 1) ---
    @GetMapping("/requests/all")
    public List<ReportingRequestDTO> getAllRequests() {
        System.out.println(">>> REPORTING: Fetching clean request list...");
        return reportingService.getAllRequestsClean();
    }

    // --- 2. CLEAN OFFERS (Issue 2) ---
    @GetMapping("/offers/all")
    public List<ReportingOfferDTO> getAllOffers() {
        System.out.println(">>> REPORTING: Fetching clean offer list...");
        return reportingService.getAllOffersClean();
    }

    // --- 3. COMBINED HIERARCHY (Issue 4) ---
    @GetMapping("/combined")
    public List<Map<String, Object>> getCombinedView() {
        System.out.println(">>> REPORTING: Fetching combined report...");
        return reportingService.getCombinedReport();
    }

    // --- 4. STATS & RANKINGS (Keep existing) ---
    @GetMapping("/requests/summary")
    public Map<String, Object> getRequestSummary() {
        return reportingService.getServiceRequestSummary();
    }

    @GetMapping("/providers/rankings")
    public List<Map<String, Object>> getRankings() {
        return reportingService.getProviderRankings();
    }
}