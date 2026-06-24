package com.nowthink.controller;

import com.nowthink.model.Discovery;
import com.nowthink.repository.DiscoveryRepository;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discoveries")
@CrossOrigin(origins = "*")
public class DiscoveryController {

    private final DiscoveryEngine discoveryEngine;
    private final DiscoveryRepository discoveryRepository;

    public DiscoveryController(DiscoveryEngine discoveryEngine,
                               DiscoveryRepository discoveryRepository) {
        this.discoveryEngine = discoveryEngine;
        this.discoveryRepository = discoveryRepository;
    }

    @PostMapping("/generate")
    public Discovery generateDiscovery() {
        return discoveryEngine.generateDiscovery();
    }

    @GetMapping
    public List<Discovery> getAllDiscoveries() {
        return discoveryEngine.getAllDiscoveries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDiscovery(@PathVariable Long id) {
        return discoveryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}