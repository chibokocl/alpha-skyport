package com.alphaskyport.admin.dto;

import com.alphaskyport.admin.model.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class IssueDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateIssueRequest {
        @NotNull(message = "Shipment ID is required")
        private UUID shipmentId;

        @NotNull(message = "Issue type is required")
        private IssueType issueType;

        @NotNull(message = "Severity is required")
        private IssueSeverity severity;

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be less than 255 characters")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        private UUID assignedTo;
        private ResponsibleParty responsibleParty;
        private BigDecimal financialImpact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateIssueRequest {
        private IssueType issueType;
        private IssueSeverity severity;
        private String title;
        private String description;
        private UUID assignedTo;
        private IssueStatus status;
        private ResponsibleParty responsibleParty;
        private BigDecimal financialImpact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveIssueRequest {
        @NotBlank(message = "Resolution notes are required")
        private String resolutionNotes;

        private BigDecimal financialImpact;
        private ResponsibleParty responsibleParty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueResponse {
        private UUID issueId;
        private UUID shipmentId;
        private String trackingNumber;
        private IssueType issueType;
        private IssueSeverity severity;
        private String title;
        private String description;
        private IssueStatus status;
        private ResponsibleParty responsibleParty;
        private BigDecimal financialImpact;
        private String resolutionNotes;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        private AdminSummary reportedBy;
        private AdminSummary assignedTo;
        private AdminSummary resolvedBy;
        
        private List<AttachmentResponse> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminSummary {
        private UUID adminId;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        private UUID attachmentId;
        private String fileName;
        private String fileType;
        private Integer fileSize;
        private LocalDateTime uploadedAt;
        private String uploadedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueListResponse {
        private List<IssueSummary> issues;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueSummary {
        private UUID issueId;
        private String trackingNumber;
        private IssueType issueType;
        private IssueSeverity severity;
        private String title;
        private IssueStatus status;
        private String assignedToName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueStatsResponse {
        private long totalOpen;
        private long critical;
        private long high;
        private long medium;
        private long low;
        private long unassigned;
    }
}
