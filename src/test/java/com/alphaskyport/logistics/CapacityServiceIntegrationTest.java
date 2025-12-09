package com.alphaskyport.logistics;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.CapacityBooking;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.CapacityBookingRepository;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import com.alphaskyport.logistics.service.CapacityService;
import com.alphaskyport.masterdata.model.Country;
import com.alphaskyport.masterdata.model.FreightService;
import com.alphaskyport.masterdata.repository.CountryRepository;
import com.alphaskyport.masterdata.repository.FreightServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CapacityServiceIntegrationTest {

    @Autowired
    private CapacityService capacityService;

    @Autowired
    private CapacityBookingRepository capacityBookingRepository;

    @Autowired
    private FreightServiceRepository freightServiceRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Test
    @Transactional
    public void testReserveCapacity_Success() {
        // Setup Dependencies
        User user = new User();
        user.setEmail("capacity_test@example.com");
        user.setPasswordHash("hash");
        user.setUserType("business");
        user = userRepository.save(user);

        Country country = new Country();
        country.setCountryCode("CA");
        country.setCountryName("Canada");
        country = countryRepository.save(country);

        FreightService service = new FreightService();
        service.setServiceName("Limited Service");
        service.setServiceType("air");
        service.setMaxDailyCapacityKg(BigDecimal.valueOf(100));
        service.setMaxDailyCapacityM3(BigDecimal.valueOf(10));
        service = freightServiceRepository.save(service);

        // Create Shipment
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TEST-TRACK-001");
        shipment.setUser(user);
        shipment.setService(service);
        shipment.setOriginCountry(country);
        shipment.setDestinationCountry(country);
        shipment.setCargoWeight(BigDecimal.valueOf(60));
        shipment.setCargoVolume(BigDecimal.valueOf(5));
        shipment.setEstimatedPickupDate(LocalDate.now().plusDays(2));
        shipment = shipmentRepository.save(shipment);

        // Execute
        capacityService.reserveCapacity(shipment);

        // Verify
        CapacityBooking booking = capacityBookingRepository
                .findByService_ServiceIdAndBookingDate(service.getServiceId(), shipment.getEstimatedPickupDate())
                .orElseThrow();
        assertEquals(0, BigDecimal.valueOf(60).compareTo(booking.getReservedWeightKg()));
        assertEquals(0, BigDecimal.valueOf(5).compareTo(booking.getReservedVolumeM3()));
    }

    @Test
    @Transactional
    public void testReserveCapacity_Exceeded() {
        // Setup Dependencies
        User user = new User();
        user.setEmail("capacity_fail@example.com");
        user.setPasswordHash("hash");
        user.setUserType("business");
        user = userRepository.save(user);

        Country country = new Country();
        country.setCountryCode("MX");
        country.setCountryName("Mexico");
        country = countryRepository.save(country);

        FreightService service = new FreightService();
        service.setServiceName("Tiny Service");
        service.setServiceType("land");
        service.setMaxDailyCapacityKg(BigDecimal.valueOf(50)); // Limit 50
        service = freightServiceRepository.save(service);

        // Create Shipment (Weight 60 > 50)
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TEST-TRACK-002");
        shipment.setUser(user);
        shipment.setService(service);
        shipment.setOriginCountry(country);
        shipment.setDestinationCountry(country);
        shipment.setCargoWeight(BigDecimal.valueOf(60));
        shipment.setCargoVolume(BigDecimal.valueOf(1));
        shipment.setEstimatedPickupDate(LocalDate.now().plusDays(3));
        shipment = shipmentRepository.save(shipment);

        // Execute & Verify Exception
        Shipment finalShipment = shipment;
        assertThrows(RuntimeException.class, () -> capacityService.reserveCapacity(finalShipment));
    }
}
