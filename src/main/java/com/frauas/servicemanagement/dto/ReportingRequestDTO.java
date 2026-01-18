package com.frauas.servicemanagement.dto;

import com.frauas.servicemanagement.entity.ServiceRequestStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportingRequestDTO {
    public Long id;
    public String title;
    public String projectName;
    public ServiceRequestStatus status;
    public String contractId;
    public Double hourlyRate;
    public LocalDate startDate;
    public LocalDate endDate;
    public LocalDateTime createdAt;

    // Constructor for easy mapping
    public ReportingRequestDTO(Long id, String title, String projectName, ServiceRequestStatus status,
                               String contractId, Double hourlyRate, LocalDate startDate,
                               LocalDate endDate, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.projectName = projectName;
        this.status = status;
        this.contractId = contractId;
        this.hourlyRate = hourlyRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }
}