package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("notifyContractRejectionDelegate")
public class NotifyContractRejectionDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long requestId = (Long) execution.getVariable("requestId");

        // Fetch request details for the log
        ServiceRequest req = serviceRequestService.getServiceRequestById(requestId).orElse(null);
        String internalId = req != null ? String.valueOf(req.getInternalRequestId()) : "UNKNOWN";

        // Update DB status
        serviceRequestService.updateServiceRequestStatus(requestId, ServiceRequestStatus.REJECTED);

        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 1B: Contract Validation Failed");
        System.out.println("   REASON: No active Frame Contract found (Group 2b)");
        System.out.println("   PAYLOAD: {");
        System.out.println("      \"requestId\": \"" + internalId + "\",");
        System.out.println("      \"status\": \"REJECTED\",");
        System.out.println("      \"reason\": \"NO_ACTIVE_CONTRACT\",");
        System.out.println("      \"message\": \"No active contract found for this project and service type.\"");
        System.out.println("   }");
        System.out.println("=========================================================\n");
    }
}