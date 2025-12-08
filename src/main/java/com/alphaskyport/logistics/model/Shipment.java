package com.alphaskyport.logistics.model;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.masterdata.model.Country;
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
@Table(name = "shipments")
@Data
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private FreightService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_country_id", nullable = false)
    private Country originCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_country_id", nullable = false)
    private Country destinationCountry;

    @Column(name = "origin_address", columnDefinition = "TEXT")
    private String originAddress;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    @Column(name = "is_residential_delivery")
    private boolean isResidentialDelivery = false;

    @Column(name = "cargo_description", columnDefinition = "TEXT")
    private String cargoDescription;

    @Column(name = "cargo_weight")
    private BigDecimal cargoWeight;

    @Column(name = "cargo_weight_unit")
    private String cargoWeightUnit = "kg";

    @Column(name = "cargo_volume")
    private BigDecimal cargoVolume;

    @Column(name = "cargo_volume_unit")
    private String cargoVolumeUnit = "m3";

    @Column(name = "declared_value")
    private BigDecimal declaredValue;

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "shipment_status")
    private String shipmentStatus = "pending"; // pending, confirmed, picked_up, in_transit, customs_clearance,
                                               // out_for_delivery, delivered, cancelled, returned, exception

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "estimated_pickup_date")
    private LocalDate estimatedPickupDate;

    @Column(name = "actual_pickup_date")
    private LocalDate actualPickupDate;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "payment_status")
    private String paymentStatus = "unpaid"; // unpaid, pending, partial, paid, refunded, failed

    @Column(name = "amount_paid")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due")
    private BigDecimal amountDue;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Version
    private Integer version;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
