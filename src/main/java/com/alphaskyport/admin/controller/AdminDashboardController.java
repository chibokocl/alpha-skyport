package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.dto.DashboardDTOs.DashboardStats;
import com.alphaskyport.admin.dto.DashboardDTOs.DashboardAlerts;
import com.alphaskyport.admin.dto.DashboardDTOs.ShipmentsByStatus;
import com.alphaskyport.admin.dto.DashboardDTOs.AnalyticsSummary;
import com.alphaskyport.admin.dto.IssueDTOs;

import com.alphaskyport.admin.service.DashboardService;
import com.alphaskyport.admin.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Dashboard and analytics endpoints")
@Slf4j
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard stats", description = "Get main dashboard statistics")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get alerts", description = "Get dashboard alerts and notifications")
    public ResponseEntity<DashboardAlerts> getAlerts() {
        return ResponseEntity.ok(dashboardService.getAlerts());
    }

    @GetMapping("/shipments/by-status")
    @Operation(summary = "Get shipments by status", description = "Get shipment count breakdown by status")
    public ResponseEntity<ShipmentsByStatus> getShipmentsByStatus() {
        return ResponseEntity.ok(dashboardService.getShipmentsByStatus());
    }

    @GetMapping("/issues/stats")
    @Operation(summary = "Get issue stats", description = "Get issue statistics")
    public ResponseEntity<IssueDTOs.IssueStatsResponse> getIssueStats() {
        return ResponseEntity.ok(dashboardService.getIssueStats());
    }

    @GetMapping("/analytics")
    @RequiresPermission("reports:view")
    @Operation(summary = "Get analytics summary", description = "Get comprehensive analytics data")
    public ResponseEntity<AnalyticsSummary> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Full analytics implementation requested. Returning empty summary for now.");
        return ResponseEntity.ok(AnalyticsSummary.builder().build());
    }
}
