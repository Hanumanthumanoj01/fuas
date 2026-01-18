package com.frauas.servicemanagement.service;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ServiceRequestService {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ProviderOfferRepository providerOfferRepository;

    @Autowired
    private CamundaProcessService camundaProcessService;

    /* =====================================================
       READ OPERATIONS
       ===================================================== */

    @Transactional(readOnly = true)
    public List<ServiceRequest> getAllServiceRequests() {
        return serviceRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ServiceRequest> getServiceRequestById(Long id) {
        return serviceRequestRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequest> getServiceRequestsByStatus(ServiceRequestStatus status) {
        return serviceRequestRepository.findByStatus(status);
    }

    /* =====================================================
       PHASE 0 — DRAFT CREATION (NO CONTRACT ID!)
       ===================================================== */

    /**
     * Creates a ServiceRequest in DRAFT state.
     * ❗ Contract ID MUST remain NULL.
     * It will be created ONLY after 2B validation.
     */
    public ServiceRequest createServiceRequest(ServiceRequest serviceRequest) {

        serviceRequest.setStatus(ServiceRequestStatus.DRAFT);

        // Mock external Workforce ID (allowed at draft stage)
        if (serviceRequest.getInternalRequestId() == null) {
            serviceRequest.setInternalRequestId(
                    1000L + (long) (Math.random() * 1000)
            );
        }

        // ❌ DO NOT SET CONTRACT ID HERE
        // serviceRequest.setContractId(null);

        return serviceRequestRepository.save(serviceRequest);
    }

    /* =====================================================
       PHASE 1 — START CAMUNDA PROCESS
       ===================================================== */

    /* =====================================================
       PHASE 1 — START CAMUNDA PROCESS
       ===================================================== */
    public void startProcessForRequest(Long requestId) {

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != ServiceRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT requests can be started.");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", request.getId());
        variables.put("title", request.getTitle());
        variables.put("description", request.getDescription());
        variables.put("procurementOfficer", "po_user");
        variables.put("projectManager", "pm_user");

        // 1. Start Process (This runs CheckContractDelegate synchronously!)
        String processInstanceId = camundaProcessService.startServiceRequestProcess(variables);

        System.out.println(">>> BPMN STARTED | Request ID=" + requestId + " | Instance=" + processInstanceId);

        // 2. RE-FETCH from DB to check if Delegate changed status to REJECTED
        ServiceRequest updatedRequest = serviceRequestRepository.findById(requestId).orElseThrow();

        // 3. Only set to WAITING_APPROVAL if it wasn't already rejected/finished
        if (updatedRequest.getStatus() != ServiceRequestStatus.REJECTED
                && updatedRequest.getStatus() != ServiceRequestStatus.COMPLETED) {

            updatedRequest.setStatus(ServiceRequestStatus.WAITING_APPROVAL);
            serviceRequestRepository.save(updatedRequest);
        }
    }

    /* =====================================================
       STATUS MANAGEMENT
       ===================================================== */

    public ServiceRequest updateServiceRequestStatus(
            Long id,
            ServiceRequestStatus status) {

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("ServiceRequest not found: " + id));

        request.setStatus(status);
        return serviceRequestRepository.save(request);
    }

    /**
     * Formal rejection (audit trail).
     */
    public ServiceRequest rejectServiceRequest(Long id, String reason) {

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("ServiceRequest not found: " + id));

        request.setStatus(ServiceRequestStatus.REJECTED);
        request.setRejectionReason(reason);

        return serviceRequestRepository.save(request);
    }

    /* =====================================================
       SAFE DELETE (ADMIN / TEST)
       ===================================================== */

    public void deleteServiceRequest(Long id) {

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("ServiceRequest not found: " + id));

        // Delete children first
        providerOfferRepository.deleteByServiceRequest(request);

        serviceRequestRepository.delete(request);
    }
}
