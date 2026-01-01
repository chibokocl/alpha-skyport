package com.alphaskyport.admin.service;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.exception.AdminException;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.model.ShipmentTrackingEvent;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import com.alphaskyport.logistics.repository.ShipmentTrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AdminActivityService activityService;

    // Valid status transitions
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "booked", Set.of("confirmed", "cancelled"),
            "confirmed", Set.of("picked_up", "cancelled"),
            "picked_up", Set.of("in_transit", "returned"),
            "in_transit", Set.of("customs_clearance", "out_for_delivery", "delayed"),
            "customs_clearance", Set.of("customs_hold", "out_for_delivery", "released"),
            "customs_hold", Set.of("released", "returned"),
            "released", Set.of("out_for_delivery"),
            "out_for_delivery", Set.of("delivered", "failed_delivery"),
            "failed_delivery", Set.of("out_for_delivery", "returned"),
            "delayed", Set.of("in_transit", "rescheduled"));

    @Transactional(readOnly = true)
    public Page<Shipment> getAllShipments(Pageable pageable) {
        return shipmentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Shipment> getShipmentsByStatus(String status, Pageable pageable) {
        return shipmentRepository.findByShipmentStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentById(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AdminException.NotFoundException("Shipment not found: " + shipmentId));
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new AdminException.NotFoundException("Shipment not found: " + trackingNumber));
    }

    @Transactional(readOnly = true)
    public List<Shipment> getActiveShipments() {
        return shipmentRepository.findActiveShipments();
    }

    @Transactional(readOnly = true)
    public List<Shipment> getTodaysPickups() {
        return shipmentRepository.findByEstimatedPickupDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Shipment> getTodaysDeliveries() {
        return shipmentRepository.findByEstimatedDeliveryDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Shipment> getDelayedShipments() {
        return shipmentRepository.findDelayedShipments(LocalDate.now());
    }

    @Transactional
    public Shipment updateStatus(UUID shipmentId, String newStatus, String location,
            String description, boolean notifyCustomer, AdminUser updatedBy) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AdminException.NotFoundException("Shipment not found: " + shipmentId));

        String currentStatus = shipment.getShipmentStatus();

        // Validate transition
        Set<String> validNextStatuses = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!validNextStatuses.contains(newStatus) && !updatedBy.getRole().hasPermission("*")) {
            throw new AdminException.InvalidStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        String oldStatus = shipment.getShipmentStatus();
        shipment.setShipmentStatus(newStatus);

        // Update delivery date if delivered
        if ("delivered".equals(newStatus)) {
            shipment.setActualDeliveryDate(LocalDate.now());
        }

        shipment = shipmentRepository.save(shipment);

        // Create tracking event
        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .shipment(shipment)
                .eventStatus(newStatus)
                .eventLocation(location)
                .eventDescription(description != null ? description : "Status updated to: " + newStatus)
                .eventTimestamp(LocalDateTime.now())
                .eventSource("admin")
                .build();
        trackingEventRepository.save(event);

        activityService.logActivity(updatedBy, "UPDATE_SHIPMENT_STATUS", "Shipment", shipmentId.toString(),
                "Updated status from " + oldStatus + " to " + newStatus, null, null);

        if (notifyCustomer) {
            log.info("Customer notification requested for shipment status update: {}", shipment.getTrackingNumber());
        }

        log.info("Shipment {} status updated from {} to {} by {}",
                shipment.getTrackingNumber(), oldStatus, newStatus, updatedBy.getEmail());
        return shipment;
    }

    @Transactional
    public void addTrackingEvent(UUID shipmentId, String status, String location,
            String description, LocalDateTime eventTime, AdminUser addedBy) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AdminException.NotFoundException("Shipment not found: " + shipmentId));

        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .shipment(shipment)
                .eventStatus(status)
                .eventLocation(location)
                .eventDescription(description)
                .eventTimestamp(eventTime != null ? eventTime : LocalDateTime.now())
                .eventSource("admin_manual")
                .build();
        trackingEventRepository.save(event);

        activityService.logActivity(addedBy, "ADD_TRACKING_EVENT", "Shipment", shipmentId.toString(),
                "Added tracking event: " + status + " at " + location, null, null);
    }

    @Transactional
    public Shipment updateExpectedDelivery(UUID shipmentId, LocalDate newDate, String reason, AdminUser updatedBy) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AdminException.NotFoundException("Shipment not found: " + shipmentId));

        LocalDate oldDate = shipment.getEstimatedDeliveryDate();
        shipment.setEstimatedDeliveryDate(newDate);
        shipment = shipmentRepository.save(shipment);

        // Add tracking event for the change
        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .shipment(shipment)
                .eventStatus("delivery_date_updated")
                .eventDescription(
                        "Expected delivery date changed from " + oldDate + " to " + newDate + ". Reason: " + reason)
                .eventTimestamp(LocalDateTime.now())
                .eventSource("admin")
                .build();
        trackingEventRepository.save(event);

        activityService.logActivity(updatedBy, "UPDATE_EXPECTED_DELIVERY", "Shipment", shipmentId.toString(),
                "Updated expected delivery from " + oldDate + " to " + newDate + ": " + reason, null, null);

        return shipment;
    }

    @Transactional
    public List<Shipment> bulkUpdateStatus(List<UUID> shipmentIds, String newStatus,
            String description, AdminUser updatedBy) {
        List<Shipment> updated = new ArrayList<>();

        for (UUID shipmentId : shipmentIds) {
            try {
                Shipment shipment = updateStatus(shipmentId, newStatus, null, description, false, updatedBy);
                updated.add(shipment);
            } catch (Exception e) {
                log.warn("Failed to update shipment {}: {}", shipmentId, e.getMessage());
            }
        }

        return updated;
    }

    @Transactional(readOnly = true)
    public List<ShipmentTrackingEvent> getTrackingHistory(UUID shipmentId) {
        return trackingEventRepository.findByShipment_ShipmentIdOrderByEventTimestampDesc(shipmentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getShipmentStats() {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE shipment_status NOT IN ('delivered', 'cancelled')) as active,
                    COUNT(*) FILTER (WHERE shipment_status = 'in_transit') as in_transit,
                    COUNT(*) FILTER (WHERE shipment_status = 'delayed') as delayed,
                    COUNT(*) FILTER (WHERE shipment_status = 'delivered' AND DATE(actual_delivery_date) = CURRENT_DATE) as delivered_today,
                    COUNT(*) FILTER (WHERE DATE(scheduled_pickup_date) = CURRENT_DATE AND shipment_status IN ('confirmed', 'booked')) as pickups_today,
                    COUNT(*) as total_last_30_days
                FROM shipments
                WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
                """;

        return jdbcTemplate.queryForMap(sql);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getShipmentsByStatusCount() {
        String sql = """
                SELECT shipment_status, COUNT(*) as count
                FROM shipments
                WHERE shipment_status NOT IN ('delivered', 'cancelled')
                GROUP BY shipment_status
                """;

        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getString("shipment_status"), rs.getLong("count"));
        });
        return result;
    }
}
