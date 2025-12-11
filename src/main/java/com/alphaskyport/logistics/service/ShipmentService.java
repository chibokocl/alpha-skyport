package com.alphaskyport.logistics.service;

import com.alphaskyport.logistics.model.Quote;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.model.ShipmentTrackingEvent;
import com.alphaskyport.logistics.repository.QuoteRepository;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import com.alphaskyport.logistics.repository.ShipmentTrackingEventRepository;
import com.alphaskyport.logistics.repository.TrackingNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final QuoteRepository quoteRepository;
    private final TrackingNumberRepository trackingNumberRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final CapacityService capacityService;
    private final NotificationService notificationService;

    @Transactional
    public Shipment createShipmentFromQuote(UUID quoteId, java.time.LocalDate pickupDate) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        if (!"accepted".equals(quote.getQuoteStatus())) {
            throw new RuntimeException("Quote must be accepted before converting to shipment");
        }

        if (quote.getConvertedToShipmentId() != null) {
            throw new RuntimeException("Quote already converted to shipment");
        }

        // Generate atomic tracking number
        String trackingNumber = trackingNumberRepository.generateTrackingNumber();

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setUser(quote.getUser());
        shipment.setQuote(quote);
        shipment.setService(quote.getService());
        shipment.setOriginCountry(quote.getOriginCountry());
        shipment.setDestinationCountry(quote.getDestinationCountry());
        shipment.setCargoDescription(quote.getCargoDescription());
        shipment.setCargoWeight(quote.getCargoWeight());
        shipment.setCargoWeightUnit(quote.getCargoWeightUnit());
        shipment.setCargoVolume(quote.getCargoVolume());
        shipment.setCargoVolumeUnit(quote.getCargoVolumeUnit());
        shipment.setDeclaredValue(quote.getCargoValue());
        shipment.setCurrency(quote.getCargoCurrency());
        shipment.setTotalCost(quote.getQuotedPrice());
        shipment.setAmountDue(quote.getQuotedPrice());
        shipment.setShipmentStatus("pending");
        shipment.setEstimatedPickupDate(pickupDate);

        shipment = shipmentRepository.save(shipment);

        // Reserve capacity
        capacityService.reserveCapacity(shipment);

        // Update quote
        quote.setConvertedToShipmentId(shipment.getShipmentId());
        quote.setConvertedAt(LocalDateTime.now());
        quote.setQuoteStatus("converted");
        quoteRepository.save(quote);

        // Create initial tracking event
        createTrackingEvent(shipment, "pending", "Shipment created from quote", "system");

        // Notify user
        notificationService.enqueueNotification(
                shipment.getUser(),
                shipment,
                "Shipment Created",
                "Your shipment " + shipment.getTrackingNumber() + " has been created.",
                "SHIPMENT_CREATED");

        return shipment;
    }

    @Transactional
    public void updateStatus(UUID shipmentId, String newStatus, String description, String source) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        String oldStatus = shipment.getShipmentStatus();
        shipment.setShipmentStatus(newStatus);
        shipment.setPreviousStatus(oldStatus);
        shipment.setStatusChangedAt(LocalDateTime.now());

        shipmentRepository.save(shipment);

        createTrackingEvent(shipment, newStatus, description, source);

        // Notify user
        notificationService.enqueueNotification(
                shipment.getUser(),
                shipment,
                "Shipment Update: " + newStatus,
                "Your shipment " + shipment.getTrackingNumber() + " is now " + newStatus + ".",
                "SHIPMENT_STATUS_UPDATE");
    }

    private void createTrackingEvent(Shipment shipment, String status, String description, String source) {
        ShipmentTrackingEvent event = new ShipmentTrackingEvent();
        event.setShipment(shipment);
        event.setEventStatus(status);
        event.setEventDescription(description);
        event.setEventTimestamp(LocalDateTime.now());
        event.setEventSource(source);

        trackingEventRepository.save(event);
    }

    // Query methods
    public java.util.Optional<Shipment> getShipmentById(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId);
    }

    public java.util.Optional<Shipment> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    public java.util.List<Shipment> getShipmentsByUser(UUID userId) {
        return shipmentRepository.findByUser_UserId(userId);
    }
}
