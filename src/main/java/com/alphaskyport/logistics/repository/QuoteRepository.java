package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    List<Quote> findByUser_UserId(UUID userId);

    Optional<Quote> findByIdempotencyKey(String idempotencyKey);
}
