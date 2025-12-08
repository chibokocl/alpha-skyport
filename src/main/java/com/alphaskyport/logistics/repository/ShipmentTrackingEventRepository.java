package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {
    List<ShipmentTrackingEvent> findByShipment_ShipmentIdOrderByEventTimestampDesc(UUID shipmentId);
}
