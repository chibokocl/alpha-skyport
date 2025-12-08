package com.alphaskyport.logistics;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.Quote;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.service.QuoteService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CoreLogisticsIntegrationTest {

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private FreightServiceRepository freightServiceRepository;

    @Test
    @Transactional
    public void testQuoteToShipmentFlow() {
        // 1. Setup Data
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setUserType("business");
        user = userRepository.save(user);

        Country origin = new Country();
        origin.setCountryCode("US");
        origin.setCountryName("United States");
        origin = countryRepository.save(origin);

        Country destination = new Country();
        destination.setCountryCode("GB");
        destination.setCountryName("United Kingdom");
        destination = countryRepository.save(destination);

        FreightService service = new FreightService();
        service.setServiceName("Air Express");
        service.setServiceType("air");
        service.setBaseRate(BigDecimal.valueOf(50));
        service = freightServiceRepository.save(service);

        // 2. Create Quote
        Quote quoteRequest = new Quote();
        quoteRequest.setOriginCountry(origin);
        quoteRequest.setDestinationCountry(destination);
        quoteRequest.setService(service);
        quoteRequest.setCargoWeight(BigDecimal.valueOf(10));

        Quote createdQuote = quoteService.createQuote(user.getUserId(), quoteRequest);

        assertNotNull(createdQuote.getQuoteId());
        assertEquals("quoted", createdQuote.getQuoteStatus());
        assertNotNull(createdQuote.getQuotedPrice());

        // 3. Accept Quote
        Quote acceptedQuote = quoteService.acceptQuote(createdQuote.getQuoteId());
        assertEquals("accepted", acceptedQuote.getQuoteStatus());

        // 4. Convert to Shipment
        Shipment shipment = shipmentService.createShipmentFromQuote(acceptedQuote.getQuoteId());

        assertNotNull(shipment.getShipmentId());
        assertNotNull(shipment.getTrackingNumber());
        assertTrue(shipment.getTrackingNumber().startsWith("ASL-"));
        assertEquals("pending", shipment.getShipmentStatus());
        assertEquals(acceptedQuote.getQuoteId(), shipment.getQuote().getQuoteId());

        // Verify Quote is updated
        assertEquals("converted", shipment.getQuote().getQuoteStatus());
        assertEquals(shipment.getShipmentId(), shipment.getQuote().getConvertedToShipmentId());
    }
}
