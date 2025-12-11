package com.alphaskyport.logistics.controller;

import com.alphaskyport.logistics.model.Quote;
import com.alphaskyport.logistics.repository.QuoteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Tag(name = "Quotes", description = "Quote management APIs")
public class QuoteController {

    private final QuoteRepository quoteRepository;

    @GetMapping("/{quoteId}")
    @Operation(summary = "Get quote by ID", description = "Retrieves quote details by ID")
    public ResponseEntity<Quote> getQuote(@PathVariable UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user quotes", description = "Retrieves all quotes for a user")
    public ResponseEntity<List<Quote>> getUserQuotes(@PathVariable UUID userId) {
        List<Quote> quotes = quoteRepository.findByUser_UserId(userId);
        return ResponseEntity.ok(quotes);
    }

    @PutMapping("/{quoteId}/accept")
    @Operation(summary = "Accept quote", description = "Marks a quote as accepted")
    public ResponseEntity<Quote> acceptQuote(@PathVariable UUID quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        quote.setQuoteStatus("accepted");
        quoteRepository.save(quote);

        return ResponseEntity.ok(quote);
    }
}
