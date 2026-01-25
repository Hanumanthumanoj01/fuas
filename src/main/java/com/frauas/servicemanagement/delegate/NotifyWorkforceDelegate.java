package com.frauas.servicemanagement.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyWorkforceDelegate")
public class NotifyWorkforceDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // We already sent the data to 1b in the 'Notify1bRecommendationDelegate' step.
        // 1b has already accepted.
        // We have already created the Service Order.

        // This is just the final system log. DO NOT send API to 1b again.

        System.out.println("\n=========================================================");
        System.out.println(">>> [FINAL STEP] PROCESS COMPLETED SUCCESSFULLY.");
        System.out.println(">>> 1b previously accepted the offer.");
        System.out.println(">>> Service Order generated.");
        System.out.println(">>> 4b notified of the win.");
        System.out.println(">>> Updating Internal Workforce Status: COMPLETED");
        System.out.println("=========================================================\n");
    }
}