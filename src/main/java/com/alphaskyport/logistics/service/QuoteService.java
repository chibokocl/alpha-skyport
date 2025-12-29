package com.alphaskyport.logistics.service;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import com.alphaskyport.logistics.model.Quote;
import com.alphaskyport.logistics.repository.QuoteRepository;
import com.alphaskyport.masterdata.model.Country;
import com.alphaskyport.masterdata.model.FreightService;
import com.alphaskyport.masterdata.repository.CountryRepository;
import com.alphaskyport.masterdata.repository.FreightServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final FreightServiceRepository freightServiceRepository;

    @Transactional
    @SuppressWarnings("null")
    public Quote createQuote(UUID userId, Quote quoteRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Country origin = countryRepository.findById(quoteRequest.getOriginCountry().getCountryId())
                .orElseThrow(() -> new RuntimeException("Origin country not found"));

        Country destination = countryRepository.findById(quoteRequest.getDestinationCountry().getCountryId())
                .orElseThrow(() -> new RuntimeException("Destination country not found"));

        FreightService service = null;
        if (quoteRequest.getService() != null) {
            service = freightServiceRepository.findById(quoteRequest.getService().getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found"));
        }

        quoteRequest.setUser(user);
        quoteRequest.setOriginCountry(origin);
        quoteRequest.setDestinationCountry(destination);
        quoteRequest.setService(service);
        quoteRequest.setQuoteStatus("calculating");

        // Simulate calculation logic (placeholder)
        calculateQuote(quoteRequest);

        return quoteRepository.save(quoteRequest);
    }

    private void calculateQuote(Quote quote) {
        // Placeholder logic for calculation
        // In a real scenario, this would use rates, weights, etc.
        BigDecimal baseRate = BigDecimal.valueOf(100);
        if (quote.getService() != null && quote.getService().getBaseRate() != null) {
            baseRate = quote.getService().getBaseRate();
        }

        BigDecimal weight = quote.getCargoWeight() != null ? quote.getCargoWeight() : BigDecimal.ONE;
        BigDecimal price = baseRate.multiply(weight);

        quote.setQuotedPrice(price);
        quote.setQuotedAt(LocalDateTime.now());
        quote.setValidUntil(LocalDateTime.now().plusDays(7));
        quote.setQuoteStatus("quoted");
    }

    @Transactional
    @SuppressWarnings("null")
    public Quote acceptQuote(UUID quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        if (!"quoted".equals(quote.getQuoteStatus())) {
            throw new RuntimeException("Quote cannot be accepted in status: " + quote.getQuoteStatus());
        }

        if (quote.getValidUntil().isBefore(LocalDateTime.now())) {
            quote.setQuoteStatus("expired");
            return quoteRepository.save(quote);
        }

        quote.setQuoteStatus("accepted");
        return quoteRepository.save(quote);
    }
}
