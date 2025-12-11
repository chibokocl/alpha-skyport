package com.alphaskyport.logistics.controller;

import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment management APIs")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping("/{quoteId}/create")
    @Operation(summary = "Create shipment from quote", description = "Converts an accepted quote into a shipment")
    public ResponseEntity<Shipment> createShipmentFromQuote(
            @PathVariable UUID quoteId,
            @RequestParam LocalDate pickupDate) {
        Shipment shipment = shipmentService.createShipmentFromQuote(quoteId, pickupDate);
        return ResponseEntity.ok(shipment);
    }

    @PutMapping("/{shipmentId}/status")
    @Operation(summary = "Update shipment status", description = "Updates the status of a shipment")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID shipmentId,
            @RequestParam String newStatus,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "manual") String source) {
        shipmentService.updateStatus(shipmentId, newStatus, description, source);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get shipment by ID", description = "Retrieves shipment details by ID")
    public ResponseEntity<Shipment> getShipment(@PathVariable UUID shipmentId) {
        return shipmentService.getShipmentById(shipmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Track shipment", description = "Retrieves shipment details by tracking number")
    public ResponseEntity<Shipment> trackShipment(@PathVariable String trackingNumber) {
        return shipmentService.getShipmentByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user shipments", description = "Retrieves all shipments for a user")
    public ResponseEntity<List<Shipment>> getUserShipments(@PathVariable UUID userId) {
        List<Shipment> shipments = shipmentService.getShipmentsByUser(userId);
        return ResponseEntity.ok(shipments);
    }
}
