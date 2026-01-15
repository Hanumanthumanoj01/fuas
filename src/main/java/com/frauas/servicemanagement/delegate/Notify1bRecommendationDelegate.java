package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("notify1bRecommendationDelegate")
public class Notify1bRecommendationDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferRepository offerRepo;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        Long reqId = (Long) execution.getVariable("requestId");
        Long offerId = (Long) execution.getVariable("selectedOfferId"); // INTERNAL DB ID

        ServiceRequest req =
                serviceRequestService.getServiceRequestById(reqId)
                        .orElseThrow(() ->
                                new IllegalStateException("ServiceRequest not found: " + reqId));

        ProviderOffer offer =
                offerRepo.findById(offerId)
                        .orElseThrow(() ->
                                new IllegalStateException("ProviderOffer not found: " + offerId));

        // =====================================================
        // 1️⃣ GROUP 3B → GROUP 1B (RECOMMENDATION)
        // =====================================================
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 1B: Recommendation Sent");
        System.out.println("   PAYLOAD: {");
        System.out.println("      \"staffingRequestId\": " + req.getInternalRequestId() + ",");

        // ✅ FIX: USE EXTERNAL OFFER ID (NOT DB ID)
        System.out.println("      \"externalEmployeeId\": \"" + offer.getExternalOfferId() + "\",");

        System.out.println("      \"provider\": \"" + offer.getProviderName() + "\",");
        System.out.println("      \"firstName\": \"" + offer.getFirstName() + "\",");
        System.out.println("      \"lastName\": \"" + offer.getLastName() + "\",");
        System.out.println("      \"email\": \"" + offer.getEmail() + "\",");
        System.out.println("      \"contractId\": \"" + offer.getContractId() + "\",");
        System.out.println("      \"evaluationScore\": " + offer.getTotalScore() + ",");
        System.out.println("      \"wagePerHour\": " + offer.getHourlyRate() + ",");
        System.out.println("      \"projectId\": " + req.getInternalProjectId() + ",");
        System.out.println("      \"status\": \"PENDING_MANAGER_APPROVAL\"");
        System.out.println("   }");

        // =====================================================
        // 2️⃣ GROUP 3B → GROUP 4B (EARLY STATUS UPDATE)
        // =====================================================
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 4B: Early Status Update");
        System.out.println("    OFFER ID   : " + offer.getExternalOfferId()); // ✅ FIXED
        System.out.println("    STATUS     : SELECTED_UNDER_VERIFICATION");

        // =====================================================
        // SYSTEM STATE
        // =====================================================
        System.out.println("\n>>> SYSTEM STATE: WAITING FOR 1B DECISION CALLBACK");
        System.out.println("=========================================================\n");
    }
}
