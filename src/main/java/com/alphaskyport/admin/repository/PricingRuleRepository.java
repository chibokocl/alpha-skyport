package com.alphaskyport.admin.repository;

import com.alphaskyport.admin.model.PricingRule;
import com.alphaskyport.admin.model.PricingRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    List<PricingRule> findByIsActiveTrue();

    List<PricingRule> findByRuleType(PricingRuleType ruleType);

    List<PricingRule> findByRuleTypeAndIsActiveTrue(PricingRuleType ruleType);

    @Query("SELECT pr FROM PricingRule pr WHERE pr.isActive = true " +
           "AND (pr.validFrom IS NULL OR pr.validFrom <= :now) " +
           "AND (pr.validUntil IS NULL OR pr.validUntil >= :now) " +
           "ORDER BY pr.priority DESC")
    List<PricingRule> findActiveAndValidRules(@Param("now") LocalDateTime now);

    @Query("SELECT pr FROM PricingRule pr WHERE pr.isActive = true " +
           "AND pr.ruleType = :ruleType " +
           "AND (pr.validFrom IS NULL OR pr.validFrom <= :now) " +
           "AND (pr.validUntil IS NULL OR pr.validUntil >= :now) " +
           "ORDER BY pr.priority DESC")
    List<PricingRule> findActiveAndValidRulesByType(@Param("ruleType") PricingRuleType ruleType, @Param("now") LocalDateTime now);

    List<PricingRule> findByIsActiveTrueOrderByPriorityDesc();
}
