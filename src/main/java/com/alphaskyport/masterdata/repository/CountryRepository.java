package com.alphaskyport.masterdata.repository;

import com.alphaskyport.masterdata.model.Country;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {

    @Cacheable("countries")
    Optional<Country> findByCountryCode(String countryCode);

    @Cacheable("activeCountries")
    List<Country> findByIsActiveTrue();
}
