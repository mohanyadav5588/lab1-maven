package com.devops.lab1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StatusController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:Lab1-Java-Maven}")
    private String appName;

    @Value("${spring.datasource.url:not-configured}")
    private String dbUrl;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        // App info
        response.put("lab", "DevOps Training - Lab 1");
        response.put("stack", "Java Spring Boot + Maven + PostgreSQL");
        response.put("appName", appName);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Backend status
        try {
            response.put("backendStatus", "✅ ONLINE");
            response.put("hostname", InetAddress.getLocalHost().getHostName());
            response.put("javaVersion", System.getProperty("java.version"));
        } catch (Exception e) {
            response.put("backendStatus", "⚠️ Partial");
        }

        // Database status
        try {
            String dbVersion = jdbcTemplate.queryForObject(
                "SELECT version()", String.class);
            response.put("dbStatus", "✅ CONNECTED");
            response.put("dbVersion", dbVersion != null ? dbVersion.split(",")[0] : "PostgreSQL");
            response.put("dbUrl", dbUrl.replaceAll("password=[^&]*", "password=***"));

            // Ping latency test
            long start = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            response.put("dbLatencyMs", System.currentTimeMillis() - start);

        } catch (Exception e) {
            response.put("dbStatus", "❌ DISCONNECTED");
            response.put("dbError", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "lab1-java-maven");
        return ResponseEntity.ok(health);
    }
}
