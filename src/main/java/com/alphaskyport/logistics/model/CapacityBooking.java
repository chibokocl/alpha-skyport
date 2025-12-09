package com.alphaskyport.logistics.model;

import com.alphaskyport.masterdata.model.FreightService;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "service_id", "booking_date" })
})
@Data
@NoArgsConstructor
public class CapacityBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private FreightService service;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "reserved_weight_kg")
    private BigDecimal reservedWeightKg = BigDecimal.ZERO;

    @Column(name = "reserved_volume_m3")
    private BigDecimal reservedVolumeM3 = BigDecimal.ZERO;

    @Column(name = "max_weight_kg")
    private BigDecimal maxWeightKg;

    @Column(name = "max_volume_m3")
    private BigDecimal maxVolumeM3;

    @Version
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
