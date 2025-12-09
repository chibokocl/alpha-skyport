package com.alphaskyport.logistics.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment_reservations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "shipment_id", "booking_id" })
})
@Data
@NoArgsConstructor
public class ShipmentReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id")
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private CapacityBooking booking;

    @Column(name = "reserved_weight_kg", nullable = false)
    private BigDecimal reservedWeightKg;

    @Column(name = "reserved_volume_m3", nullable = false)
    private BigDecimal reservedVolumeM3;

    @Column(name = "reservation_status")
    private String reservationStatus = "active"; // active, released, confirmed

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
