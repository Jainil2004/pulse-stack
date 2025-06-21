package com.pulsestack.service;

import com.pulsestack.dto.SystemDto;
import com.pulsestack.model.User;
import com.pulsestack.repository.SystemRepository;
import com.pulsestack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pulsestack.model.System;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SystemService {
    private final SystemRepository systemRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SystemService(SystemRepository systemRepository, UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.systemRepository = systemRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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

    public SystemDto registerSystem(String name) throws NoSuchAlgorithmException {
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
        return new SystemDto(system.getSystemId(), system.getName(), system.getRegisteredAt(), system.getUser().getUsername(), machineAuthToken);
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
}
