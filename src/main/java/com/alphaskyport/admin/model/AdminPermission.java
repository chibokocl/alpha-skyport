package com.alphaskyport.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminUser adminUser;

    @Column(name = "permission_key", nullable = false)
    private String permissionKey;

    @Column(name = "granted_at")
    @Builder.Default
    private LocalDateTime grantedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private AdminUser grantedBy;
}
