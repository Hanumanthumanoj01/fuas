package com.frauas.servicemanagement.service;

import com.frauas.servicemanagement.entity.*;
import com.frauas.servicemanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @Autowired private ServiceRequestRepository serviceRequestRepository;
    @Autowired private ProviderOfferRepository providerOfferRepository;

    public Map<String, Object> getServiceRequestSummary() {
        List<ServiceRequest> all = serviceRequestRepository.findAll();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRequests", all.size());

        // Count by status
        Map<ServiceRequestStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));
        summary.put("byStatus", byStatus);

        return summary;
    }

    public Map<String, Object> getOfferStatistics() {
        List<ProviderOffer> offers = providerOfferRepository.findAll();
        List<ServiceRequest> requests = serviceRequestRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOffers", offers.size());

        double avg = requests.isEmpty() ? 0 : (double) offers.size() / requests.size();
        stats.put("avgOffersPerRequest", avg);

        return stats;
    }

    public List<Map<String, Object>> getProviderRankings() {
        // Group offers by Provider Name
        Map<String, List<ProviderOffer>> grouped = providerOfferRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProviderOffer::getProviderName));

        List<Map<String, Object>> ranking = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            Map<String, Object> p = new HashMap<>();
            p.put("providerName", entry.getKey());
            p.put("totalOffers", entry.getValue().size());

            // Calculate selection rate
            long wins = entry.getValue().stream()
                    .filter(o -> o.getStatus() == OfferStatus.SELECTED).count();
            p.put("wins", wins);

            ranking.add(p);
        }

        // Sort by total offers
        ranking.sort((a,b) -> Integer.compare((int)b.get("totalOffers"), (int)a.get("totalOffers")));
        return ranking;
    }
}