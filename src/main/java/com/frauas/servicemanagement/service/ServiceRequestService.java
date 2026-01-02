package com.frauas.servicemanagement.service;

import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
       PHASE 0 — PURE DATA ENTRY (DRAFT)
       ===================================================== */

    /**
     * Creates a ServiceRequest in DRAFT state.
     * ❗ Does NOT start Camunda.
     * This represents simple data entry by the PM.
     */
    public ServiceRequest createServiceRequest(ServiceRequest serviceRequest) {

        serviceRequest.setStatus(ServiceRequestStatus.DRAFT);

        // Mocked external dependencies (as per project constraints)
        serviceRequest.setInternalRequestId(
                1000L + (long) (Math.random() * 1000)
        );
        serviceRequest.setContractId(
                2000L + (long) (Math.random() * 1000)
        );

        return serviceRequestRepository.save(serviceRequest);
    }

    /* =====================================================
       PHASE 1 — SUBMIT (START BPMN PROCESS)
       ===================================================== */

    /**
     * Starts the Camunda process for an existing ServiceRequest.
     * This method is called ONLY when PM clicks "Submit".
     */
    public void startProcessForRequest(Long requestId) {

        Optional<ServiceRequest> optionalRequest =
                serviceRequestRepository.findById(requestId);

        if (optionalRequest.isEmpty()) {
            throw new RuntimeException(
                    "Cannot start process. ServiceRequest not found: " + requestId
            );
        }

        ServiceRequest request = optionalRequest.get();

        if (request.getStatus() != ServiceRequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT requests can be submitted to workflow."
            );
        }

        // Start Camunda workflow
        String processInstanceId =
                camundaProcessService.startServiceRequestProcess(
                        request.getId(),
                        request.getTitle(),
                        request.getDescription()
                );

        // Update status to reflect workflow ownership
        request.setStatus(ServiceRequestStatus.WAITING_APPROVAL);
        serviceRequestRepository.save(request);

        System.out.println(
                ">>> BPMN PROCESS STARTED | Request ID: " + requestId +
                        " | Camunda Instance: " + processInstanceId
        );
    }

    /* =====================================================
       STATUS MANAGEMENT
       ===================================================== */

    public ServiceRequest updateServiceRequestStatus(
            Long id,
            ServiceRequestStatus status
    ) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("ServiceRequest not found: " + id));

        request.setStatus(status);
        return serviceRequestRepository.save(request);
    }

    /**
     * Handles formal rejection with reason (audit requirement).
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
       SAFE DELETION (ADMIN / TEST USE ONLY)
       ===================================================== */

    /**
     * Deletes a ServiceRequest safely by removing dependent
     * ProviderOffers first to avoid FK violations.
     */
    public void deleteServiceRequest(Long id) {

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("ServiceRequest not found: " + id));

        // Delete children first
        providerOfferRepository.deleteByServiceRequest(request);

        // Delete parent
        serviceRequestRepository.delete(request);
    }
}
