package com.alphaskyport.logistics.controller;

import com.alphaskyport.logistics.service.ShipmentService;
import com.alphaskyport.logistics.model.Shipment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/public/shipments")
@RequiredArgsConstructor
@Tag(name = "Public Shipments", description = "Public API for booking shipments")
public class PublicShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping("/book")
    @Operation(summary = "Book a Shipment", description = "Creates a new shipment booking")
    public ResponseEntity<BookingResponse> bookShipment(@RequestBody BookingRequest request) {
        Shipment shipment = shipmentService.createPublicShipment(
                request.getSenderName(),
                request.getSenderEmail(),
                request.getReceiverName(),
                request.getReceiverAddress(),
                request.getWeightKg(),
                request.getLengthCm(),
                request.getWidthCm(),
                request.getHeightCm(),
                request.getPickupDate(),
                request.getOriginCountryCode(),
                request.getDestinationCountryCode(),
                request.getEstimatedPrice(),
                request.getCurrency());

        return ResponseEntity.ok(new BookingResponse(
                shipment.getShipmentId(),
                shipment.getTrackingNumber(),
                "Booking confirmed. Please check your email for details."));
    }

    @Data
    public static class BookingRequest {
        private String senderName;
        private String senderEmail;
        private String receiverName;
        private String receiverAddress;
        private BigDecimal weightKg;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private LocalDate pickupDate; // Format: YYYY-MM-DD
        private String originCountryCode;
        private String destinationCountryCode;
        private BigDecimal estimatedPrice;
        private String currency;
    }

    @Data
    public static class BookingResponse {
        private final java.util.UUID shipmentId;
        private final String trackingNumber;
        private final String message;
    }
}
