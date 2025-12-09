package com.alphaskyport.logistics;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.Notification;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.NotificationRepository;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import com.alphaskyport.logistics.service.ShipmentService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceIntegrationTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private FreightServiceRepository freightServiceRepository;

    @Test
    @Transactional
    public void testUpdateStatus_TriggersNotification() {
        // Setup
        Shipment shipment = createTestShipment();

        // Execute
        shipmentService.updateStatus(
                shipment.getShipmentId(),
                "in_transit",
                "Shipment is on the way",
                "system");

        // Verify
        List<Notification> notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());

        Notification notification = notifications.get(0);
        assertEquals(shipment.getShipmentId(), notification.getShipment().getShipmentId());
        assertEquals("SHIPMENT_STATUS_UPDATE", notification.getNotificationType());
        assertEquals("pending", notification.getStatus());
        assertTrue(notification.getMessage().contains("in_transit"));
    }

    private Shipment createTestShipment() {
        User user = new User();
        user.setEmail("notif_test_" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hash");
        user.setUserType("private");
        user = userRepository.save(user);

        Country country = new Country();
        country.setCountryCode("DE");
        country.setCountryName("Germany");
        country = countryRepository.save(country);

        FreightService service = new FreightService();
        service.setServiceName("Express");
        service.setServiceType("air");
        service = freightServiceRepository.save(service);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("NOTIF-" + UUID.randomUUID());
        shipment.setUser(user);
        shipment.setService(service);
        shipment.setOriginCountry(country);
        shipment.setDestinationCountry(country);
        shipment.setTotalCost(BigDecimal.valueOf(100));
        shipment.setAmountDue(BigDecimal.valueOf(100));
        shipment.setAmountPaid(BigDecimal.ZERO);
        shipment.setPaymentStatus("unpaid");
        shipment.setShipmentStatus("pending");

        return shipmentRepository.save(shipment);
    }
}
