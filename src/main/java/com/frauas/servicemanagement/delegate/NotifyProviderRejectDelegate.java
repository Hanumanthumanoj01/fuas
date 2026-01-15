package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("notifyProviderRejectDelegate")
public class NotifyProviderRejectDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ProviderOfferRepository offerRepository;

    @Override
    public void execute(DelegateExecution execution) {

        Long requestId = (Long) execution.getVariable("requestId");
        Long offerId   = (Long) execution.getVariable("selectedOfferId");

        // 🔎 Fetch offer to get EXTERNAL offer ID
        ProviderOffer offer = offerRepository.findById(offerId).orElse(null);
        String externalOfferId =
                (offer != null && offer.getExternalOfferId() != null)
                        ? offer.getExternalOfferId()
                        : "UNKNOWN";

        // 🔄 Update Service Request Status
        serviceRequestService.updateServiceRequestStatus(
                requestId,
                ServiceRequestStatus.REJECTED
        );

        // 📤 Notify 4B (REJECTION)
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 4B: Rejection Notification");
        System.out.println("   ENDPOINT: PUT /api/opportunities/status");
        System.out.println("   PAYLOAD: {");
        System.out.println("      \"requestId\": " + requestId + ",");
        System.out.println("      \"offerId\": \"" + externalOfferId + "\",");
        System.out.println("      \"status\": \"NOT_SELECTED\",");
        System.out.println("      \"reason\": \"Rejected by Client Manager (1b)\"");
        System.out.println("   }");
        System.out.println("=========================================================\n");
    }
}
