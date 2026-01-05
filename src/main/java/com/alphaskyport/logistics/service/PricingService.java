package com.alphaskyport.logistics.service;

import com.alphaskyport.admin.model.PricingRule;
import com.alphaskyport.admin.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;

    // Standard IATA divisor for volumetric weight (cm^3 / 6000)
    private static final BigDecimal VOLUMETRIC_DIVISOR = new BigDecimal("6000");

    /**
     * Calculates the estimated price for a shipment.
     * 
     * @param weightKg               Actual weight in KG
     * @param lengthCm               Length in CM
     * @param widthCm                Width in CM
     * @param heightCm               Height in CM
     * @param originCountryCode      Origin Country Code (e.g., "US")
     * @param destinationCountryCode Destination Country Code (e.g., "TZ")
     * @return Estimated price
     */
    public BigDecimal calculateEstimate(BigDecimal weightKg, BigDecimal lengthCm, BigDecimal widthCm,
            BigDecimal heightCm,
            String originCountryCode, String destinationCountryCode) {

        // 1. Calculate Volumetric Weight
        BigDecimal volume = lengthCm.multiply(widthCm).multiply(heightCm);
        BigDecimal volumetricWeight = volume.divide(VOLUMETRIC_DIVISOR, 2, RoundingMode.HALF_UP);

        // 2. Determine Chargeable Weight (Max of Actual vs Volumetric)
        BigDecimal chargeableWeight = weightKg.max(volumetricWeight);

        // 3. Fetch Applicable Rules (Simplified: Fetch all and filter in memory for
        // MVP)
        // In production, use a more specific database query
        List<PricingRule> rules = pricingRuleRepository.findByIsActiveTrueOrderByPriorityDesc();

        BigDecimal basePrice = BigDecimal.ZERO;
        BigDecimal multipliers = BigDecimal.ONE;
        BigDecimal surcharges = BigDecimal.ZERO;

        // 4. Apply Rules
        for (PricingRule rule : rules) {
            if (matchesCondition(rule, originCountryCode, destinationCountryCode)) {
                switch (rule.getAdjustmentType()) {
                    case BASE_RATE_PER_KG:
                        // E.g., $10 per KG
                        basePrice = basePrice.add(rule.getAdjustmentValue().multiply(chargeableWeight));
                        break;
                    case FIXED:
                        // E.g., +$50 Handling Fee
                        surcharges = surcharges.add(rule.getAdjustmentValue());
                        break;
                    case PERCENTAGE:
                        // E.g. +10%
                        multipliers = multipliers.multiply(
                                BigDecimal.ONE.add(rule.getAdjustmentValue().divide(BigDecimal.valueOf(100))));
                        break;
                    case SET_PRICE:
                        // Override base price
                        basePrice = rule.getAdjustmentValue();
                        break;
                    case MULTIPLIER:
                        // E.g., 1.2x for Rush
                        multipliers = multipliers.multiply(rule.getAdjustmentValue());
                        break;
                }
            }
        }

        // If no base rate found, apply a default fallback (e.g., $5 * weight)
        // This prevents $0 estimates if no rules match
        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            basePrice = chargeableWeight.multiply(new BigDecimal("5.00"));
        }

        // 5. Final Calculation
        return basePrice.add(surcharges).multiply(multipliers).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean matchesCondition(PricingRule rule, String origin, String destination) {
        // Simplified JSON condition matching for MVP
        Map<String, Object> conditions = rule.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions = applies to all
        }

        // Check Origin
        if (conditions.containsKey("origin")) {
            String requiredOrigin = (String) conditions.get("origin");
            if (!requiredOrigin.equalsIgnoreCase(origin)) {
                return false;
            }
        }

        // Check Destination
        if (conditions.containsKey("destination")) {
            String requiredDest = (String) conditions.get("destination");
            if (!requiredDest.equalsIgnoreCase(destination)) {
                return false;
            }
        }

        return true;
    }
}
