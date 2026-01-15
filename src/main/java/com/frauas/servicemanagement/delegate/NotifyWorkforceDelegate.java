package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component("notifyWorkforceDelegate")
public class NotifyWorkforceDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferRepository offerRepo;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        Long reqId = (Long) execution.getVariable("requestId");
        Long offerId = (Long) execution.getVariable("selectedOfferId");

        if (reqId == null || offerId == null) {
            throw new IllegalStateException(
                    "Missing process variables: requestId or selectedOfferId");
        }

        ServiceRequest req = serviceRequestService
                .getServiceRequestById(reqId)
                .orElseThrow(() ->
                        new IllegalStateException("ServiceRequest not found: " + reqId));

        ProviderOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() ->
                        new IllegalStateException("ProviderOffer not found: " + offerId));

        // --------------------------------------------------
        // Skills → JSON Array
        // --------------------------------------------------
        String skillsJsonArray = "[]";
        if (offer.getSkills() != null && !offer.getSkills().isBlank()) {
            skillsJsonArray =
                    "[" + Arrays.stream(
                                    offer.getSkills()
                                            .replace("[", "")
                                            .replace("]", "")
                                            .split(","))
                            .map(s -> "\"" + s.trim() + "\"")
                            .collect(Collectors.joining(",")) + "]";
        }

        // --------------------------------------------------
        // Contract ID Fallback
        // --------------------------------------------------
        String contractId =
                offer.getContractId() != null
                        ? offer.getContractId()
                        : req.getContractId();

        // --------------------------------------------------
        // FINAL OUTBOUND PAYLOAD (1B)
        // --------------------------------------------------
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 1B: Hiring Confirmation");
        System.out.println("   ENDPOINT: POST /api/v1/external-employee/assign");
        System.out.println("   PAYLOAD: {");

        // ✅ EXACT STRUCTURE AS REQUESTED
        System.out.println("      \"externalEmployeeId\": \"" + offer.getExternalOfferId() + "\",");
        System.out.println("      \"provider\": \"" + offer.getProviderName() + "\",");
        System.out.println("      \"firstName\": \"" + offer.getFirstName() + "\",");
        System.out.println("      \"lastName\": \"" + offer.getLastName() + "\",");
        System.out.println("      \"email\": \"" + offer.getEmail() + "\",");
        System.out.println("      \"contractId\": \"" + contractId + "\",");
        System.out.println("      \"evaluationScore\": " + offer.getTotalScore() + ",");
        System.out.println("      \"wagePerHour\": " + offer.getHourlyRate() + ",");
        System.out.println("      \"skills\": " + skillsJsonArray + ",");
        System.out.println("      \"staffingRequestId\": " + req.getInternalRequestId() + ",");
        System.out.println("      \"experienceYears\": " + offer.getExperienceYears() + ",");
        System.out.println("      \"projectId\": " + req.getInternalProjectId() + ",");
        System.out.println("      \"status\": \"OFFER_WON\"");

        System.out.println("   }");
        System.out.println("=========================================================\n");
    }
}
