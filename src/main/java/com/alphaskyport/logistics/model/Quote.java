package com.alphaskyport.logistics.model;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.masterdata.model.Country;
import com.alphaskyport.masterdata.model.FreightService;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Data
@NoArgsConstructor
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quote_id")
    private UUID quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_country_id", nullable = false)
    private Country originCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_country_id", nullable = false)
    private Country destinationCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private FreightService service;

    @Column(name = "is_residential")
    private boolean isResidential = false;

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

    @Column(name = "cargo_value")
    private BigDecimal cargoValue;

    @Column(name = "cargo_currency")
    private String cargoCurrency = "USD";

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(name = "quote_status")
    private String quoteStatus = "pending"; // pending, calculating, quoted, accepted, rejected, expired, converted

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pricing_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> pricingSnapshot;

    @Column(name = "quoted_price")
    private BigDecimal quotedPrice;

    @Column(name = "quoted_at")
    private LocalDateTime quotedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "converted_to_shipment_id")
    private UUID convertedToShipmentId;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Version
    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
