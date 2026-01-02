package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.service.EmailService;
import com.frauas.servicemanagement.service.ProviderOfferService;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component("publishToProvidersDelegate")
public class PublishToProvidersDelegate implements JavaDelegate {

    @Autowired private ServiceRequestService serviceRequestService;
    @Autowired private ProviderOfferRepository providerOfferRepository;
    @Autowired private ProviderOfferService providerOfferService;
    @Autowired private EmailService emailService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long requestId = (Long) execution.getVariable("requestId");
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 4: Publishing Request ID " + requestId);

        Optional<ServiceRequest> requestOpt = serviceRequestService.getServiceRequestById(requestId);

        if (requestOpt.isPresent()) {
            ServiceRequest request = requestOpt.get();

            // 1. Update Status
            serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.PUBLISHED);

            try {
                System.out.println(">>> [API IN] GROUP 4 -> GROUP 3B: Receiving Mock Offers...");

                // ✅ MOCK OFFER 1 (Based on Group 4b Excel)
                ProviderOffer o1 = new ProviderOffer();
                o1.setServiceRequest(request);
                o1.setProviderName("Check24");
                o1.setServiceType("Low Wage");
                o1.setSpecialistName("Test1");
                o1.setDailyRate(100.00);
                o1.setOnsiteDays(8);
                o1.setTravelCost(80.00);
                o1.setTotalCost(1280.00);
                o1.setContractType("Working student");
                o1.setSkills("Python, German");
                o1.setSubmittedAt(LocalDateTime.now());
                providerOfferRepository.save(o1);

                // ✅ MOCK OFFER 2
                ProviderOffer o2 = new ProviderOffer();
                o2.setServiceRequest(request);
                o2.setProviderName("Siemens");
                o2.setServiceType("Expert");
                o2.setSpecialistName("Test2");
                o2.setDailyRate(200.00);
                o2.setOnsiteDays(20);
                o2.setTravelCost(400.00);
                o2.setTotalCost(4400.00);
                o2.setContractType("Employee");
                o2.setSkills("5 years experience");
                o2.setSubmittedAt(LocalDateTime.now());
                providerOfferRepository.save(o2);

                // Calculate Scores
                providerOfferService.calculateRanking(requestId);

                // Update Status & Notify
                serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.OFFERS_RECEIVED);
                emailService.sendNotification("rp_user", "Offers Received", "Check Dashboard.");

            } catch (Exception e) {
                System.err.println("!!! INTEGRATION ERROR: " + e.getMessage());
            }
        }
    }
}