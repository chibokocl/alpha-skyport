package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.ShipmentReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShipmentReservationRepository extends JpaRepository<ShipmentReservation, UUID> {
}
