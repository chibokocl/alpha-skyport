package com.alphaskyport.logistics.controller;

import com.alphaskyport.logistics.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/pricing")
@RequiredArgsConstructor
@Tag(name = "Public Pricing", description = "Public API for price estimation")
public class PublicPriceController {

    private final PricingService pricingService;

    @PostMapping("/estimate")
    @Operation(summary = "Get Price Estimate", description = "Calculates shipping cost based on weight, dimensions, and route")
    public ResponseEntity<PriceEstimateResponse> getEstimate(@RequestBody PriceEstimateRequest request) {
        BigDecimal estimatedPrice = pricingService.calculateEstimate(
                request.getWeightKg(),
                request.getLengthCm(),
                request.getWidthCm(),
                request.getHeightCm(),
                request.getOriginCountryCode(),
                request.getDestinationCountryCode());

        return ResponseEntity.ok(new PriceEstimateResponse(estimatedPrice, "USD"));
    }

    @Data
    public static class PriceEstimateRequest {
        private BigDecimal weightKg;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private String originCountryCode;
        private String destinationCountryCode;
    }

    @Data
    public static class PriceEstimateResponse {
        private final BigDecimal amount;
        private final String currency;
    }
}
