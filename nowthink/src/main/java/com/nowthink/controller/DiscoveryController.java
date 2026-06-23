package com.nowthink.controller;

import com.nowthink.model.Discovery;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discoveries")
@CrossOrigin(origins = "*")
public class DiscoveryController {

    private final DiscoveryEngine discoveryEngine;

    public DiscoveryController(DiscoveryEngine discoveryEngine) {
        this.discoveryEngine = discoveryEngine;
    }

    @PostMapping("/generate")
    public Discovery generateDiscovery() {
        return discoveryEngine.generateDiscovery();
    }

    @GetMapping
    public List<Discovery> getAllDiscoveries() {
        return discoveryEngine.getAllDiscoveries();
    }
}
