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

import java.util.HashMap;
import java.util.Map;

@Component("notify1bRecommendationDelegate")
public class Notify1bRecommendationDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferRepository offerRepo;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        Long reqId = (Long) execution.getVariable("requestId");
        Long offerId = (Long) execution.getVariable("selectedOfferId");

        ServiceRequest req = serviceRequestService.getServiceRequestById(reqId)
                .orElseThrow(() -> new IllegalStateException("ServiceRequest not found: " + reqId));

        ProviderOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new IllegalStateException("ProviderOffer not found: " + offerId));

        // --------------------------------------------------
        // 1. DATA PREPARATION
        // --------------------------------------------------
        String cleanSkills = "Unknown";
        if (offer.getSkills() != null) {
            cleanSkills = offer.getSkills().replace("[", "").replace("]", "").replace("\"", "").trim();
        }

        // Location Logic: Try Offer first (from 4b), then Request
        String finalLocation = offer.getLocation();
        if (finalLocation == null || finalLocation.isEmpty()) {
            finalLocation = req.getPerformanceLocation();
        }
        if (finalLocation == null || finalLocation.isEmpty()) {
            finalLocation = "Frankfurt";
        }

        String contractId = offer.getContractId();
        if (contractId == null || contractId.isEmpty()) contractId = req.getContractId();
        if (contractId == null) contractId = "CTR-PENDING";

        // --------------------------------------------------
        // 2. BUILD PAYLOAD
        // --------------------------------------------------
        Map<String, Object> payload = new HashMap<>();
        payload.put("staffingRequestId", req.getInternalRequestId());
        payload.put("externalEmployeeId", offer.getExternalOfferId());
        payload.put("provider", offer.getProviderName());
        payload.put("firstName", offer.getFirstName());
        payload.put("lastName", offer.getLastName());
        payload.put("email", offer.getEmail());
        payload.put("wagePerHour", offer.getHourlyRate());
        payload.put("skills", cleanSkills);
        payload.put("location", finalLocation);
        payload.put("experienceYears", offer.getExperienceYears());
        payload.put("contractId", contractId);
        payload.put("evaluationScore", offer.getTotalScore());
        payload.put("projectId", req.getInternalProjectId());

        // Convert to JSON String here (safe in main thread)
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // --------------------------------------------------
        // 3. ASYNCHRONOUS SEND (BACKGROUND THREAD)
        // --------------------------------------------------
        // This prevents the UI from hanging waiting for 1b
        new Thread(() -> {
            try {
                System.out.println("\n>>> [ASYNC API OUT] Sending to 1b in background...");
                System.out.println("   PAYLOAD: " + jsonPayload);

                String targetUrl = "https://workforce-planning-tool.onrender.com/api/group3b/workforce-response";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);

                restTemplate.postForObject(targetUrl, requestEntity, String.class);
                System.out.println(">>> [ASYNC SUCCESS] 1b Received the offer.");

            } catch (Exception e) {
                System.err.println("!!! [ASYNC FAILURE] Could not send to 1b: " + e.getMessage());
            }
        }).start();

        // --------------------------------------------------
        // 4. MAIN THREAD FINISHES IMMEDIATELY
        // --------------------------------------------------
        // We log locally and let the process continue to the Wait State
        System.out.println(">>> Delegate finished. Process moving to Wait State.");

        // 4b Log (kept for reference)
        System.out.println(">>> [INFO] 4b Offer " + offer.getExternalOfferId() + " is SELECTED_UNDER_VERIFICATION");
    }
}