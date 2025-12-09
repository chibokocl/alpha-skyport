package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.CapacityBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapacityBookingRepository extends JpaRepository<CapacityBooking, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CapacityBooking> findByService_ServiceIdAndBookingDate(Integer serviceId, LocalDate bookingDate);
}
