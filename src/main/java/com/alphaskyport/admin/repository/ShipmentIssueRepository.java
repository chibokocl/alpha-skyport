package com.alphaskyport.admin.repository;

import com.alphaskyport.admin.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentIssueRepository extends JpaRepository<ShipmentIssue, UUID>, JpaSpecificationExecutor<ShipmentIssue> {

    List<ShipmentIssue> findByShipment_ShipmentId(UUID shipmentId);

    Page<ShipmentIssue> findByStatus(IssueStatus status, Pageable pageable);

    Page<ShipmentIssue> findByStatusNot(IssueStatus status, Pageable pageable);

    List<ShipmentIssue> findByAssignedTo_AdminId(UUID adminId);

    Page<ShipmentIssue> findByAssignedTo_AdminIdAndStatusNot(UUID adminId, IssueStatus status, Pageable pageable);

    @Query("SELECT si FROM ShipmentIssue si WHERE si.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY " +
           "CASE si.severity WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, " +
           "si.createdAt ASC")
    Page<ShipmentIssue> findOpenIssuesBySeverity(Pageable pageable);

    @Query("SELECT si FROM ShipmentIssue si WHERE si.severity = :severity AND si.status NOT IN ('RESOLVED', 'CLOSED')")
    List<ShipmentIssue> findOpenIssuesBySeverity(@Param("severity") IssueSeverity severity);

    @Query("SELECT COUNT(si) FROM ShipmentIssue si WHERE si.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenIssues();

    @Query("SELECT COUNT(si) FROM ShipmentIssue si WHERE si.status NOT IN ('RESOLVED', 'CLOSED') AND si.severity = :severity")
    long countOpenIssuesBySeverity(@Param("severity") IssueSeverity severity);

    @Query("SELECT si.issueType, COUNT(si) FROM ShipmentIssue si WHERE si.createdAt BETWEEN :start AND :end GROUP BY si.issueType")
    List<Object[]> countByIssueTypeBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT si FROM ShipmentIssue si WHERE si.assignedTo IS NULL AND si.status = 'OPEN'")
    List<ShipmentIssue> findUnassignedOpenIssues();
}
