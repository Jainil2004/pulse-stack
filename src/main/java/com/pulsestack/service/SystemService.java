package com.pulsestack.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsestack.dto.ConfigFile;
import com.pulsestack.dto.MetricsIngestRequest;
import com.pulsestack.dto.SystemDto;
import com.pulsestack.model.User;
import com.pulsestack.repository.SystemRepository;
import com.pulsestack.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pulsestack.model.System;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.springframework.kafka.annotation.KafkaListener;

@Service
public class SystemService {
    private final SystemRepository systemRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<Map<String, Object>> dashboardQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_QUEUE_SIZE = 100;

    @Autowired
    public SystemService(SystemRepository systemRepository, UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.systemRepository = systemRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public User getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found"));
    }

//    have a watch here this is a big shit show
    private String hashWithSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public ConfigFile registerSystem(String name) throws NoSuchAlgorithmException {
        User currentUser = getCurrentUser();
        Optional<System> isAlreadyRegistered = systemRepository.findSystemByName(name);
        if (isAlreadyRegistered.isPresent()) {
            throw new RuntimeException("system with the defined name already exists");
        }

        String systemId = UUID.randomUUID().toString();
        String machineAuthToken = jwtService.generateTokenForSystem(systemId);
        String hashedToken = hashWithSHA256(machineAuthToken);

        System system = System.builder()
                .systemId(systemId)
                .name(name)
                .registeredAt(LocalDateTime.now())
                .user(currentUser)
                .authToken(hashedToken)
                .build();

        systemRepository.save(system);
        // build JSON config in-memory
        record Cfg(String systemName, String systemId, String jwtToken,
                   String csvFilePath, String offsetFile, double sendDelay) {}

        Cfg cfg = new Cfg(
                system.getName(),
                system.getSystemId(),
                machineAuthToken,
                "test1.csv",
                "last_processed_offset.txt",
                1.0
        );

        byte[] content;
        try {
            content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(cfg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize client config", e);
        }

        String safeName = system.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = "client-config-" + safeName + ".json";

        return new ConfigFile(content, filename);
    }

    public List<SystemDto> getAllSystems() {
        User currentUser = getCurrentUser();

        List<System> systems = systemRepository.findAllByUser(currentUser);
        List<SystemDto> systemDtos = systems.stream()
                .map(system -> new SystemDto(system.getSystemId(),
                        system.getName(),
                        system.getRegisteredAt(),
                        system.getUser().getUsername())).collect(Collectors.toList());

        return systemDtos;
    }

    public boolean deleteSystemByName(String systemName) {
        User currentUser = getCurrentUser();
        Optional<System> system = systemRepository.findSystemByName(systemName);

        if (system.isEmpty()) {
            throw new RuntimeException("machine with the defined name not found");
        }

        if (!system.get().getUser().equals(currentUser)) {
            throw new RuntimeException("You are not authorized to delete this machine");
        }

        systemRepository.delete(system.get());
        return true;
    }

    public SystemDto updateSystemName(String originalName, String newName) {
        Optional<System> updateSystem = systemRepository.findSystemByName(originalName);
        if (updateSystem.isEmpty()) {
            throw new RuntimeException("system with the defined name not found");
        }

        updateSystem.get().setName(newName);
        systemRepository.save(updateSystem.get());
        return new SystemDto(updateSystem.get().getSystemId(),
                updateSystem.get().getName(),
                updateSystem.get().getRegisteredAt(),
                updateSystem.get().getUser().getUsername());
    }

    public SystemDto getSystemUsingSystemName(String systemName) {
        Optional<System> system = systemRepository.findSystemByName(systemName);
        return SystemDto.builder()
                .name(system.get().getName())
                .registeredAt(system.get().getRegisteredAt())
                .systemId(system.get().getSystemId())
                .username(system.get().getUser().getUsername())
                .build();
    }

    public SystemDto getSystemUsingSystemId(String systemId) {
        Optional<System> system = systemRepository.findBySystemId(systemId);
        return SystemDto.builder()
                .name(system.get().getName())
                .registeredAt(system.get().getRegisteredAt())
                .systemId(system.get().getSystemId())
                .username(system.get().getUser().getUsername())
                .build();
    }

    public boolean validateMetricTypes(Map<String, Object> actualPayload) {
        Map<String, Class<?>> expectedMetricsTypes_dev = Map.ofEntries(
                Map.entry("timestamp", String.class),

                // === CPU Metrics ===
                Map.entry("Core_VIDs_avg_V", Number.class),
                Map.entry("Core_Clocks_avg_MHz", Number.class),
                Map.entry("Ring_LLC_Clock_MHz", Number.class),
                Map.entry("Core_Usage_avg_percent", Number.class),
                Map.entry("Core_Temperatures_avg_C", Number.class),
                Map.entry("Core_Distance_to_TjMAX_avg_C", Number.class),
                Map.entry("CPU_Package_C", Number.class),
                Map.entry("CPU_Package_Power_W", Number.class),
                Map.entry("PL1_Power_Limit_Static_W", Number.class),
                Map.entry("PL1_Power_Limit_Dynamic_W", Number.class),
                Map.entry("PL2_Power_Limit_Static_W", Number.class),
                Map.entry("PL2_Power_Limit_Dynamic_W", Number.class),
                Map.entry("CPU_FAN_RPM", Number.class),
                Map.entry("GPU_FAN_RPM", Number.class),

                // === GPU Metrics ===
                Map.entry("GPU_Temperature", Number.class),
                Map.entry("GPU_Thermal_Limit", Number.class),
                Map.entry("GPU_Core_Voltage", Number.class),
                Map.entry("GPU_Power", Number.class),
                Map.entry("GPU_Clock", Number.class),
                Map.entry("GPU_Core_Load", Number.class),
                Map.entry("GPU_Memory_Usage", Number.class)
        );

        // Reject any unexpected fields
        for (String key : actualPayload.keySet()) {
            if (!expectedMetricsTypes_dev.containsKey(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("unexpected field '%s' in payload", key));
            }
        }

        for (Map.Entry<String, Class<?>> entry : expectedMetricsTypes_dev.entrySet()) {
            String key = entry.getKey();
            Class<?> expectedType = entry.getValue();
            Object value = actualPayload.get(key);

            if (value != null && !expectedType.isInstance(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("invalid type for '%s'. expected type: '%s', got '%s'", key, expectedType.getSimpleName(), value.getClass().getSimpleName()));
            }
        }

        return true;
    }

    public void ingestMetrics(MetricsIngestRequest request) {
        String token = request.getAuthToken();
        String systemId = request.getSystemId();

        String extractedSystemId;
        try {
            extractedSystemId = jwtService.extractSystemId(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }

        if (!extractedSystemId.equals(systemId)) {
            throw new RuntimeException("System ID mismatch between token and request");
        }

        System system = systemRepository.findBySystemId(systemId).orElseThrow(() -> new RuntimeException("system not found"));

        String hashedToken;
        try {
            hashedToken = hashWithSHA256(token);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("token hashing failed", e);
        }

        if (!hashedToken.equals(system.getAuthToken())) {
            throw new RuntimeException("Token mismatch for system: " + systemId);
        }

        Map<String, Object> payload = request.getMetricsPayload();

        if (validateMetricTypes(payload)) {
            Map<String, Object> enrichedPayload = new HashMap<>(request.getMetricsPayload());
            enrichedPayload.put("systemId", systemId);
            enrichedPayload.put("username", system.getUser().getUsername());

            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(enrichedPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize metrics payload", e);
            }

            String ingestionTopic = "metricsIngestion";
            String key = systemId;

            kafkaTemplate.send(ingestionTopic, key, payloadJson);
        }
    }

    public void consumeDashboardLog(String message) {
        try {
            Map<String, Object> logEntry = objectMapper.readValue(message, new TypeReference<>() {});
            if (dashboardQueue.size() >= MAX_QUEUE_SIZE) {
                dashboardQueue.poll();
            }
            dashboardQueue.offer(logEntry);
        } catch (JsonProcessingException ignored) {
//            System.out.println("issue with JSON processing");
        }
    }

    public List<Map<String, Object>> getDashboardLogsForUserAndSystem(String username, String systemId) {
        // Verify the system belongs to the user
        System system = systemRepository.findBySystemId(systemId)
                .orElseThrow(() -> new RuntimeException("System not found"));

        if (!system.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to system data");
        }

        // Filter the logs for username and systemId
        return dashboardQueue.stream()
                .filter(entry -> username.equals(entry.get("username")) && systemId.equals(entry.get("systemId")))
                .collect(Collectors.toList());
    }

}
