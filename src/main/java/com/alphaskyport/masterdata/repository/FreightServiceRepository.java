package com.alphaskyport.masterdata.repository;

import com.alphaskyport.masterdata.model.FreightService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreightServiceRepository extends JpaRepository<FreightService, Integer> {

    @Cacheable("freightServices")
    List<FreightService> findByServiceType(String serviceType);

    @Cacheable("activeFreightServices")
    List<FreightService> findByIsActiveTrue();
}
