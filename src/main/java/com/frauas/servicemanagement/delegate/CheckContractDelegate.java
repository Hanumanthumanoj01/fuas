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

        Optional<ServiceRequest> reqOpt = serviceRequestService.getServiceRequestById(requestId);

        if(reqOpt.isPresent()) {
            ServiceRequest req = reqOpt.get();

            // 1. Send RICH Request to 2b (As requested)
            System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 2B: Validating Contract Eligibility");
            System.out.println("   PAYLOAD: {");
            System.out.println("      \"internalRequestId\": " + req.getInternalRequestId() + ",");
            System.out.println("      \"projectId\": " + req.getInternalProjectId() + ",");
            System.out.println("      \"projectName\": \"" + req.getInternalProjectName() + "\",");
            System.out.println("      \"jobTitle\": \"" + req.getTitle() + "\"");
            System.out.println("   }");

            // 2. MOCK RESPONSE LOGIC (Simulate Valid vs Invalid)
            // Logic: If the Project ID is '999', simulate NO CONTRACT (Failure Case)
            boolean contractExists = req.getInternalProjectId() != 999;

            if (contractExists) {
                //  CASE A: Contract Found
                String contractId = "CTR-2025-" + (1000 + (long)(Math.random() * 9000));

                System.out.println(">>> [API IN] GROUP 2B RESPONSE (Case A):");
                System.out.println("   { \"contractId\": \"" + contractId + "\", \"status\": \"ACTIVE\", \"validUntil\": \"2026-12-31\" }");

                req.setContractId(contractId);
                serviceRequestService.createServiceRequest(req); // Save ID

                // Set Process Variables
                execution.setVariable("contractId", contractId);
                execution.setVariable("contractValid", true);

            } else {
                //  CASE B: No Contract
                System.out.println(">>> [API IN] GROUP 2B RESPONSE (Case B):");
                System.out.println("   { \"status\": \"NO_ACTIVE_CONTRACT\" }");

                execution.setVariable("contractValid", false);
            }
        } else {
            throw new RuntimeException("Request not found in DB: " + requestId);
        }
    }
}