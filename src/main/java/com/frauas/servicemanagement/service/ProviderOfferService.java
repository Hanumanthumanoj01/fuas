package com.frauas.servicemanagement.service;

import com.frauas.servicemanagement.entity.ProviderOffer;
import com.frauas.servicemanagement.entity.ServiceRequest;
import com.frauas.servicemanagement.repository.ProviderOfferRepository;
import com.frauas.servicemanagement.repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProviderOfferService {

    @Autowired
    private ProviderOfferRepository providerOfferRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Transactional(readOnly = true)
    public List<ProviderOffer> getOffersByServiceRequest(Long serviceRequestId) {
        return providerOfferRepository.findByServiceRequestId(serviceRequestId);
    }

    public ProviderOffer submitOffer(Long serviceRequestId, ProviderOffer offer) {
        Optional<ServiceRequest> serviceRequest = serviceRequestRepository.findById(serviceRequestId);
        if (serviceRequest.isPresent()) {
            offer.setServiceRequest(serviceRequest.get());
            return providerOfferRepository.save(offer);
        }
        return null;
    }

    /**
     * THE ALGORITHM (UPDATED):
     * 1. Updates Technical Score (from User Input)
     * 2. Auto-calculates Commercial Score based on TOTAL COST (Not just rate)
     * 3. Calculates Total Weighted Score
     */
    public void calculateRanking(Long requestId) {
        List<ProviderOffer> offers = getOffersByServiceRequest(requestId);
        if (offers.isEmpty()) return;

        ServiceRequest request = offers.get(0).getServiceRequest();

        // Find Lowest Total Cost
        double minTotal = offers.stream()
                .mapToDouble(o -> (o.getTotalCost() != null) ? o.getTotalCost() : 0.0)
                .filter(c -> c > 0)
                .min().orElse(1.0);

        for (ProviderOffer offer : offers) {
            double actualCost = (offer.getTotalCost() != null && offer.getTotalCost() > 0)
                    ? offer.getTotalCost() : 1.0;

            // Commercial Logic: (LowestCost / ActualCost) * 100
            double commScore = (minTotal / actualCost) * 100.0;
            offer.setCommercialScore(Math.round(commScore * 100.0) / 100.0);

            // Total Score Logic
            double techWeight = request.getTechnicalWeighting() / 100.0;
            double commWeight = request.getCommercialWeighting() / 100.0;

            double total = (offer.getTechnicalScore() * techWeight) + (commScore * commWeight);
            offer.setTotalScore(Math.round(total * 100.0) / 100.0);

            providerOfferRepository.save(offer);
        }
    }

    // Called when Resource Planner inputs a technical score
    public void updateTechnicalScore(Long offerId, Double score) {
        Optional<ProviderOffer> opt = providerOfferRepository.findById(offerId);
        if(opt.isPresent()) {
            ProviderOffer offer = opt.get();
            offer.setTechnicalScore(score);
            providerOfferRepository.save(offer);
            // Recalculate rankings for this request
            calculateRanking(offer.getServiceRequest().getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ProviderOffer> getAllOffers() {
        return providerOfferRepository.findAll();
    }

    public Optional<ProviderOffer> getOfferById(Long id) {
        return providerOfferRepository.findById(id);
    }
}