package com.alphaskyport.admin.repository;

import com.alphaskyport.admin.model.AdminRole;
import com.alphaskyport.admin.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AdminUser> findByIsActiveTrue();

    List<AdminUser> findByRole(AdminRole role);

    List<AdminUser> findByRoleAndIsActiveTrue(AdminRole role);

    @Query("SELECT au FROM AdminUser au LEFT JOIN FETCH au.permissions WHERE au.email = :email")
    Optional<AdminUser> findByEmailWithPermissions(@Param("email") String email);

    @Query("SELECT au FROM AdminUser au LEFT JOIN FETCH au.permissions WHERE au.adminId = :id")
    Optional<AdminUser> findByIdWithPermissions(@Param("id") UUID id);

    @Query("SELECT au FROM AdminUser au WHERE au.isActive = true ORDER BY au.lastName, au.firstName")
    List<AdminUser> findAllActiveOrderedByName();

    @Query("SELECT COUNT(au) FROM AdminUser au WHERE au.role = :role AND au.isActive = true")
    long countActiveByRole(@Param("role") AdminRole role);
}
