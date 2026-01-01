package com.alphaskyport.admin.model;

import com.alphaskyport.logistics.model.Shipment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipment_issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "issue_id")
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "issue_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private AdminUser reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AdminUser assignedTo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "responsible_party")
    @Enumerated(EnumType.STRING)
    private ResponsibleParty responsibleParty;

    @Column(name = "financial_impact", precision = 12, scale = 2)
    private BigDecimal financialImpact;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private AdminUser resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueAttachment> attachments = new ArrayList<>();

    public void resolve(AdminUser resolver, String notes) {
        this.status = IssueStatus.RESOLVED;
        this.resolvedBy = resolver;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    public void close() {
        this.status = IssueStatus.CLOSED;
    }
}
