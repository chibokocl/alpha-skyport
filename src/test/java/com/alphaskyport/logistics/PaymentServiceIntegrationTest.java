package com.alphaskyport.logistics;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.PaymentTransaction;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.PaymentTransactionRepository;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import com.alphaskyport.logistics.service.PaymentService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private FreightServiceRepository freightServiceRepository;

    @Test
    @Transactional
    public void testProcessPayment_Success() {
        // Setup
        Shipment shipment = createTestShipment(BigDecimal.valueOf(100));

        // 1. Partial Payment
        PaymentTransaction tx1 = paymentService.processPayment(
                shipment.getShipmentId(),
                BigDecimal.valueOf(50),
                "USD",
                "credit_card",
                "IDEM-1");

        assertNotNull(tx1.getTransactionId());
        assertEquals("completed", tx1.getTransactionStatus());

        Shipment updatedShipment = shipmentRepository.findById(shipment.getShipmentId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(updatedShipment.getAmountPaid()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(updatedShipment.getAmountDue()));
        assertEquals("partial", updatedShipment.getPaymentStatus());

        // 2. Full Payment
        PaymentTransaction tx2 = paymentService.processPayment(
                shipment.getShipmentId(),
                BigDecimal.valueOf(50),
                "USD",
                "credit_card",
                "IDEM-2");

        updatedShipment = shipmentRepository.findById(shipment.getShipmentId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(updatedShipment.getAmountPaid()));
        assertEquals(0, BigDecimal.valueOf(0).compareTo(updatedShipment.getAmountDue()));
        assertEquals("paid", updatedShipment.getPaymentStatus());
    }

    @Test
    @Transactional
    public void testProcessPayment_Idempotency() {
        // Setup
        Shipment shipment = createTestShipment(BigDecimal.valueOf(100));

        // 1. First Request
        PaymentTransaction tx1 = paymentService.processPayment(
                shipment.getShipmentId(),
                BigDecimal.valueOf(50),
                "USD",
                "credit_card",
                "IDEM-DUPLICATE");

        // 2. Duplicate Request
        PaymentTransaction tx2 = paymentService.processPayment(
                shipment.getShipmentId(),
                BigDecimal.valueOf(50),
                "USD",
                "credit_card",
                "IDEM-DUPLICATE");

        // Verify same transaction returned
        assertEquals(tx1.getTransactionId(), tx2.getTransactionId());

        // Verify Shipment charged only once
        Shipment updatedShipment = shipmentRepository.findById(shipment.getShipmentId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(updatedShipment.getAmountPaid()));
    }

    @Test
    @Transactional
    public void testProcessPayment_Overpayment() {
        // Setup
        Shipment shipment = createTestShipment(BigDecimal.valueOf(100));

        // Execute & Verify
        assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(
                    shipment.getShipmentId(),
                    BigDecimal.valueOf(150),
                    "USD",
                    "credit_card",
                    "IDEM-OVER");
        });
    }

    private Shipment createTestShipment(BigDecimal totalCost) {
        User user = new User();
        user.setEmail("payment_test_" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hash");
        user.setUserType("private");
        user = userRepository.save(user);

        Country country = new Country();
        country.setCountryCode("FR");
        country.setCountryName("France");
        country = countryRepository.save(country);

        FreightService service = new FreightService();
        service.setServiceName("Standard");
        service.setServiceType("land");
        service = freightServiceRepository.save(service);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("PAY-" + UUID.randomUUID());
        shipment.setUser(user);
        shipment.setService(service);
        shipment.setOriginCountry(country);
        shipment.setDestinationCountry(country);
        shipment.setTotalCost(totalCost);
        shipment.setAmountDue(totalCost);
        shipment.setAmountPaid(BigDecimal.ZERO);
        shipment.setPaymentStatus("unpaid");

        return shipmentRepository.save(shipment);
    }
}
