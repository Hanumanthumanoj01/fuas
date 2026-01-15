package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("checkContractDelegate")
public class CheckContractDelegate implements JavaDelegate {

    @Autowired private ServiceRequestService serviceRequestService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long requestId = (Long) execution.getVariable("requestId");

        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 2B: Validating Contract Eligibility");
        System.out.println("   PAYLOAD: { \"requestType\": \"Single Request\", \"domain\": \"IT\", \"role\": \"Developer\" }");

        // Mock Response from 2b
        String contractId = "CTR-2025-" + (1000 + (long)(Math.random() * 9000));
        System.out.println(">>> [API IN] GROUP 2B RESPONSE: Eligible. Contract ID: " + contractId);

        // Update Entity
        Optional<ServiceRequest> reqOpt = serviceRequestService.getServiceRequestById(requestId);
        if(reqOpt.isPresent()) {
            ServiceRequest req = reqOpt.get();
            req.setContractId(contractId);
            serviceRequestService.createServiceRequest(req); // Save
        }

        execution.setVariable("contractId", contractId);
    }
}