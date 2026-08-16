package com.example.demo.controller;

import com.example.demo.dto.DisputeMetricsDTO;
import com.example.demo.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "APIs for retrieving system metrics and analytics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @GetMapping("/disputes")
    @Operation(summary = "Get dispute metrics and analytics")
    public ResponseEntity<DisputeMetricsDTO> getDisputeMetrics() {
        try {
            DisputeMetricsDTO metrics = metricsService.calculateMetrics();
            return new ResponseEntity<>(metrics, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> healthCheck() {
        return new ResponseEntity<>("{\"status\": \"UP\"}", HttpStatus.OK);
    }
}
