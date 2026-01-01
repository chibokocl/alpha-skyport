package com.alphaskyport.admin.service;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.exception.AdminException;
import com.alphaskyport.logistics.model.Quote;
import com.alphaskyport.logistics.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminQuoteService {

    private final QuoteRepository quoteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AdminActivityService activityService;

    @Transactional(readOnly = true)
    public Page<Quote> getAllQuotes(Pageable pageable) {
        return quoteRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Quote> getQuotesByStatus(String status, Pageable pageable) {
        return quoteRepository.findByQuoteStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Quote getQuoteById(UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));
    }

    @Transactional(readOnly = true)
    public List<Quote> getExpiringQuotes(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(hours);
        return quoteRepository.findExpiringQuotes(now, cutoff);
    }

    @Transactional(readOnly = true)
    public List<Quote> getPendingQuotes() {
        return quoteRepository.findByQuoteStatus("pending");
    }

    @Transactional
    public Quote adjustPrice(UUID quoteId, BigDecimal newAmount, String reason,
            String adjustmentType, AdminUser adjustedBy) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));

        if (!"pending".equals(quote.getQuoteStatus()) && !"quoted".equals(quote.getQuoteStatus())) {
            throw new AdminException.InvalidStateException("Can only adjust pending or quoted quotes");
        }

        BigDecimal originalAmount = quote.getQuotedPrice();

        // Record the adjustment
        String sql = """
                INSERT INTO quote_price_adjustments
                (quote_id, adjusted_by, original_amount, adjusted_amount, adjustment_reason, adjustment_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, quoteId, adjustedBy.getAdminId(), originalAmount, newAmount, reason, adjustmentType);

        // Update the quote
        quote.setQuotedPrice(newAmount);
        quote = quoteRepository.save(quote);

        activityService.logActivity(adjustedBy, "ADJUST_QUOTE_PRICE", "Quote", quoteId.toString(),
                "Adjusted price from " + originalAmount + " to " + newAmount + ": " + reason, null, null);

        log.info("Quote {} price adjusted from {} to {} by {}", quoteId, originalAmount, newAmount,
                adjustedBy.getEmail());
        return quote;
    }

    @Transactional
    public Quote approveQuote(UUID quoteId, AdminUser approvedBy) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));

        if (!"pending".equals(quote.getQuoteStatus())) {
            throw new AdminException.InvalidStateException("Can only approve pending quotes");
        }

        quote.setQuoteStatus("quoted");
        quote = quoteRepository.save(quote);

        activityService.logActivity(approvedBy, "APPROVE_QUOTE", "Quote", quoteId.toString(),
                "Approved quote", null, null);

        log.info("Quote approved. Notification would be sent to customer here.");

        return quote;
    }

    @Transactional
    public Quote rejectQuote(UUID quoteId, String reason, AdminUser rejectedBy) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));

        if (!"pending".equals(quote.getQuoteStatus())) {
            throw new AdminException.InvalidStateException("Can only reject pending quotes");
        }

        quote.setQuoteStatus("rejected");
        quote = quoteRepository.save(quote);

        activityService.logActivity(rejectedBy, "REJECT_QUOTE", "Quote", quoteId.toString(),
                "Rejected quote: " + reason, null, null);

        return quote;
    }

    @Transactional
    public Quote extendValidity(UUID quoteId, LocalDateTime newValidUntil, AdminUser extendedBy) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));

        if ("expired".equals(quote.getQuoteStatus()) || "rejected".equals(quote.getQuoteStatus())) {
            throw new AdminException.InvalidStateException("Cannot extend expired or rejected quotes");
        }

        LocalDateTime originalValidUntil = quote.getValidUntil();
        quote.setValidUntil(newValidUntil);

        // If quote was expired, reactivate it
        if ("expired".equals(quote.getQuoteStatus())) {
            quote.setQuoteStatus("quoted");
        }

        quote = quoteRepository.save(quote);

        activityService.logActivity(extendedBy, "EXTEND_QUOTE_VALIDITY", "Quote", quoteId.toString(),
                "Extended validity from " + originalValidUntil + " to " + newValidUntil, null, null);

        return quote;
    }

    @Transactional
    public void assignQuote(UUID quoteId, UUID assignToAdminId, AdminUser assignedBy) {
        quoteRepository.findById(quoteId)
                .orElseThrow(() -> new AdminException.NotFoundException("Quote not found: " + quoteId));

        String sql = """
                INSERT INTO quote_assignments (quote_id, assigned_to, assigned_by)
                VALUES (?, ?, ?)
                ON CONFLICT (quote_id, assigned_to, completed_at)
                WHERE completed_at IS NULL
                DO NOTHING
                """;
        jdbcTemplate.update(sql, quoteId, assignToAdminId, assignedBy.getAdminId());

        activityService.logActivity(assignedBy, "ASSIGN_QUOTE", "Quote", quoteId.toString(),
                "Assigned quote to admin: " + assignToAdminId, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuoteStats() {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE quote_status = 'pending') as pending,
                    COUNT(*) FILTER (WHERE quote_status = 'quoted') as quoted,
                    COUNT(*) FILTER (WHERE quote_status = 'accepted') as accepted,
                    COUNT(*) FILTER (WHERE quote_status = 'rejected') as rejected,
                    COUNT(*) FILTER (WHERE quote_status = 'expired') as expired,
                    COUNT(*) as total,
                    COALESCE(AVG(total_amount) FILTER (WHERE quote_status = 'accepted'), 0) as avg_accepted_value
                FROM quotes
                WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
                """;

        return jdbcTemplate.queryForMap(sql);
    }
}
