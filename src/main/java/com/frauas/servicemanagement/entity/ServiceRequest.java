package com.frauas.servicemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // 1B MAPPING (Workforce)
    // =========================
    private String title;                // jobTitle
    private Long internalRequestId;      // internalRequestId
    private Long internalProjectId;      // projectId
    private String internalProjectName;  // projectName
    private Integer hoursPerWeek;        // availabilityHoursPerWeek
    private Double hourlyRate;           // wagePerHour
    private String performanceLocation;  // location

    @Column(columnDefinition = "TEXT")
    private String description;          // description

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;       // skills (joined)

    // =========================
    // RAW PAYLOAD (1B AUDIT)
    // =========================
    @Column(columnDefinition = "TEXT")
    private String rawPayload;            // Original JSON from 1b

    // =========================
    // INTERNAL / 2B / 4B
    // =========================
    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus status;

    private String contractId;           // FROM GROUP 2B (CTR-xxxx)

    private Integer minExperience;       // experienceYears
    private Double maxDailyRate;         // hourlyRate * 8

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    private String projectContext;
    private String rejectionReason;

    // =========================
    // 2B – OFFER EVALUATION
    // =========================
    private Integer commercialWeighting;
    private Integer technicalWeighting;

    // =========================
    // AUDIT
    // =========================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================
    // CONSTRUCTOR
    // =========================
    public ServiceRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = ServiceRequestStatus.DRAFT;
    }

    // =========================
    // FORMATTED REQUIREMENT ID
    // =========================
    /**
     * Display-only formatted ID.
     * Example: REQ-2025-045
     */
    @Transient
    public String getFormattedId() {
        if (id == null) {
            return "REQ-2025-XXX";
        }
        return "REQ-2025-" + String.format("%03d", id);
    }

    // =========================
    // GETTERS & SETTERS
    // =========================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getInternalRequestId() { return internalRequestId; }
    public void setInternalRequestId(Long internalRequestId) {
        this.internalRequestId = internalRequestId;
    }

    public Long getInternalProjectId() { return internalProjectId; }
    public void setInternalProjectId(Long internalProjectId) {
        this.internalProjectId = internalProjectId;
    }

    public String getInternalProjectName() { return internalProjectName; }
    public void setInternalProjectName(String internalProjectName) {
        this.internalProjectName = internalProjectName;
    }

    public Integer getHoursPerWeek() { return hoursPerWeek; }
    public void setHoursPerWeek(Integer hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getPerformanceLocation() { return performanceLocation; }
    public void setPerformanceLocation(String performanceLocation) {
        this.performanceLocation = performanceLocation;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public ServiceRequestStatus getStatus() { return status; }
    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) {
        this.minExperience = minExperience;
    }

    public Double getMaxDailyRate() { return maxDailyRate; }
    public void setMaxDailyRate(Double maxDailyRate) {
        this.maxDailyRate = maxDailyRate;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public String getProjectContext() { return projectContext; }
    public void setProjectContext(String projectContext) {
        this.projectContext = projectContext;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Integer getCommercialWeighting() {
        return commercialWeighting != null ? commercialWeighting : 50;
    }

    public void setCommercialWeighting(Integer commercialWeighting) {
        this.commercialWeighting = commercialWeighting;
    }

    public Integer getTechnicalWeighting() {
        return technicalWeighting != null ? technicalWeighting : 50;
    }

    public void setTechnicalWeighting(Integer technicalWeighting) {
        this.technicalWeighting = technicalWeighting;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // =========================
    // JPA LIFECYCLE
    // =========================
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ServiceRequestStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
