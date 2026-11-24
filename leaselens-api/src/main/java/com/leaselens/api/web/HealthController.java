package com.leaselens.api.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness check used by the frontend to show whether the Java backend is reachable. */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final String version;

    public HealthController(@Value("${leaselens.version:dev}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "leaselens-api", "version", version);
    }
}
