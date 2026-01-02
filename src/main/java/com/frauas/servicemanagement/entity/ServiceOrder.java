package com.frauas.servicemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_orders")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the request
    @OneToOne
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    // Snapshot of the winning offer details
    private String supplierName;
    private String specialistName;
    private Double agreedRate;
    private Integer totalManDays;
    private Double totalContractValue;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ServiceOrderStatus status;

    private LocalDateTime createdTime;

    public ServiceOrder() {
        this.createdTime = LocalDateTime.now();
        this.status = ServiceOrderStatus.ACTIVE;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ServiceRequest getServiceRequest() { return serviceRequest; }
    public void setServiceRequest(ServiceRequest serviceRequest) { this.serviceRequest = serviceRequest; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSpecialistName() { return specialistName; }
    public void setSpecialistName(String specialistName) { this.specialistName = specialistName; }

    public Double getAgreedRate() { return agreedRate; }
    public void setAgreedRate(Double agreedRate) { this.agreedRate = agreedRate; }

    public Integer getTotalManDays() { return totalManDays; }
    public void setTotalManDays(Integer totalManDays) { this.totalManDays = totalManDays; }

    public Double getTotalContractValue() { return totalContractValue; }
    public void setTotalContractValue(Double totalContractValue) { this.totalContractValue = totalContractValue; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ServiceOrderStatus getStatus() { return status; }
    public void setStatus(ServiceOrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedTime() { return createdTime; }
}