package com.frauas.servicemanagement.controller;

import com.frauas.servicemanagement.entity.*;
import com.frauas.servicemanagement.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
public class ExternalIntegrationController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferService providerOfferService;

    @Autowired
    private RuntimeService runtimeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =====================================================
    // 1B → INBOUND WORKFORCE REQUEST (CREATE DRAFT ONLY)
    // =====================================================
    @PostMapping("/group1/workforce-request")
    public ResponseEntity<String> receiveWorkforceNeed(
            @RequestBody Map<String, Object> payload) {

        ServiceRequest req = new ServiceRequest();

        // ---------- RAW PAYLOAD (AUDIT / DEBUG) ----------
        try {
            req.setRawPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            req.setRawPayload("ERROR_SERIALIZING_JSON");
        }

        // ---------- SAFE FIELD MAPPING ----------
        if (payload.get("internalRequestId") != null) {
            req.setInternalRequestId(
                    Long.valueOf(payload.get("internalRequestId").toString()));
        }

        if (payload.get("projectId") != null) {
            req.setInternalProjectId(
                    Long.valueOf(payload.get("projectId").toString()));
        }

        req.setInternalProjectName(
                String.valueOf(payload.getOrDefault("projectName", "")));

        req.setTitle(
                String.valueOf(payload.getOrDefault("jobTitle", "Workforce Request")));

        req.setDescription(
                String.valueOf(payload.getOrDefault("description", "")));

        if (payload.get("availabilityHoursPerWeek") != null) {
            req.setHoursPerWeek(
                    Integer.parseInt(payload.get("availabilityHoursPerWeek").toString()));
        }

        if (payload.get("wagePerHour") != null) {
            double hourly =
                    Double.parseDouble(payload.get("wagePerHour").toString());
            req.setHourlyRate(hourly);
            req.setMaxDailyRate(hourly * 8);
        }

        if (payload.get("skills") instanceof List) {
            req.setRequiredSkills(payload.get("skills").toString());
        } else {
            req.setRequiredSkills(
                    String.valueOf(payload.getOrDefault("skills", "")));
        }

        if (payload.get("experienceYears") != null) {
            req.setMinExperience(
                    Integer.parseInt(payload.get("experienceYears").toString()));
        }

        req.setPerformanceLocation(
                String.valueOf(payload.getOrDefault("location", "Remote")));

        req.setProjectContext(
                String.valueOf(payload.getOrDefault("projectContext", "")));

        if (payload.get("startDate") != null) {
            req.setStartDate(
                    LocalDate.parse(payload.get("startDate").toString()));
        }

        if (payload.get("endDate") != null) {
            req.setEndDate(
                    LocalDate.parse(payload.get("endDate").toString()));
        }

        // ---------- SAVE AS DRAFT ----------
        serviceRequestService.createServiceRequest(req);

        return ResponseEntity.ok(
                "Draft created successfully. PM review pending.");
    }

    // =====================================================
    // 1B → DECISION CALLBACK (ROBUST + TYPE-SAFE + DEBUG)
    // =====================================================
    @PostMapping("/group1/decision")
    public ResponseEntity<String> receive1bDecision(@RequestBody Map<String, Object> payload) {

        System.out.println(">>> [API IN] 1B Decision Payload: " + payload);

        if (!payload.containsKey("requestId") || !payload.containsKey("decision")) {
            return ResponseEntity.badRequest().body("Error: Missing 'requestId' or 'decision'");
        }

        try {
            // 1. Get the ID provided by 1b (e.g., "1001")
            String providedId = String.valueOf(payload.get("requestId"));
            String decision = String.valueOf(payload.get("decision"));

            Long internalRefId;
            try {
                internalRefId = Long.valueOf(providedId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Error: requestId must be numeric.");
            }

            // 2. LOOKUP: Find the Database ID (e.g., "2") using the Internal ID (e.g., "1001")
            ServiceRequest targetRequest = serviceRequestService.getAllServiceRequests().stream()
                    .filter(r -> r.getInternalRequestId() != null && r.getInternalRequestId().equals(internalRefId))
                    .findFirst()
                    .orElse(null);

            if (targetRequest == null) {
                return ResponseEntity.badRequest().body("Error: No Active Request found for Internal ID " + providedId);
            }

            // 3. Use the DB ID for Camunda
            String targetDbId = String.valueOf(targetRequest.getId());
            System.out.println(">>> [API IN] Mapped Internal ID " + providedId + " -> DB ID " + targetDbId);

            // 4. Find the Process waiting for this DB ID
            List<Execution> waitingExecutions = runtimeService.createExecutionQuery()
                    .messageEventSubscriptionName("Message_1b_Decision")
                    .list();

            Execution matchedExecution = null;

            for (Execution exec : waitingExecutions) {
                Object storedReqId = runtimeService.getVariable(exec.getId(), "requestId");
                if (storedReqId != null && String.valueOf(storedReqId).equals(targetDbId)) {
                    matchedExecution = exec;
                    break;
                }
            }

            if (matchedExecution == null) {
                return ResponseEntity.badRequest().body("No process is waiting for decision on Request " + providedId);
            }

            // 5. Resume
            System.out.println(">>> Resuming Process Instance: " + matchedExecution.getProcessInstanceId());

            runtimeService.createMessageCorrelation("Message_1b_Decision")
                    .processInstanceId(matchedExecution.getProcessInstanceId())
                    .setVariable("oneBDecision", decision)
                    .correlate();

            return ResponseEntity.ok("Decision processed successfully for Internal ID " + providedId);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing decision: " + e.getMessage());
        }
    }


    // =====================================================
    // 4B → INBOUND PROVIDER OFFER (ROBUST)
    // =====================================================
    @PostMapping("/group4/offer")
    public ResponseEntity<String> receiveProviderOffer(@RequestBody Map<String, Object> payload) {

        System.out.println(">>> [API IN] 4B Offer Payload: " + payload);


        Long internalId = Long.valueOf(String.valueOf(payload.get("internalRequestId"))); // e.g., 1001

        ServiceRequest targetRequest = serviceRequestService.getAllServiceRequests().stream()
                .filter(r -> r.getInternalRequestId() != null && r.getInternalRequestId().equals(internalId))
                .findFirst()
                .orElse(null);

        if (targetRequest == null) {
            return ResponseEntity.badRequest().body("Error: No Active Request found for Internal ID " + internalId);
        }

        Long reqId = targetRequest.getId(); // THIS is the DB ID (e.g., 2)
        // ------------------------------------------------

        ProviderOffer offer = new ProviderOffer();
        offer.setExternalOfferId(String.valueOf(payload.get("offerId")));
        offer.setProviderName(String.valueOf(payload.get("company")));

        // Handle Contract ID (flexible key)
        if (payload.containsKey("contractid")) offer.setContractId(String.valueOf(payload.get("contractid")));
        else if (payload.containsKey("contractId")) offer.setContractId(String.valueOf(payload.get("contractId")));

        // Name Parsing
        if (payload.containsKey("firstName") && payload.containsKey("lastName")) {
            offer.setFirstName(String.valueOf(payload.get("firstName")));
            offer.setLastName(String.valueOf(payload.get("lastName")));
            offer.setSpecialistName(offer.getFirstName() + " " + offer.getLastName());
        } else {
            String full = String.valueOf(payload.getOrDefault("specialistName", "Unknown"));
            offer.setSpecialistName(full);
        }

        offer.setEmail(String.valueOf(payload.getOrDefault("email", "contact@provider.com")));

        offer.setExperienceYears(payload.get("experienceYears") != null
                ? Float.parseFloat(payload.get("experienceYears").toString()) : 0.0f);

        // Financials
        if (payload.get("wagePerHour") != null) {
            double hourly = Double.parseDouble(payload.get("wagePerHour").toString());
            offer.setHourlyRate(hourly);
            offer.setDailyRate(hourly * 8);
        }

        offer.setTotalCost(Double.parseDouble(payload.get("totalCost").toString()));
        offer.setSkills(String.valueOf(payload.get("skills")));

        // SAVE
        providerOfferService.submitOffer(reqId, offer);
        providerOfferService.calculateRanking(reqId);
        serviceRequestService.updateServiceRequestStatus(reqId, ServiceRequestStatus.OFFERS_RECEIVED);

        return ResponseEntity.ok("Offer received for Request " + reqId + " (Internal: " + internalId + ")");
    }
}
