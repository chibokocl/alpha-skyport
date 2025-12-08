package com.alphaskyport.masterdata.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "freight_services")
@Data
@NoArgsConstructor
public class FreightService implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "service_type", nullable = false)
    private String serviceType; // 'sea', 'air', 'land'

    private String description;

    @Column(name = "base_rate")
    private BigDecimal baseRate;

    private String currency = "USD";

    @Column(name = "estimated_days_min")
    private Integer estimatedDaysMin;

    @Column(name = "estimated_days_max")
    private Integer estimatedDaysMax;

    @Column(name = "max_daily_capacity_kg")
    private BigDecimal maxDailyCapacityKg;

    @Column(name = "max_daily_capacity_m3")
    private BigDecimal maxDailyCapacityM3;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "cache_version")
    private Integer cacheVersion = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
