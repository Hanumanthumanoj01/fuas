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

    private String title;

    @Column(columnDefinition = "TEXT") // Allow long text
    private String description;

    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    private Integer durationDays;
    private String projectContext; // Maps to "ProjectName"
    private String clientName;     // New: Requested by 4b

    private LocalDate startDate;
    private LocalDate endDate;

    // --- NEW FIELDS FOR GROUP 4B MATCHING ---
    private Integer minExperience; // Years
    private Double maxDailyRate;   // Budget Cap
    private LocalDate submissionDeadline; // When providers must stop

    private String performanceLocation; // Onsite/Remote

    // Weighting
    private Integer commercialWeighting;
    private Integer technicalWeighting;

    // References
    private Long contractId;
    private Long internalRequestId; // Link to Group 1b

    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = ServiceRequestStatus.DRAFT;
        this.commercialWeighting = 50;
        this.technicalWeighting = 50;
        this.clientName = "FraUAS IT Dept"; // Default
        this.submissionDeadline = LocalDate.now().plusDays(7); // Default 1 week
    }

    // --- GETTERS & SETTERS (Generate all, specifically these new ones) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ServiceRequestStatus getStatus() { return status; }
    public void setStatus(ServiceRequestStatus status) { this.status = status; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public String getProjectContext() { return projectContext; }
    public void setProjectContext(String projectContext) { this.projectContext = projectContext; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }
    public Double getMaxDailyRate() { return maxDailyRate; }
    public void setMaxDailyRate(Double maxDailyRate) { this.maxDailyRate = maxDailyRate; }
    public LocalDate getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(LocalDate submissionDeadline) { this.submissionDeadline = submissionDeadline; }
    public String getPerformanceLocation() { return performanceLocation; }
    public void setPerformanceLocation(String performanceLocation) { this.performanceLocation = performanceLocation; }
    public Integer getCommercialWeighting() { return commercialWeighting; }
    public void setCommercialWeighting(Integer commercialWeighting) { this.commercialWeighting = commercialWeighting; }
    public Integer getTechnicalWeighting() { return technicalWeighting; }
    public void setTechnicalWeighting(Integer technicalWeighting) { this.technicalWeighting = technicalWeighting; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getInternalRequestId() { return internalRequestId; }
    public void setInternalRequestId(Long internalRequestId) { this.internalRequestId = internalRequestId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ServiceRequestStatus.DRAFT;
    }
    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}