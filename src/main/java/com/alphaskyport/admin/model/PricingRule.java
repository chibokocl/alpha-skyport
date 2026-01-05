package com.alphaskyport.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "rule_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PricingRuleType ruleType;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> conditions;

    @Column(name = "adjustment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @Column(name = "adjustment_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustmentValue;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AdminUser createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = validFrom == null || now.isAfter(validFrom);
        boolean beforeEnd = validUntil == null || now.isBefore(validUntil);
        return isActive && afterStart && beforeEnd;
    }

    public BigDecimal applyTo(BigDecimal basePrice) {
        return switch (adjustmentType) {
            case PERCENTAGE -> basePrice.multiply(BigDecimal.ONE.add(adjustmentValue.divide(BigDecimal.valueOf(100))));
            case FIXED -> basePrice.add(adjustmentValue);
            case SET_PRICE -> adjustmentValue;
            case BASE_RATE_PER_KG -> basePrice; // Not applicable for direct application usually, used in service logic
            case MULTIPLIER -> basePrice.multiply(adjustmentValue);
        };
    }
}
