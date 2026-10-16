package com.distributed.ecommerce.inventory.repository;

import com.distributed.ecommerce.inventory.entity.ProcessedInventoryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedInventoryOrderRepository
        extends JpaRepository<ProcessedInventoryOrder, Long> {
}