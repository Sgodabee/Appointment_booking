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


@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource    dataSource;
    private final AppProperties appProperties;
    private static final long   START_TIME = System.currentTimeMillis();

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbOk = checkDatabase();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    dbOk ? "healthy" : "degraded");
        body.put("timestamp", Instant.now().toString());
        body.put("uptimeSeconds", (System.currentTimeMillis() - START_TIME) / 1000);
        body.put("services", Map.of("database", dbOk ? "up" : "down"));

        return ResponseEntity
                .status(dbOk ? 200 : 503)
                .body(body);
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }
}
