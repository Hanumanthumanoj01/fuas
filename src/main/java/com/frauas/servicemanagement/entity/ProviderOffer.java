package com.frauas.servicemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "provider_offers")
public class ProviderOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // RELATION
    // =========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    // =========================
    // 4B INPUT / 1B OUTPUT
    // =========================
    private String externalOfferId;      // 1B: externalEmployeeId
    private String providerName;         // 1B: provider

    private String firstName;             // 1B: firstName
    private String lastName;              // 1B: lastName
    private String email;                 // 1B: email

    private Double hourlyRate;            // 1B: wagePerHour
    private Float experienceYears;        // 1B: experienceYears

    @Column(columnDefinition = "TEXT")
    private String skills;                // 1B: skills (raw array string)

    // =========================
    // 4B CONTRACT REFERENCE  ✅
    // =========================
    private String contractId;            // 4B contract reference

    // =========================
    // INTERNAL / SCORING
    // =========================
    private String specialistName;        // UI helper
    private Double dailyRate;             // hourlyRate * 8
    private Double totalCost;             // Used for commercial scoring

    private Integer onsiteDays;
    private Double travelCost;
    private String contractType;

    private Double technicalScore = 0.0;
    private Double commercialScore = 0.0;
    private Double totalScore = 0.0;

    @Enumerated(EnumType.STRING)
    private OfferStatus status = OfferStatus.SUBMITTED;

    private LocalDateTime submittedAt = LocalDateTime.now();

    // =========================
    // GETTERS & SETTERS
    // =========================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ServiceRequest getServiceRequest() { return serviceRequest; }
    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public String getExternalOfferId() { return externalOfferId; }
    public void setExternalOfferId(String externalOfferId) {
        this.externalOfferId = externalOfferId;
    }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateSpecialistName();
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateSpecialistName();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
        if (hourlyRate != null) {
            this.dailyRate = hourlyRate * 8;
        }
    }

    public Float getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Float experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    // ===== NEW FIELD =====
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getSpecialistName() { return specialistName; }
    public void setSpecialistName(String specialistName) {
        this.specialistName = specialistName;
    }

    public Double getDailyRate() { return dailyRate; }
    public void setDailyRate(Double dailyRate) { this.dailyRate = dailyRate; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Integer getOnsiteDays() { return onsiteDays; }
    public void setOnsiteDays(Integer onsiteDays) { this.onsiteDays = onsiteDays; }

    public Double getTravelCost() { return travelCost; }
    public void setTravelCost(Double travelCost) { this.travelCost = travelCost; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public Double getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Double technicalScore) {
        this.technicalScore = technicalScore;
    }

    public Double getCommercialScore() { return commercialScore; }
    public void setCommercialScore(Double commercialScore) {
        this.commercialScore = commercialScore;
    }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    // =========================
    // INTERNAL HELPERS
    // =========================
    private void updateSpecialistName() {
        if (firstName != null && lastName != null) {
            this.specialistName = firstName + " " + lastName;
        }
    }
}
