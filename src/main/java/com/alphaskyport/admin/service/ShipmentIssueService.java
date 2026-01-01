package com.alphaskyport.admin.service;

import com.alphaskyport.admin.dto.IssueDTOs.IssueResponse;
import com.alphaskyport.admin.dto.IssueDTOs.IssueListResponse;
import com.alphaskyport.admin.dto.IssueDTOs.IssueSummary;
import com.alphaskyport.admin.dto.IssueDTOs.CreateIssueRequest;
import com.alphaskyport.admin.dto.IssueDTOs.UpdateIssueRequest;
import com.alphaskyport.admin.dto.IssueDTOs.ResolveIssueRequest;
import com.alphaskyport.admin.dto.IssueDTOs.AttachmentResponse;
import com.alphaskyport.admin.dto.IssueDTOs.AdminSummary;
import com.alphaskyport.admin.dto.IssueDTOs.IssueStatsResponse;
import com.alphaskyport.admin.model.*;
import com.alphaskyport.admin.repository.ShipmentIssueRepository;
import com.alphaskyport.admin.repository.AdminUserRepository;
import com.alphaskyport.admin.exception.AdminException;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ShipmentIssueService {

        private final ShipmentIssueRepository issueRepository;
        private final ShipmentRepository shipmentRepository;
        private final AdminUserRepository adminUserRepository;
        private final AdminActivityService activityService;

        @Transactional(readOnly = true)
        public IssueResponse getIssueById(UUID issueId) {
                ShipmentIssue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new AdminException.NotFoundException("Issue not found: " + issueId));
                return mapToResponse(issue);
        }

        @Transactional(readOnly = true)
        public IssueListResponse getIssues(IssueStatus status, Pageable pageable) {
                Page<ShipmentIssue> page;
                if (status != null) {
                        page = issueRepository.findByStatus(status, pageable);
                } else {
                        page = issueRepository.findByStatusNot(IssueStatus.CLOSED, pageable);
                }

                List<IssueSummary> summaries = page.getContent().stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return IssueListResponse.builder()
                                .issues(summaries)
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build();
        }

        @Transactional(readOnly = true)
        public IssueListResponse getIssuesByShipment(UUID shipmentId) {
                List<ShipmentIssue> issues = issueRepository.findByShipment_ShipmentId(shipmentId);
                List<IssueSummary> summaries = issues.stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return IssueListResponse.builder()
                                .issues(summaries)
                                .page(0)
                                .size(summaries.size())
                                .totalElements(summaries.size())
                                .totalPages(1)
                                .build();
        }

        @Transactional(readOnly = true)
        public IssueListResponse getMyIssues(UUID adminId, Pageable pageable) {
                Page<ShipmentIssue> page = issueRepository.findByAssignedTo_AdminIdAndStatusNot(
                                adminId, IssueStatus.CLOSED, pageable);

                List<IssueSummary> summaries = page.getContent().stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return IssueListResponse.builder()
                                .issues(summaries)
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build();
        }

        @Transactional(readOnly = true)
        public IssueListResponse getOpenIssuesBySeverity(Pageable pageable) {
                Page<ShipmentIssue> page = issueRepository.findOpenIssuesBySeverity(pageable);

                List<IssueSummary> summaries = page.getContent().stream()
                                .map(this::mapToSummary)
                                .collect(Collectors.toList());

                return IssueListResponse.builder()
                                .issues(summaries)
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build();
        }

        @Transactional
        public IssueResponse createIssue(CreateIssueRequest request, AdminUser reportedBy) {
                Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Shipment not found: " + request.getShipmentId()));

                AdminUser assignedTo = null;
                if (request.getAssignedTo() != null) {
                        assignedTo = adminUserRepository.findById(request.getAssignedTo())
                                        .orElseThrow(() -> new AdminException.NotFoundException(
                                                        "Admin not found: " + request.getAssignedTo()));
                }

                ShipmentIssue issue = ShipmentIssue.builder()
                                .shipment(shipment)
                                .issueType(request.getIssueType())
                                .severity(request.getSeverity())
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .reportedBy(reportedBy)
                                .assignedTo(assignedTo)
                                .responsibleParty(request.getResponsibleParty())
                                .financialImpact(request.getFinancialImpact())
                                .status(IssueStatus.OPEN)
                                .build();

                issue = issueRepository.save(issue);

                activityService.logActivity(reportedBy, "CREATE_ISSUE", "ShipmentIssue", issue.getIssueId().toString(),
                                "Created issue: " + issue.getTitle() + " for shipment: " + shipment.getTrackingNumber(),
                                null, null);

                log.info("Issue created: {} for shipment: {}", issue.getIssueId(), shipment.getTrackingNumber());
                return mapToResponse(issue);
        }

        @Transactional
        public IssueResponse updateIssue(UUID issueId, UpdateIssueRequest request, AdminUser updatedBy) {
                ShipmentIssue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new AdminException.NotFoundException("Issue not found: " + issueId));

                if (request.getIssueType() != null)
                        issue.setIssueType(request.getIssueType());
                if (request.getSeverity() != null)
                        issue.setSeverity(request.getSeverity());
                if (request.getTitle() != null)
                        issue.setTitle(request.getTitle());
                if (request.getDescription() != null)
                        issue.setDescription(request.getDescription());
                if (request.getStatus() != null)
                        issue.setStatus(request.getStatus());
                if (request.getResponsibleParty() != null)
                        issue.setResponsibleParty(request.getResponsibleParty());
                if (request.getFinancialImpact() != null)
                        issue.setFinancialImpact(request.getFinancialImpact());

                if (request.getAssignedTo() != null) {
                        AdminUser assignedTo = adminUserRepository.findById(request.getAssignedTo())
                                        .orElseThrow(() -> new AdminException.NotFoundException(
                                                        "Admin not found: " + request.getAssignedTo()));
                        issue.setAssignedTo(assignedTo);
                }

                issue = issueRepository.save(issue);

                activityService.logActivity(updatedBy, "UPDATE_ISSUE", "ShipmentIssue", issue.getIssueId().toString(),
                                "Updated issue: " + issue.getTitle(), null, null);

                return mapToResponse(issue);
        }

        @Transactional
        public IssueResponse assignIssue(UUID issueId, UUID assignToAdminId, AdminUser assignedBy) {
                ShipmentIssue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new AdminException.NotFoundException("Issue not found: " + issueId));

                AdminUser assignedTo = adminUserRepository.findById(assignToAdminId)
                                .orElseThrow(() -> new AdminException.NotFoundException(
                                                "Admin not found: " + assignToAdminId));

                issue.setAssignedTo(assignedTo);
                if (issue.getStatus() == IssueStatus.OPEN) {
                        issue.setStatus(IssueStatus.INVESTIGATING);
                }

                issue = issueRepository.save(issue);

                activityService.logActivity(assignedBy, "ASSIGN_ISSUE", "ShipmentIssue", issue.getIssueId().toString(),
                                "Assigned issue to: " + assignedTo.getFullName(), null, null);

                return mapToResponse(issue);
        }

        @Transactional
        public IssueResponse resolveIssue(UUID issueId, ResolveIssueRequest request, AdminUser resolvedBy) {
                ShipmentIssue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new AdminException.NotFoundException("Issue not found: " + issueId));

                if (issue.getStatus() == IssueStatus.CLOSED) {
                        throw new AdminException.InvalidStateException("Issue is already closed");
                }

                if (request.getResponsibleParty() != null) {
                        issue.setResponsibleParty(request.getResponsibleParty());
                }
                if (request.getFinancialImpact() != null) {
                        issue.setFinancialImpact(request.getFinancialImpact());
                }

                issue.resolve(resolvedBy, request.getResolutionNotes());
                issue = issueRepository.save(issue);

                activityService.logActivity(resolvedBy, "RESOLVE_ISSUE", "ShipmentIssue", issue.getIssueId().toString(),
                                "Resolved issue: " + issue.getTitle(), null, null);

                log.info("Issue resolved: {} by {}", issue.getIssueId(), resolvedBy.getEmail());
                return mapToResponse(issue);
        }

        @Transactional
        public IssueResponse closeIssue(UUID issueId, AdminUser closedBy) {
                ShipmentIssue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new AdminException.NotFoundException("Issue not found: " + issueId));

                if (issue.getStatus() != IssueStatus.RESOLVED) {
                        throw new AdminException.InvalidStateException("Only resolved issues can be closed");
                }

                issue.close();
                issue = issueRepository.save(issue);

                activityService.logActivity(closedBy, "CLOSE_ISSUE", "ShipmentIssue", issue.getIssueId().toString(),
                                "Closed issue: " + issue.getTitle(), null, null);

                return mapToResponse(issue);
        }

        @Transactional(readOnly = true)
        public IssueStatsResponse getIssueStats() {
                return IssueStatsResponse.builder()
                                .totalOpen(issueRepository.countOpenIssues())
                                .critical(issueRepository.countOpenIssuesBySeverity(IssueSeverity.CRITICAL))
                                .high(issueRepository.countOpenIssuesBySeverity(IssueSeverity.HIGH))
                                .medium(issueRepository.countOpenIssuesBySeverity(IssueSeverity.MEDIUM))
                                .low(issueRepository.countOpenIssuesBySeverity(IssueSeverity.LOW))
                                .unassigned((long) issueRepository.findUnassignedOpenIssues().size())
                                .build();
        }

        private IssueResponse mapToResponse(ShipmentIssue issue) {
                List<AttachmentResponse> attachments = issue.getAttachments().stream()
                                .map(a -> AttachmentResponse.builder()
                                                .attachmentId(a.getAttachmentId())
                                                .fileName(a.getFileName())
                                                .fileType(a.getFileType())
                                                .fileSize(a.getFileSize())
                                                .uploadedAt(a.getCreatedAt())
                                                .uploadedBy(a.getUploadedBy() != null ? a.getUploadedBy().getFullName()
                                                                : null)
                                                .build())
                                .collect(Collectors.toList());

                return IssueResponse.builder()
                                .issueId(issue.getIssueId())
                                .shipmentId(issue.getShipment().getShipmentId())
                                .trackingNumber(issue.getShipment().getTrackingNumber())
                                .issueType(issue.getIssueType())
                                .severity(issue.getSeverity())
                                .title(issue.getTitle())
                                .description(issue.getDescription())
                                .status(issue.getStatus())
                                .responsibleParty(issue.getResponsibleParty())
                                .financialImpact(issue.getFinancialImpact())
                                .resolutionNotes(issue.getResolutionNotes())
                                .resolvedAt(issue.getResolvedAt())
                                .createdAt(issue.getCreatedAt())
                                .updatedAt(issue.getUpdatedAt())
                                .reportedBy(mapAdminSummary(issue.getReportedBy()))
                                .assignedTo(mapAdminSummary(issue.getAssignedTo()))
                                .resolvedBy(mapAdminSummary(issue.getResolvedBy()))
                                .attachments(attachments)
                                .build();
        }

        private IssueSummary mapToSummary(ShipmentIssue issue) {
                return IssueSummary.builder()
                                .issueId(issue.getIssueId())
                                .trackingNumber(issue.getShipment().getTrackingNumber())
                                .issueType(issue.getIssueType())
                                .severity(issue.getSeverity())
                                .title(issue.getTitle())
                                .status(issue.getStatus())
                                .assignedToName(issue.getAssignedTo() != null ? issue.getAssignedTo().getFullName()
                                                : null)
                                .createdAt(issue.getCreatedAt())
                                .build();
        }

        private AdminSummary mapAdminSummary(AdminUser admin) {
                if (admin == null)
                        return null;
                return AdminSummary.builder()
                                .adminId(admin.getAdminId())
                                .fullName(admin.getFullName())
                                .email(admin.getEmail())
                                .build();
        }
}
