package com.frauas.servicemanagement.delegate;

import com.frauas.servicemanagement.entity.ServiceOrder;
import com.frauas.servicemanagement.repository.ServiceOrderRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("notifyWorkforceDelegate")
public class NotifyWorkforceDelegate implements JavaDelegate {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // We get the Service Order ID that was just created (Ensure CreateServiceOrderDelegate sets this variable!)
        Long orderId = (Long) execution.getVariable("createdOrderId");
        Long internalReqId = (Long) execution.getVariable("internalRequestId");

        if (orderId != null) {
            Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                ServiceOrder order = orderOpt.get();

                // --- INTEGRATION MOCK ---
                System.out.println("\n>>> [API OUT] GROUP 3B -> GROUP 1B (Workforce Mgmt)");
                System.out.println("   ACTION: Updating Internal Request ID: " + internalReqId);
                System.out.println("   STATUS: EXTERNAL_HIRED");
                System.out.println("   PAYLOAD: {");
                System.out.println("      'expertName': '" + order.getSpecialistName() + "',");
                System.out.println("      'supplier': '" + order.getSupplierName() + "',");
                System.out.println("      'startDate': '" + order.getStartDate() + "',");
                System.out.println("      'contractValue': " + order.getTotalContractValue());
                System.out.println("   }");
                System.out.println("=========================================================\n");
            }
        }
    }
}