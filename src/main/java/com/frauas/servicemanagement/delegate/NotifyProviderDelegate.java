package com.frauas.servicemanagement.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyProviderDelegate")
public class NotifyProviderDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long offerId = (Long) execution.getVariable("selectedOfferId");

        // Fetch to get external ID
        // (Assuming you inject repository here like in previous delegates)
        // For simplicity in logs, we often just printed internal ID, but for real API:

        System.out.println(">>> NOTIFICATION DELEGATE: Informing Group 4 about Offer Acceptance (Internal ID: " + offerId + ")");
        System.out.println(">>> [API OUT] 4B Payload: { \"status\": \"OFFER_WON\" }");
    }
}