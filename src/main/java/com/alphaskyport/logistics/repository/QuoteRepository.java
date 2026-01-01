package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    List<Quote> findByUser_UserId(UUID userId);

    Optional<Quote> findByIdempotencyKey(String idempotencyKey);

    // Admin methods
    Page<Quote> findByQuoteStatus(String status, Pageable pageable);

    List<Quote> findByQuoteStatus(String status);

    @Query("SELECT q FROM Quote q WHERE q.quoteStatus = 'quoted' AND q.validUntil BETWEEN :start AND :end")
    List<Quote> findExpiringQuotes(LocalDateTime start, LocalDateTime end);
}
