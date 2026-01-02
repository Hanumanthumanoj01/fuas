package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.service.ProviderOfferService;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * External Integration Controller
 * Handles communication with other project groups
 */
@RestController
@RequestMapping("/api/integration")
public class ExternalIntegrationController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferService providerOfferService;

    // =====================================================
    // GROUP 1: WORKFORCE INTEGRATION
    // Receives workforce requirements from Group 1
    // =====================================================
    @PostMapping("/group1/workforce-request")
    public ResponseEntity<String> receiveWorkforceNeed(
            @RequestBody Map<String, String> payload) {

        ServiceRequest request = new ServiceRequest();
        request.setTitle(payload.getOrDefault(
                "title", "Incoming Request from Workforce System"));
        request.setDescription(payload.getOrDefault(
                "description", "External Workforce Requirement"));
        request.setProjectContext(
                payload.getOrDefault("projectContext", "External Workforce Dependency"));

        serviceRequestService.createServiceRequest(request);

        return ResponseEntity.ok(
                "Workforce request received and queued for PM review.");
    }

    // =====================================================
    // GROUP 4: CONTRACT ACKNOWLEDGEMENT
    // Confirms contract preparation has started
    // =====================================================
    @PostMapping("/group4/notify")
    public ResponseEntity<String> receiveNotification(
            @RequestBody Long offerId) {

        return ResponseEntity.ok(
                "Provider Management System acknowledged. " +
                        "Contract preparation started for Offer ID: " + offerId);
    }

    // =====================================================
    // GROUP 4: PROVIDER OFFER (UPDATED – REAL INTEGRATION)
    // Accepts offers in Group 4 JSON structure
    // =====================================================
    @PostMapping("/group4/offer")
    public ResponseEntity<String> receiveProviderOffer(
            @RequestBody Map<String, Object> payload) {

        Long serviceRequestId =
                Long.valueOf(payload.get("serviceRequestId").toString());

        ProviderOffer offer = new ProviderOffer();

        // Mapping Group 4 JSON → Internal Entity
        offer.setExternalOfferId(
                payload.get("offerId").toString());
        offer.setProviderName(
                payload.get("company").toString());
        offer.setServiceType(
                payload.get("serviceType").toString());
        offer.setSpecialistName(
                payload.get("specialistName").toString());
        offer.setDailyRate(
                Double.valueOf(payload.get("dailyRate").toString()));
        offer.setOnsiteDays(
                Integer.valueOf(payload.get("onsiteDays").toString()));
        offer.setTravelCost(
                Double.valueOf(payload.get("travellingCost").toString()));
        offer.setTotalCost(
                Double.valueOf(payload.get("totalCost").toString()));
        offer.setContractType(
                payload.get("contractualRelationship").toString());
        offer.setSkills(
                payload.get("skills").toString());

        providerOfferService.submitOffer(serviceRequestId, offer);

        return ResponseEntity.ok(
                "Offer received successfully and queued for evaluation.");
    }

    // =====================================================
    // GROUP 4: PROVIDER SYSTEM SIMULATION (MOCK)
    // Used for demo/testing when Group 4 is unavailable
    // =====================================================
    @PostMapping("/providers/publish-request")
    public List<ProviderOffer> simulateProviderResponses(
            @RequestBody ServiceRequest request) {

        System.out.println(
                ">>> EXTERNAL INTEGRATION: Broadcasting request: "
                        + request.getTitle());

        List<ProviderOffer> offers = new ArrayList<>();

        ProviderOffer offer1 = new ProviderOffer();
        offer1.setProviderName("Global Tech Solutions");
        offer1.setDailyRate(95.50);
        offer1.setSpecialistName("Senior DevOps Engineer");
        offer1.setSkills("Docker, Kubernetes, CI/CD");
        offer1.setOnsiteDays(10);
        offer1.setTravelCost(1200.00);
        offer1.setTotalCost(9500.00);
        offer1.setContractType("Time & Material");

        ProviderOffer offer2 = new ProviderOffer();
        offer2.setProviderName("Cloud Innovators GmbH");
        offer2.setDailyRate(87.25);
        offer2.setSpecialistName("Full-stack Developer");
        offer2.setSkills("Java, Spring Boot, AWS");
        offer2.setOnsiteDays(5);
        offer2.setTravelCost(800.00);
        offer2.setTotalCost(8200.00);
        offer2.setContractType("Fixed Price");

        ProviderOffer offer3 = new ProviderOffer();
        offer3.setProviderName("FraUAS Research Lab");
        offer3.setDailyRate(75.00);
        offer3.setSpecialistName("R&D Specialist Team");
        offer3.setSkills("AI Research, Prototyping");
        offer3.setOnsiteDays(0);
        offer3.setTravelCost(0.00);
        offer3.setTotalCost(7000.00);
        offer3.setContractType("Research Contract");

        offers.add(offer1);
        offers.add(offer2);
        offers.add(offer3);

        return offers;
    }
}
