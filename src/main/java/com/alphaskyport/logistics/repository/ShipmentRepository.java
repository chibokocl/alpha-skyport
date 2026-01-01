package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByUser_UserId(UUID userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Shipment s WHERE s.shipmentId = :id")
    Optional<Shipment> findByIdWithLock(UUID id);

    // Admin methods
    Page<Shipment> findByShipmentStatus(String status, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.shipmentStatus NOT IN ('delivered', 'cancelled', 'returned')")
    List<Shipment> findActiveShipments();

    List<Shipment> findByEstimatedPickupDate(LocalDate date);

    List<Shipment> findByEstimatedDeliveryDate(LocalDate date);

    @Query("SELECT s FROM Shipment s WHERE s.estimatedDeliveryDate < :today AND s.shipmentStatus NOT IN ('delivered', 'cancelled')")
    List<Shipment> findDelayedShipments(LocalDate today);
}
