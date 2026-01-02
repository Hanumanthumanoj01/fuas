package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceOrder;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.repository.ServiceOrderRepository;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("createServiceOrderDelegate")
public class CreateServiceOrderDelegate implements JavaDelegate {

    @Autowired private ServiceRequestService serviceRequestService;
    @Autowired private ProviderOfferRepository providerOfferRepository;
    @Autowired private ServiceOrderRepository serviceOrderRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long requestId = (Long) execution.getVariable("requestId");
        Long offerId = (Long) execution.getVariable("selectedOfferId");

        System.out.println(">>> GENERATING SERVICE ORDER for Request " + requestId + " with Offer " + offerId);

        Optional<ServiceRequest> reqOpt = serviceRequestService.getServiceRequestById(requestId);
        Optional<ProviderOffer> offerOpt = providerOfferRepository.findById(offerId);

        if (reqOpt.isPresent() && offerOpt.isPresent()) {
            ServiceRequest request = reqOpt.get();
            ProviderOffer offer = offerOpt.get();

            ServiceOrder order = new ServiceOrder();
            order.setServiceRequest(request);
            order.setSupplierName(offer.getProviderName());

            // ✅ FIX 1: Updated field name (was expertProfile)
            order.setSpecialistName(offer.getSpecialistName());

            // ✅ FIX 2: Updated field name (was proposedRate)
            order.setAgreedRate(offer.getDailyRate());

            order.setStartDate(request.getStartDate());
            order.setEndDate(request.getEndDate());

            // ✅ FIX 3: Use Total Cost provided by Group 4b
            // Fallback to calculation if null
            double totalValue = (offer.getTotalCost() != null)
                    ? offer.getTotalCost()
                    : (offer.getDailyRate() * request.getDurationDays());

            order.setTotalContractValue(totalValue);
            order.setTotalManDays(request.getDurationDays());

            serviceOrderRepository.save(order);

            // Critical for next steps
            execution.setVariable("createdOrderId", order.getId());

            serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.COMPLETED);

            System.out.println(">>> SERVICE ORDER CREATED SUCCESSFULLY. ID: " + order.getId());

            // SIMULATION FOR GROUP 2B
            System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 2B (Legal Dept)");
            System.out.println("   ACTION: Trigger Final Contract Generation");
            System.out.println("   PAYLOAD: {");
            System.out.println("      'referenceId': 'SR-" + requestId + "',");
            System.out.println("      'provider': '" + order.getSupplierName() + "',");
            System.out.println("      'totalValue': " + order.getTotalContractValue());
            System.out.println("   }");
            System.out.println("=========================================================");

        } else {
            throw new RuntimeException("Cannot create order: Request or Offer not found.");
        }
    }
}