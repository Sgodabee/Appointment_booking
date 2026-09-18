package com.appointmentbooking.controller;

import com.appointmentbooking.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HealthController — lightweight application health probe.
 *
 * <p>Exposes a single public endpoint that reports the overall health of the
 * application by verifying connectivity to the PostgreSQL database. This is
 * separate from the Spring Actuator {@code /actuator/health} endpoint and is
 * intended for use by load balancers, container orchestration (e.g. Kubernetes
 * liveness/readiness probes), and monitoring dashboards.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource    dataSource;
    private final AppProperties appProperties;

    /** Records the JVM start time to calculate uptime in seconds. */
    private static final long START_TIME = System.currentTimeMillis();

    /**
     * Return the current health status of the application.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbOk = checkDatabase();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",        dbOk ? "healthy" : "degraded");
        body.put("timestamp",     Instant.now().toString());
        body.put("uptimeSeconds", (System.currentTimeMillis() - START_TIME) / 1000);
        body.put("services",      Map.of("database", dbOk ? "up" : "down"));

        return ResponseEntity
                .status(dbOk ? 200 : 503)
                .body(body);
    }

    /**
     * Attempt to borrow a connection from the DataSource pool and validate it.
     */
    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }
}
