package com.frauas.servicemanagement.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.service.ServiceRequestService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public PublishToProvidersDelegate() {
        // Essential to handle LocalDate (startDate/endDate) without crashing
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
        payload.put("internalRequestId", req.getInternalRequestId());
        payload.put("contractId", req.getContractId());
        payload.put("title", req.getTitle());
        payload.put("description", req.getDescription());
        payload.put("projectName", req.getInternalProjectName());
        payload.put("startDate", req.getStartDate());
        payload.put("endDate", req.getEndDate());
        payload.put("skills", skillsList);
        payload.put("budget", req.getHourlyRate());
        payload.put("location", req.getPerformanceLocation());

        // --------------------------------------------------
        // 3. SEND TO WEBHOOK (TESTING)
        // --------------------------------------------------
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 4B: Publishing Open Request");
        // 4b link
        String targetUrl = "https://providermanagement-arg3e3hsgwefarbr.germanywestcentral-01.azurewebsites.net/api/ServiceRequestApi/ReceiveServiceRequest";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity =
                    new HttpEntity<>(payload, headers);

            restTemplate.postForObject(targetUrl, requestEntity, String.class);
            System.out.println(">>> SUCCESS: 4B received the service request.");

        } catch (Exception e) {
            System.err.println("!!! [FAILURE] Could not send to 4B: " + e.getMessage());
        }


        System.out.println("    ENDPOINT: " + targetUrl);
        System.out.println("    PAYLOAD: " + jsonString);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            // Fire the request to Webhook.site
            restTemplate.postForObject(targetUrl, requestEntity, String.class);
            System.out.println(">>> SUCCESS: Payload captured in Webhook.site");

        } catch (Exception e) {
            System.err.println("!!! WARNING: Failed to send to Webhook: " + e.getMessage());
        }

        System.out.println("=========================================================\n");
    }
}