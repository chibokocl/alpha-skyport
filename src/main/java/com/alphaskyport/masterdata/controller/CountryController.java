package com.alphaskyport.masterdata.controller;

import com.alphaskyport.masterdata.model.Country;
import com.alphaskyport.masterdata.repository.CountryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @GetMapping
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @GetMapping("/active")
    public List<Country> getActiveCountries() {
        return countryRepository.findByIsActiveTrue();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Country> getCountryByCode(@PathVariable String code) {
        return countryRepository.findByCountryCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @NonNull
    public Country createCountry(@RequestBody Country country) {
        @SuppressWarnings("null")
        Country saved = countryRepository.save(country);
        return saved;
    }
}
