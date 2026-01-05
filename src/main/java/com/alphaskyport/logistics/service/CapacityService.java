package com.alphaskyport.logistics.service;

import com.alphaskyport.logistics.model.CapacityBooking;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.model.ShipmentReservation;
import com.alphaskyport.logistics.repository.CapacityBookingRepository;
import com.alphaskyport.logistics.repository.ShipmentReservationRepository;
import com.alphaskyport.masterdata.model.FreightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CapacityService {

    private final CapacityBookingRepository capacityBookingRepository;
    private final ShipmentReservationRepository shipmentReservationRepository;

    /**
     * Checks if there is sufficient capacity for a shipment on a given date.
     */
    @Transactional(readOnly = true)
    public boolean checkAvailability(LocalDate date, Integer serviceId, BigDecimal weightKg, BigDecimal volumeM3) {
        if (serviceId == null || date == null) {
            return true; // Assume available if generic check
        }

        // Check if a specific constraint exists
        return capacityBookingRepository
                .findByService_ServiceIdAndBookingDate(serviceId, date)
                .map(booking -> {
                    BigDecimal newTotalWeight = booking.getReservedWeightKg()
                            .add(weightKg != null ? weightKg : BigDecimal.ZERO);
                    BigDecimal newTotalVolume = booking.getReservedVolumeM3()
                            .add(volumeM3 != null ? volumeM3 : BigDecimal.ZERO);

                    boolean weightOk = booking.getMaxWeightKg() == null
                            || newTotalWeight.compareTo(booking.getMaxWeightKg()) <= 0;
                    boolean volumeOk = booking.getMaxVolumeM3() == null
                            || newTotalVolume.compareTo(booking.getMaxVolumeM3()) <= 0;

                    return weightOk && volumeOk;
                })
                .orElse(true);
    }

    @Transactional
    public void reserveCapacity(Shipment shipment) {
        // Determine booking date (e.g., estimated pickup date)
        LocalDate bookingDate = shipment.getEstimatedPickupDate();
        if (bookingDate == null) {
            throw new IllegalArgumentException("Shipment must have an estimated pickup date for capacity reservation");
        }

        FreightService service = shipment.getService();

        // 1. Get or Create CapacityBooking (with Lock)
        CapacityBooking booking = capacityBookingRepository
                .findByService_ServiceIdAndBookingDate(service.getServiceId(), bookingDate)
                .orElseGet(() -> {
                    // Create new booking record if not exists
                    CapacityBooking newBooking = new CapacityBooking();
                    newBooking.setService(service);
                    newBooking.setBookingDate(bookingDate);
                    newBooking.setMaxWeightKg(service.getMaxDailyCapacityKg());
                    newBooking.setMaxVolumeM3(service.getMaxDailyCapacityM3());
                    try {
                        return capacityBookingRepository.save(newBooking);
                    } catch (Exception e) {
                        // Handle race condition where another transaction inserted it just now
                        return capacityBookingRepository
                                .findByService_ServiceIdAndBookingDate(service.getServiceId(), bookingDate)
                                .orElseThrow(() -> new RuntimeException(
                                        "Failed to retrieve capacity booking after race condition"));
                    }
                });

        // Ensure we have the lock (save() might not lock if it was just inserted, but
        // subsequent find with lock will)
        // If we just inserted it, we are in the same transaction, so we hold the lock
        // on the new row effectively?
        // Actually, to be safe and consistent with PESSIMISTIC_WRITE, we should
        // re-fetch if we want to be 100% sure we have the DB lock,
        // but since we just inserted it in this transaction, no one else can see it yet
        // (Read Committed), so we are safe.
        // However, if it existed, we need the lock. The findBy... call has @Lock.
        // If we fell into orElseGet, we inserted it.
        // If we found it, we have the lock.

        // 2. Check Capacity
        BigDecimal shipmentWeight = shipment.getCargoWeight() != null ? shipment.getCargoWeight() : BigDecimal.ZERO;
        BigDecimal shipmentVolume = shipment.getCargoVolume() != null ? shipment.getCargoVolume() : BigDecimal.ZERO;

        BigDecimal newTotalWeight = booking.getReservedWeightKg().add(shipmentWeight);
        BigDecimal newTotalVolume = booking.getReservedVolumeM3().add(shipmentVolume);

        if (booking.getMaxWeightKg() != null && newTotalWeight.compareTo(booking.getMaxWeightKg()) > 0) {
            throw new RuntimeException("Capacity exceeded: Weight limit reached for service on " + bookingDate);
        }
        if (booking.getMaxVolumeM3() != null && newTotalVolume.compareTo(booking.getMaxVolumeM3()) > 0) {
            throw new RuntimeException("Capacity exceeded: Volume limit reached for service on " + bookingDate);
        }

        // 3. Update Capacity
        booking.setReservedWeightKg(newTotalWeight);
        booking.setReservedVolumeM3(newTotalVolume);
        capacityBookingRepository.save(booking);

        // 4. Create Reservation
        ShipmentReservation reservation = new ShipmentReservation();
        reservation.setShipment(shipment);
        reservation.setBooking(booking);
        reservation.setReservedWeightKg(shipmentWeight);
        reservation.setReservedVolumeM3(shipmentVolume);
        shipmentReservationRepository.save(reservation);
    }
}
