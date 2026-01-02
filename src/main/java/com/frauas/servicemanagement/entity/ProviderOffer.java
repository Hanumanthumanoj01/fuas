package com.frauas.servicemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "provider_offers")
public class ProviderOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to our request
    @ManyToOne
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    // --- FIELDS FROM GROUP 4B CSV ---
    private String externalOfferId;      // "Offer ID" from their system
    private String providerName;         // "Company"
    private String serviceType;          // "Service Type" (e.g., Expert, Low Wage)
    private String specialistName;       // "Specialist Name"
    private Double dailyRate;            // "Daily Rate"
    private Integer onsiteDays;          // "Onsite Days"
    private Double travelCost;           // "Travelling cost"
    private Double totalCost;            // "Total cost"
    private String contractType;         // "Contractual relationship"
    private String skills;               // "Skills"

    // --- INTERNAL FIELDS ---
    private Double technicalScore;       // 0-100 (Assigned by PM)
    private Double commercialScore;      // Calculated from Total Cost
    private Double totalScore;           // Weighted

    @Enumerated(EnumType.STRING)
    private OfferStatus status;          // SUBMITTED, SELECTED, REJECTED

    private LocalDateTime submittedAt;

    public ProviderOffer() {
        this.submittedAt = LocalDateTime.now();
        this.status = OfferStatus.SUBMITTED;
        this.technicalScore = 0.0;
        this.commercialScore = 0.0;
        this.totalScore = 0.0;
    }

    // --- CONSTRUCTOR FOR MOCKING ---
    public ProviderOffer(ServiceRequest req, String provider, String specialist,
                         Double rate, Integer onsite, Double travel, Double total) {
        this();
        this.serviceRequest = req;
        this.providerName = provider;
        this.specialistName = specialist;
        this.dailyRate = rate;
        this.onsiteDays = onsite;
        this.travelCost = travel;
        this.totalCost = total;
    }

    // --- GETTERS & SETTERS (Generate Standard) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ServiceRequest getServiceRequest() { return serviceRequest; }
    public void setServiceRequest(ServiceRequest serviceRequest) { this.serviceRequest = serviceRequest; }
    public String getExternalOfferId() { return externalOfferId; }
    public void setExternalOfferId(String externalOfferId) { this.externalOfferId = externalOfferId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getSpecialistName() { return specialistName; }
    public void setSpecialistName(String specialistName) { this.specialistName = specialistName; }
    public Double getDailyRate() { return dailyRate; }
    public void setDailyRate(Double dailyRate) { this.dailyRate = dailyRate; }
    public Integer getOnsiteDays() { return onsiteDays; }
    public void setOnsiteDays(Integer onsiteDays) { this.onsiteDays = onsiteDays; }
    public Double getTravelCost() { return travelCost; }
    public void setTravelCost(Double travelCost) { this.travelCost = travelCost; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public Double getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Double technicalScore) { this.technicalScore = technicalScore; }
    public Double getCommercialScore() { return commercialScore; }
    public void setCommercialScore(Double commercialScore) { this.commercialScore = commercialScore; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}