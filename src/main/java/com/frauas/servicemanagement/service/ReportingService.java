package com.frauas.servicemanagement.service;

import com.frauas.servicemanagement.dto.ReportingOfferDTO;
import com.frauas.servicemanagement.dto.ReportingRequestDTO;
import com.frauas.servicemanagement.entity.*;
import com.frauas.servicemanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @Autowired private ServiceRequestRepository serviceRequestRepository;
    @Autowired private ProviderOfferRepository providerOfferRepository;

    // --- ISSUE 1 & 4 FIX: CLEAN DTO MAPPING ---
    @Transactional(readOnly = true)
    public List<ReportingRequestDTO> getAllRequestsClean() {
        return serviceRequestRepository.findAll().stream()
                .map(req -> new ReportingRequestDTO(
                        req.getId(),
                        req.getTitle(),
                        req.getInternalProjectName(),
                        req.getStatus(),
                        req.getContractId(),
                        req.getHourlyRate(),
                        req.getStartDate(),
                        req.getEndDate(),
                        req.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // --- ISSUE 2 FIX: CLEAN DTO MAPPING (Avoids Hibernate Proxy) ---
    @Transactional(readOnly = true)
    public List<ReportingOfferDTO> getAllOffersClean() {
        return providerOfferRepository.findAll().stream()
                .map(offer -> new ReportingOfferDTO(
                        offer.getId(),
                        offer.getServiceRequest().getId(), // Safe ID access
                        offer.getProviderName(),
                        offer.getSpecialistName(),
                        offer.getTotalCost(),
                        offer.getHourlyRate(),
                        offer.getTotalScore(),
                        offer.getStatus()
                ))
                .collect(Collectors.toList());
    }

    // --- ISSUE 4 FIX: HIERARCHICAL VIEW ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCombinedReport() {
        List<ServiceRequest> requests = serviceRequestRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (ServiceRequest req : requests) {
            Map<String, Object> entry = new HashMap<>();

            // Request Info
            entry.put("requestId", req.getId());
            entry.put("project", req.getInternalProjectName());
            entry.put("role", req.getTitle());
            entry.put("status", req.getStatus());

            // Associated Offers (Mapped to DTOs)
            List<ReportingOfferDTO> offers = providerOfferRepository.findByServiceRequest(req).stream()
                    .map(o -> new ReportingOfferDTO(
                            o.getId(),
                            req.getId(),
                            o.getProviderName(),
                            o.getSpecialistName(),
                            o.getTotalCost(),
                            o.getHourlyRate(),
                            o.getTotalScore(),
                            o.getStatus()
                    ))
                    .collect(Collectors.toList());

            entry.put("offers", offers);
            report.add(entry);
        }
        return report;
    }

    // --- EXISTING SUMMARY METHODS ---
    public Map<String, Object> getServiceRequestSummary() {
        List<ServiceRequest> all = serviceRequestRepository.findAll();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRequests", all.size());
        Map<ServiceRequestStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));
        summary.put("byStatus", byStatus);
        return summary;
    }

    public Map<String, Object> getOfferStatistics() {
        List<ProviderOffer> offers = providerOfferRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOffers", offers.size());
        return stats;
    }

    public List<Map<String, Object>> getProviderRankings() {
        Map<String, List<ProviderOffer>> grouped = providerOfferRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProviderOffer::getProviderName));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Map<String, Object> p = new HashMap<>();
            p.put("providerName", entry.getKey());
            p.put("totalOffers", entry.getValue().size());
            long wins = entry.getValue().stream()
                    .filter(o -> o.getStatus() == OfferStatus.SELECTED).count(); // Ensure status matches ENUM
            p.put("wins", wins);
            ranking.add(p);
        }
        return ranking;
    }
}