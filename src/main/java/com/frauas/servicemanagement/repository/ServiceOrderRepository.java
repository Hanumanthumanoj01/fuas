package com.frauas.servicemanagement.repository;

import com.frauas.servicemanagement.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    // Basic CRUD is enough for now
}