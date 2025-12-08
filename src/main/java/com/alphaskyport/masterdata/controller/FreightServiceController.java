package com.alphaskyport.masterdata.controller;

import com.alphaskyport.masterdata.model.FreightService;
import com.alphaskyport.masterdata.repository.FreightServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class FreightServiceController {

    private final FreightServiceRepository freightServiceRepository;

    public FreightServiceController(FreightServiceRepository freightServiceRepository) {
        this.freightServiceRepository = freightServiceRepository;
    }

    @GetMapping
    public List<FreightService> getAllServices() {
        return freightServiceRepository.findAll();
    }

    @GetMapping("/active")
    public List<FreightService> getActiveServices() {
        return freightServiceRepository.findByIsActiveTrue();
    }

    @PostMapping
    public FreightService createService(@RequestBody FreightService service) {
        return freightServiceRepository.save(service);
    }
}
