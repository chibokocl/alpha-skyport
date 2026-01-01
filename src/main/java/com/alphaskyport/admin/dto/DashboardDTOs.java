package com.alphaskyport.admin.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private long activeShipments;
        private long pendingQuotes;
        private long todaysPickups;
        private long todaysDeliveries;
        private long openIssues;
        private long pendingVerifications;
        
        private BigDecimal todayRevenue;
        private BigDecimal weekRevenue;
        private BigDecimal monthRevenue;
        
        private BigDecimal outstandingReceivables;
        
        private TrendIndicator shipmentsTrend;
        private TrendIndicator revenueTrend;
        
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendIndicator {
        private double percentageChange;
        private String direction; // "up", "down", "stable"
        private String comparisonPeriod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertItem {
        private String type; // "warning", "error", "info"
        private String category; // "shipment", "quote", "payment", "issue", "system"
        private String title;
        private String message;
        private String entityId;
        private String actionUrl;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAlerts {
        private List<AlertItem> criticalAlerts;
        private List<AlertItem> warnings;
        private List<AlertItem> info;
        private int totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentsByStatus {
        private long booked;
        private long confirmed;
        private long pickedUp;
        private long inTransit;
        private long customsClearance;
        private long outForDelivery;
        private long delivered;
        private long delayed;
        private long cancelled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByService {
        private String serviceName;
        private String serviceType;
        private BigDecimal revenue;
        private long shipmentCount;
        private double percentageOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByPeriod {
        private String period;
        private BigDecimal revenue;
        private long invoiceCount;
        private BigDecimal avgOrderValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapacityUtilization {
        private Integer serviceId;
        private String serviceName;
        private String serviceType;
        private BigDecimal maxCapacityKg;
        private BigDecimal usedCapacityKg;
        private BigDecimal maxCapacityM3;
        private BigDecimal usedCapacityM3;
        private double utilizationPercentage;
        private String date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomer {
        private String customerId;
        private String customerName;
        private String customerType;
        private long shipmentCount;
        private BigDecimal totalRevenue;
        private LocalDateTime lastShipmentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteConversionStats {
        private long totalQuotes;
        private long convertedQuotes;
        private long expiredQuotes;
        private long rejectedQuotes;
        private double conversionRate;
        private BigDecimal avgQuoteValue;
        private double avgDaysToConvert;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePerformance {
        private String originCountry;
        private String destinationCountry;
        private long shipmentCount;
        private BigDecimal revenue;
        private double avgTransitDays;
        private double onTimeDeliveryRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsSummary {
        private ShipmentsByStatus shipmentsByStatus;
        private List<RevenueByService> revenueByService;
        private List<RevenueByPeriod> revenueByPeriod;
        private List<CapacityUtilization> capacityUtilization;
        private List<TopCustomer> topCustomers;
        private QuoteConversionStats quoteConversion;
        private List<RoutePerformance> topRoutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private String reportType;
        private String dateFrom;
        private String dateTo;
        private String groupBy;
        private Map<String, String> filters;
        private String format; // "json", "csv", "pdf"
    }
}
