package com.alphaskyport.admin.service;

import com.alphaskyport.admin.dto.DashboardDTOs.DashboardStats;
import com.alphaskyport.admin.dto.DashboardDTOs.DashboardAlerts;
import com.alphaskyport.admin.dto.DashboardDTOs.ShipmentsByStatus;
import com.alphaskyport.admin.dto.DashboardDTOs.TrendIndicator;
import com.alphaskyport.admin.dto.DashboardDTOs.AlertItem;
import com.alphaskyport.admin.dto.IssueDTOs;
import com.alphaskyport.admin.model.IssueSeverity;
import com.alphaskyport.admin.repository.ShipmentIssueRepository;
import com.alphaskyport.admin.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ShipmentIssueRepository issueRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'main'", unless = "#result == null")
    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);

        // Get counts from database
        long activeShipments = countActiveShipments();
        long pendingQuotes = countPendingQuotes();
        long todaysPickups = countTodaysPickups();
        long todaysDeliveries = countTodaysDeliveries();
        long openIssues = issueRepository.countOpenIssues();
        long pendingVerifications = countPendingVerifications();

        // Revenue calculations
        BigDecimal todayRevenue = invoiceRepository.sumPaidAmountBetweenDates(today, today);
        BigDecimal weekRevenue = invoiceRepository.sumPaidAmountBetweenDates(weekStart, today);
        BigDecimal monthRevenue = invoiceRepository.sumPaidAmountBetweenDates(monthStart, today);
        BigDecimal lastMonthRevenue = invoiceRepository.sumPaidAmountBetweenDates(lastMonthStart,
                monthStart.minusDays(1));
        BigDecimal outstandingReceivables = invoiceRepository.sumOutstandingAmount();

        // Calculate trends
        TrendIndicator revenueTrend = calculateRevenueTrend(monthRevenue, lastMonthRevenue);

        return DashboardStats.builder()
                .activeShipments(activeShipments)
                .pendingQuotes(pendingQuotes)
                .todaysPickups(todaysPickups)
                .todaysDeliveries(todaysDeliveries)
                .openIssues(openIssues)
                .pendingVerifications(pendingVerifications)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weekRevenue(weekRevenue != null ? weekRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .outstandingReceivables(outstandingReceivables != null ? outstandingReceivables : BigDecimal.ZERO)
                .revenueTrend(revenueTrend)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardAlerts getAlerts() {
        List<AlertItem> criticalAlerts = new ArrayList<>();
        List<AlertItem> warnings = new ArrayList<>();
        List<AlertItem> info = new ArrayList<>();

        // Critical: Open critical/high severity issues
        long criticalIssues = issueRepository.countOpenIssuesBySeverity(IssueSeverity.CRITICAL);
        if (criticalIssues > 0) {
            criticalAlerts.add(AlertItem.builder()
                    .type("error")
                    .category("issue")
                    .title("Critical Issues")
                    .message(criticalIssues + " critical issue(s) require immediate attention")
                    .actionUrl("/admin/issues?severity=critical&status=open")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Warning: Overdue invoices
        List<?> overdueInvoices = invoiceRepository.findOverdueInvoices(LocalDate.now());
        if (!overdueInvoices.isEmpty()) {
            warnings.add(AlertItem.builder()
                    .type("warning")
                    .category("payment")
                    .title("Overdue Invoices")
                    .message(overdueInvoices.size() + " invoice(s) are overdue")
                    .actionUrl("/admin/invoices?status=overdue")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Warning: Expiring quotes
        long expiringQuotes = countExpiringQuotes();
        if (expiringQuotes > 0) {
            warnings.add(AlertItem.builder()
                    .type("warning")
                    .category("quote")
                    .title("Expiring Quotes")
                    .message(expiringQuotes + " quote(s) expiring within 48 hours")
                    .actionUrl("/admin/quotes?expiring=true")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Info: Pending verifications
        long pendingVerifications = countPendingVerifications();
        if (pendingVerifications > 0) {
            info.add(AlertItem.builder()
                    .type("info")
                    .category("verification")
                    .title("Pending Verifications")
                    .message(pendingVerifications + " business verification(s) awaiting review")
                    .actionUrl("/admin/customers/verifications")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return DashboardAlerts.builder()
                .criticalAlerts(criticalAlerts)
                .warnings(warnings)
                .info(info)
                .totalCount(criticalAlerts.size() + warnings.size() + info.size())
                .build();
    }

    @Transactional(readOnly = true)
    public ShipmentsByStatus getShipmentsByStatus() {
        // Using native query for efficiency
        String sql = """
                SELECT
                    SUM(CASE WHEN shipment_status = 'booked' THEN 1 ELSE 0 END) as booked,
                    SUM(CASE WHEN shipment_status = 'confirmed' THEN 1 ELSE 0 END) as confirmed,
                    SUM(CASE WHEN shipment_status = 'picked_up' THEN 1 ELSE 0 END) as picked_up,
                    SUM(CASE WHEN shipment_status = 'in_transit' THEN 1 ELSE 0 END) as in_transit,
                    SUM(CASE WHEN shipment_status = 'customs_clearance' THEN 1 ELSE 0 END) as customs,
                    SUM(CASE WHEN shipment_status = 'out_for_delivery' THEN 1 ELSE 0 END) as out_for_delivery,
                    SUM(CASE WHEN shipment_status = 'delivered' THEN 1 ELSE 0 END) as delivered,
                    SUM(CASE WHEN shipment_status = 'delayed' THEN 1 ELSE 0 END) as delayed,
                    SUM(CASE WHEN shipment_status = 'cancelled' THEN 1 ELSE 0 END) as cancelled
                FROM shipments
                WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> ShipmentsByStatus.builder()
                .booked(rs.getLong("booked"))
                .confirmed(rs.getLong("confirmed"))
                .pickedUp(rs.getLong("picked_up"))
                .inTransit(rs.getLong("in_transit"))
                .customsClearance(rs.getLong("customs"))
                .outForDelivery(rs.getLong("out_for_delivery"))
                .delivered(rs.getLong("delivered"))
                .delayed(rs.getLong("delayed"))
                .cancelled(rs.getLong("cancelled"))
                .build());
    }

    @Transactional(readOnly = true)
    public IssueDTOs.IssueStatsResponse getIssueStats() {
        return IssueDTOs.IssueStatsResponse.builder()
                .totalOpen(issueRepository.countOpenIssues())
                .critical(issueRepository.countOpenIssuesBySeverity(IssueSeverity.CRITICAL))
                .high(issueRepository.countOpenIssuesBySeverity(IssueSeverity.HIGH))
                .medium(issueRepository.countOpenIssuesBySeverity(IssueSeverity.MEDIUM))
                .low(issueRepository.countOpenIssuesBySeverity(IssueSeverity.LOW))
                .unassigned((long) issueRepository.findUnassignedOpenIssues().size())
                .build();
    }

    // Helper methods
    private long countActiveShipments() {
        String sql = "SELECT COUNT(*) FROM shipments WHERE shipment_status NOT IN ('delivered', 'cancelled')";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private long countPendingQuotes() {
        String sql = "SELECT COUNT(*) FROM quotes WHERE quote_status = 'pending'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private long countTodaysPickups() {
        String sql = "SELECT COUNT(*) FROM shipments WHERE DATE(scheduled_pickup_date) = CURRENT_DATE AND shipment_status IN ('confirmed', 'booked')";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private long countTodaysDeliveries() {
        String sql = "SELECT COUNT(*) FROM shipments WHERE DATE(expected_delivery_date) = CURRENT_DATE AND shipment_status NOT IN ('delivered', 'cancelled')";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private long countPendingVerifications() {
        String sql = "SELECT COUNT(*) FROM business_verification_requests WHERE status = 'pending'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private long countExpiringQuotes() {
        String sql = "SELECT COUNT(*) FROM quotes WHERE quote_status = 'quoted' AND valid_until BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '48 hours'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private TrendIndicator calculateRevenueTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return TrendIndicator.builder()
                    .percentageChange(0)
                    .direction("stable")
                    .comparisonPeriod("vs last month")
                    .build();
        }

        BigDecimal curr = current != null ? current : BigDecimal.ZERO;
        BigDecimal change = curr.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String direction = change.compareTo(BigDecimal.ZERO) > 0 ? "up"
                : change.compareTo(BigDecimal.ZERO) < 0 ? "down" : "stable";

        return TrendIndicator.builder()
                .percentageChange(change.doubleValue())
                .direction(direction)
                .comparisonPeriod("vs last month")
                .build();
    }
}
