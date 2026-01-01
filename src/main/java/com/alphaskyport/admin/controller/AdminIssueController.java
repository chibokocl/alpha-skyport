package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.dto.IssueDTOs.*;
import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.model.IssueStatus;
import com.alphaskyport.admin.service.ShipmentIssueService;
import com.alphaskyport.admin.security.CurrentAdmin;
import com.alphaskyport.admin.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/issues")
@RequiredArgsConstructor
@Tag(name = "Admin Issues", description = "Shipment issue management endpoints")
public class AdminIssueController {

    private final ShipmentIssueService issueService;

    @GetMapping
    @RequiresPermission("shipments:read")
    @Operation(summary = "List issues", description = "Get paginated list of issues")
    public ResponseEntity<IssueListResponse> getIssues(
            @RequestParam(required = false) IssueStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(issueService.getIssues(status, pageable));
    }

    @GetMapping("/priority")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get issues by priority", description = "Get open issues ordered by severity")
    public ResponseEntity<IssueListResponse> getIssuesByPriority(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(issueService.getOpenIssuesBySeverity(pageable));
    }

    @GetMapping("/my-issues")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get my issues", description = "Get issues assigned to current admin")
    public ResponseEntity<IssueListResponse> getMyIssues(
            @CurrentAdmin AdminUser admin,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(issueService.getMyIssues(admin.getAdminId(), pageable));
    }

    @GetMapping("/{issueId}")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get issue", description = "Get issue details by ID")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable UUID issueId) {
        return ResponseEntity.ok(issueService.getIssueById(issueId));
    }

    @GetMapping("/shipment/{shipmentId}")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get shipment issues", description = "Get all issues for a shipment")
    public ResponseEntity<IssueListResponse> getShipmentIssues(@PathVariable UUID shipmentId) {
        return ResponseEntity.ok(issueService.getIssuesByShipment(shipmentId));
    }

    @GetMapping("/stats")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get issue stats", description = "Get issue statistics")
    public ResponseEntity<IssueStatsResponse> getIssueStats() {
        return ResponseEntity.ok(issueService.getIssueStats());
    }

    @PostMapping
    @RequiresPermission("shipments:write")
    @Operation(summary = "Create issue", description = "Create a new shipment issue")
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(issueService.createIssue(request, admin));
    }

    @PutMapping("/{issueId}")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Update issue", description = "Update issue details")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody UpdateIssueRequest request,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(issueService.updateIssue(issueId, request, admin));
    }

    @PostMapping("/{issueId}/assign")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Assign issue", description = "Assign issue to an admin")
    public ResponseEntity<IssueResponse> assignIssue(
            @PathVariable UUID issueId,
            @RequestParam UUID assignTo,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(issueService.assignIssue(issueId, assignTo, admin));
    }

    @PostMapping("/{issueId}/resolve")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Resolve issue", description = "Mark issue as resolved")
    public ResponseEntity<IssueResponse> resolveIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody ResolveIssueRequest request,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(issueService.resolveIssue(issueId, request, admin));
    }

    @PostMapping("/{issueId}/close")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Close issue", description = "Close a resolved issue")
    public ResponseEntity<IssueResponse> closeIssue(
            @PathVariable UUID issueId,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(issueService.closeIssue(issueId, admin));
    }
}
