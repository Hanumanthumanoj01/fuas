package com.frauas.servicemanagement.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // IMPORT THIS
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("publishToProvidersDelegate")
public class PublishToProvidersDelegate implements JavaDelegate {

    @Autowired
    private ServiceRequestService serviceRequestService;

    // JSON Helper
    private final ObjectMapper objectMapper = new ObjectMapper();
    // RestTemplate for real API calls (if 4b provides an endpoint)
    private final RestTemplate restTemplate = new RestTemplate();

    // CONSTRUCTOR TO REGISTER TIME MODULE
    public PublishToProvidersDelegate() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long serviceRequestId = (Long) execution.getVariable("requestId");

        ServiceRequest req = serviceRequestService.getServiceRequestById(serviceRequestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + serviceRequestId));

        // --------------------------------------------------
        // 1. PREPARE DATA
        // --------------------------------------------------
        List<String> skillsList = List.of();
        if (req.getRequiredSkills() != null) {
            skillsList = Arrays.stream(req.getRequiredSkills().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        // --------------------------------------------------
        // 2. BUILD PAYLOAD (For 4B)
        // --------------------------------------------------
        Map<String, Object> payload = new HashMap<>();
        payload.put("internalRequestId", req.getInternalRequestId()); // CRITICAL for mapping
        payload.put("title", req.getTitle());
        payload.put("description", req.getDescription());
        payload.put("skills", skillsList);
        payload.put("budget", req.getHourlyRate());
        payload.put("contractId", req.getContractId()); // Sent from 2b earlier
        payload.put("startDate", req.getStartDate());
        payload.put("endDate", req.getEndDate());
        payload.put("location", req.getPerformanceLocation());

        // --------------------------------------------------
        // 3. LOG THE JSON (Verification)
        // --------------------------------------------------
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 4B: Publishing Open Request");
        // NOTE: If 4b provides a real URL, uncomment the lines below to send it.
        // System.out.println("    ENDPOINT: POST https://4b-url.com/api/receive-request");
        System.out.println("    PAYLOAD: " + jsonString);
        System.out.println("=========================================================\n");
    }
}