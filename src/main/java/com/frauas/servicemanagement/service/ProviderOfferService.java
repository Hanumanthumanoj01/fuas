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

    // =========================
    // FETCHING
    // =========================
    @Transactional(readOnly = true)
    public List<ProviderOffer> getOffersByServiceRequest(Long serviceRequestId) {
        return providerOfferRepository.findByServiceRequestId(serviceRequestId);
    }

    @Transactional(readOnly = true)
    public List<ProviderOffer> getAllOffers() {
        return providerOfferRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ProviderOffer> getOfferById(Long id) {
        return providerOfferRepository.findById(id);
    }

    // =========================
    // SUBMIT OFFER
    // =========================
    public ProviderOffer submitOffer(Long serviceRequestId, ProviderOffer offer) {

        Optional<ServiceRequest> serviceRequestOpt =
                serviceRequestRepository.findById(serviceRequestId);

        if (serviceRequestOpt.isEmpty()) {
            return null;
        }

        offer.setServiceRequest(serviceRequestOpt.get());
        return providerOfferRepository.save(offer);
    }

    // =========================
    // RANKING & SCORING LOGIC
    // =========================
    /**
     * SCORING MODEL:
     * - Commercial Score = (lowestTotalCost / offerTotalCost) * 100
     * - Technical Score  = entered by RP (0–100)
     * - Total Score      = weighted sum
     */
    public void calculateRanking(Long requestId) {

        List<ProviderOffer> offers = getOffersByServiceRequest(requestId);
        if (offers == null || offers.isEmpty()) {
            return;
        }

        ServiceRequest request = offers.get(0).getServiceRequest();

        // -------------------------
        // ✅ FIX: FORCE DOUBLE DIVISION
        // -------------------------
        double techWeight =
                (request.getTechnicalWeighting() != null
                        ? request.getTechnicalWeighting()
                        : 50) / 100.0;

        double commWeight =
                (request.getCommercialWeighting() != null
                        ? request.getCommercialWeighting()
                        : 50) / 100.0;

        // -------------------------
        // FIND LOWEST TOTAL COST
        // -------------------------
        double minTotalCost = offers.stream()
                .map(ProviderOffer::getTotalCost)
                .filter(c -> c != null && c > 0)
                .min(Double::compareTo)
                .orElse(1.0); // hard safety

        // -------------------------
        // CALCULATE SCORES
        // -------------------------
        for (ProviderOffer offer : offers) {

            double actualCost =
                    (offer.getTotalCost() != null && offer.getTotalCost() > 0)
                            ? offer.getTotalCost()
                            : minTotalCost;

            // ✅ COMMERCIAL SCORE (0–100)
            double commercialScore =
                    (minTotalCost / actualCost) * 100.0;

            commercialScore =
                    Math.round(commercialScore * 100.0) / 100.0;

            offer.setCommercialScore(commercialScore);

            // TECH SCORE (safe default)
            double technicalScore =
                    offer.getTechnicalScore() != null
                            ? offer.getTechnicalScore()
                            : 0.0;

            // FINAL SCORE
            double totalScore =
                    (technicalScore * techWeight) +
                            (commercialScore * commWeight);

            totalScore =
                    Math.round(totalScore * 100.0) / 100.0;

            offer.setTotalScore(totalScore);

            providerOfferRepository.save(offer);
        }
    }

    // =========================
    // UPDATE TECHNICAL SCORE
    // =========================
    public void updateTechnicalScore(Long offerId, Double score) {

        Optional<ProviderOffer> opt =
                providerOfferRepository.findById(offerId);

        if (opt.isEmpty()) {
            return;
        }

        ProviderOffer offer = opt.get();
        offer.setTechnicalScore(score);
        providerOfferRepository.save(offer);

        if (offer.getServiceRequest() != null) {
            calculateRanking(offer.getServiceRequest().getId());
        }
    }
}
