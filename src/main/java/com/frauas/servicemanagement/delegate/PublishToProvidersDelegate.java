package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.service.ServiceRequestService;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("publishToProvidersDelegate")
public class PublishToProvidersDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        Long requestId = (Long) execution.getVariable("requestId");

        System.out.println(
                "\n>>> [API OUT] GROUP 3B -> GROUP 4: Publishing Request ID " + requestId
        );

        Optional<ServiceRequest> requestOpt =
                serviceRequestService.getServiceRequestById(requestId);

        if (requestOpt.isPresent()) {

            // ✅ Update status only
            serviceRequestService.updateServiceRequestStatus(
                    requestId,
                    ServiceRequestStatus.PUBLISHED
            );

            System.out.println(
                    ">>> Request published successfully. Waiting for offers via external API..."
            );

        } else {
            System.err.println(
                    "!!! ERROR: ServiceRequest not found for ID " + requestId
            );
        }
    }
}
