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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final QuoteRepository quoteRepository;
    private final com.alphaskyport.iam.repository.UserRepository userRepository; // Direct access or via UserService
    private final TrackingNumberRepository trackingNumberRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final com.alphaskyport.masterdata.repository.CountryRepository countryRepository;
    private final com.alphaskyport.masterdata.repository.FreightServiceRepository freightServiceRepository;
    private final CapacityService capacityService;
    private final NotificationService notificationService;

    @Transactional
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
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
        @SuppressWarnings("null")
        java.util.Optional<Shipment> result = shipmentRepository.findById(shipmentId);
        return result;
    }

    public java.util.Optional<Shipment> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    public java.util.List<Shipment> getShipmentsByUser(UUID userId) {
        return shipmentRepository.findByUser_UserId(userId);
    } // Restore closing brace

    @Transactional
    public Shipment createPublicShipment(
            String senderName, String senderEmail,
            String receiverName, String receiverAddress,
            java.math.BigDecimal weightKg,
            java.math.BigDecimal lengthCm, java.math.BigDecimal widthCm, java.math.BigDecimal heightCm,
            java.time.LocalDate pickupDate,
            String originCountryCode, String destinationCountryCode,
            java.math.BigDecimal estimatedPrice, String currency) {

        // 1. Get or Create User
        com.alphaskyport.iam.model.User user = userRepository.findByEmail(senderEmail)
                .orElseGet(() -> {
                    com.alphaskyport.iam.model.User newUser = new com.alphaskyport.iam.model.User();
                    newUser.setEmail(senderEmail);
                    newUser.setFullName(senderName);
                    newUser.setPasswordHash("PENDING_ACTIVATION");
                    newUser.setUserRole(com.alphaskyport.iam.model.UserRole.USER);
                    newUser.setCreatedAt(java.time.LocalDateTime.now());
                    newUser.setIsActive(true);
                    return userRepository.save(newUser);
                });

        // 2. Generate Tracking
        String trackingNumber = trackingNumberRepository.generateTrackingNumber();

        // 3. Resolve Countries
        com.alphaskyport.masterdata.model.Country origin = countryRepository.findByCountryCode(originCountryCode)
                .orElseThrow(() -> new RuntimeException("Origin country not found: " + originCountryCode));

        com.alphaskyport.masterdata.model.Country destination = countryRepository
                .findByCountryCode(destinationCountryCode)
                .orElseThrow(() -> new RuntimeException("Destination country not found: " + destinationCountryCode));

        // 4. Resolve Service (Default to first/main for MVP)
        com.alphaskyport.masterdata.model.FreightService service = freightServiceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No freight services available"));

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setUser(user);
        shipment.setService(service);
        shipment.setOriginCountry(origin);
        shipment.setDestinationCountry(destination);
        shipment.setCargoDescription("General Cargo (Public Booking)");

        shipment.setCargoWeight(weightKg);
        shipment.setCargoWeightUnit("kg");

        // Volumetric Calc (cm3 -> m3)
        java.math.BigDecimal volume = lengthCm.multiply(widthCm).multiply(heightCm)
                .divide(java.math.BigDecimal.valueOf(1000000), 4, java.math.RoundingMode.HALF_UP);

        shipment.setCargoVolume(volume);
        shipment.setCargoVolumeUnit("m3");

        shipment.setTotalCost(estimatedPrice);
        shipment.setCurrency(currency);
        shipment.setShipmentStatus("pending");
        shipment.setEstimatedPickupDate(pickupDate);
        shipment.setOriginAddress("Origin: " + origin.getCountryName());
        shipment.setDestinationAddress(receiverAddress + ", " + destination.getCountryName());
        shipment.setSpecialInstructions("Receiver: " + receiverName);

        // Save
        shipment = shipmentRepository.save(shipment);

        // Reserve Capacity
        capacityService.reserveCapacity(shipment);

        // Track
        createTrackingEvent(shipment, "pending", "Public booking received", "web");

        // Notify
        try {
            notificationService.enqueueNotification(
                    user, shipment, "Booking Confirmed",
                    "Your shipment " + trackingNumber + " is booked.", "BOOKING_CONFIRMED");
        } catch (Exception e) {
            // Log but don't fail booking
            System.err.println("Failed to enqueue notification: " + e.getMessage());
        }

        return shipment;
    }
}
