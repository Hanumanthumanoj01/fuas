package com.frauas.servicemanagement.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("notifyWorkforceDelegate")
public class NotifyWorkforceDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferRepository offerRepo;

    // Use RestTemplate to send HTTP requests
    private final RestTemplate restTemplate = new RestTemplate();
    // Use ObjectMapper to create clean JSON strings
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        Long reqId = (Long) execution.getVariable("requestId");
        Long offerId = (Long) execution.getVariable("selectedOfferId");

        if (reqId == null || offerId == null) {
            throw new IllegalStateException("Missing process variables: requestId or selectedOfferId");
        }

        ServiceRequest req = serviceRequestService.getServiceRequestById(reqId)
                .orElseThrow(() -> new IllegalStateException("ServiceRequest not found: " + reqId));

        ProviderOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new IllegalStateException("ProviderOffer not found: " + offerId));

        // --------------------------------------------------
        // 1. Prepare Data & Skills List
        // --------------------------------------------------
        List<String> skillsList = List.of();
        if (offer.getSkills() != null && !offer.getSkills().isBlank()) {
            // Convert string "[Java, Spring]" into a real List object
            skillsList = Arrays.stream(offer.getSkills()
                            .replace("[", "")
                            .replace("]", "")
                            .split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        String contractId = offer.getContractId() != null ? offer.getContractId() : req.getContractId();

        // --------------------------------------------------
        // 2. Build the Payload Object (Map)
        // --------------------------------------------------
        Map<String, Object> payload = new HashMap<>();
        payload.put("externalEmployeeId", offer.getExternalOfferId());
        payload.put("provider", offer.getProviderName());
        payload.put("firstName", offer.getFirstName());
        payload.put("lastName", offer.getLastName());
        payload.put("email", offer.getEmail());
        payload.put("contractId", contractId);
        payload.put("evaluationScore", offer.getTotalScore());
        payload.put("wagePerHour", offer.getHourlyRate());
        payload.put("skills", skillsList); // Sends real JSON array: ["Java", "Spring"]
        payload.put("staffingRequestId", req.getInternalRequestId());
        payload.put("experienceYears", offer.getExperienceYears());
        payload.put("projectId", req.getInternalProjectId());
        payload.put("status", "OFFER_WON");

        // --------------------------------------------------
        // 3. LOGGING (Console)
        // --------------------------------------------------
        // Convert Map to JSON String for Logging
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 1B: Hiring Confirmation");
        System.out.println("   ENDPOINT: POST https://workforce-planning-tool.onrender.com/api/group3b/workforce-response");
        System.out.println("   PAYLOAD: " + jsonString);
        System.out.println("=========================================================\n");

        // --------------------------------------------------
        // 4. SEND REAL REQUEST (The Implementation)
        // --------------------------------------------------
        String targetUrl = "https://workforce-planning-tool.onrender.com/api/group3b/workforce-response";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            // Fire the request
            restTemplate.postForObject(targetUrl, requestEntity, String.class);

            System.out.println(">>> SUCCESS: Real notification successfully sent to Group 1b API.");

        } catch (Exception e) {
            // SAFETY: If their server is down, log it but DO NOT crash our process.
            System.err.println("!!! WARNING: Failed to send real notification to Group 1b. Their server might be down.");
            System.err.println("!!! Error Details: " + e.getMessage());
            System.err.println("!!! Continuing process execution...");
        }
    }
}