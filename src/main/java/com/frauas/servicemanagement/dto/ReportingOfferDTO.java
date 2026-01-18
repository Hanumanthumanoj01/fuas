package com.frauas.servicemanagement.dto;

import com.frauas.servicemanagement.entity.OfferStatus;

public class ReportingOfferDTO {
    public Long offerId;
    public Long requestId; // Link back to request
    public String providerName;
    public String specialistName;
    public Double totalCost;
    public Double hourlyRate;
    public Double totalScore;
    public OfferStatus status;

    public ReportingOfferDTO(Long offerId, Long requestId, String providerName, String specialistName,
                             Double totalCost, Double hourlyRate, Double totalScore, OfferStatus status) {
        this.offerId = offerId;
        this.requestId = requestId;
        this.providerName = providerName;
        this.specialistName = specialistName;
        this.totalCost = totalCost;
        this.hourlyRate = hourlyRate;
        this.totalScore = totalScore;
        this.status = status;
    }
}