package com.pulsestack.controllers;

import com.pulsestack.TestDataUtil;
import com.pulsestack.controller.SystemController;
import com.pulsestack.model.System;
import com.pulsestack.model.User;
import com.pulsestack.repository.SystemRepository;
import com.pulsestack.repository.UserRepository;
import com.pulsestack.service.SystemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SystemControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final SystemService systemService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SystemRepository systemRepository;

    @Autowired
    public SystemControllerIntegrationTest(MockMvc mockMvc, SystemService systemService, PasswordEncoder passwordEncoder, UserRepository userRepository, SystemRepository systemRepository) {
        this.mockMvc = mockMvc;
        this.systemService = systemService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.systemRepository = systemRepository;
    }

    @Test
    public void testThatSystemCanBeRegisteredByAnUser() throws Exception {
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(normalUser1.getPassword()));

        userRepository.save(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        System system1 = TestDataUtil.createSystem1();
        system1.setUser(normalUser1);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.systemId").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.name").value(system1.getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.registeredAt").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(system1.getUser().getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.authToken").isNotEmpty()
        );
    }

    @Test
    public void testThatMultipleSystemsCanBeFetchedByTheUser() throws Exception {
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(normalUser1.getPassword()));

        userRepository.save(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        System system1 = TestDataUtil.createSystem1();
        system1.setUser(normalUser1);

        System system2 = TestDataUtil.createSystem2();
        system2.setUser(normalUser1);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        );

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system2.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/systems")
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$", hasSize(2))
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[0].name").value(system1.getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[0].registeredAt").exists()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[0].username").value(system1.getUser().getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[1].name").value(system2.getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[1].registeredAt").exists()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[1].username").value(system2.getUser().getUsername())
        );
    }

    @Test
    public void testThatUserCanRemoveSystemByName() throws Exception {
        System system1 = TestDataUtil.createSystem1();
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(unencodedPassword));

        userRepository.save(normalUser1);
        system1.setUser(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.name").value(system1.getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.systemId").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.registeredAt").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(system1.getUser().getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.authToken").isNotEmpty()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/systems/{systemName}", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

    @Test
    public void testThatSystemDetailsCanBeFetchedUsingSystemName() throws Exception {
        System system1 = TestDataUtil.createSystem1();
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(normalUser1.getPassword()));
        userRepository.save(normalUser1);
        system1.setUser(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

//        register a new machine
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        Optional<System> retrieved = systemRepository.findSystemByName(system1.getName());

//        fetch system details using system name
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/systems/get/{systemName}", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.systemId").value(retrieved.get().getSystemId())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.registeredAt").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.name").value(retrieved.get().getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(retrieved.get().getUser().getUsername())
        );
    }

    @Test
    public void testThatSystemDetailsCanBeFetchedUsingSystemId() throws Exception {
        System system1 = TestDataUtil.createSystem1();
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(normalUser1.getPassword()));
        userRepository.save(normalUser1);
        system1.setUser(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        Optional<System> retrieved = systemRepository.findSystemByName(system1.getName());

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/systems/{systemId}", retrieved.get().getSystemId())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.systemId").value(retrieved.get().getSystemId())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.registeredAt").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.name").value(retrieved.get().getName())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(retrieved.get().getUser().getUsername())
        );
    }

    @Test
    public void testThatSystemFieldsCanBeUpdated() throws Exception {
        System system1 = TestDataUtil.createSystem1();
        User normalUser1 = TestDataUtil.createNormalUser1();
        String unencodedPassword = normalUser1.getPassword();
        normalUser1.setPassword(passwordEncoder.encode(normalUser1.getPassword()));
        userRepository.save(normalUser1);
        system1.setUser(normalUser1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", normalUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/systems/register")
                        .param("systemName", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        String updatedSystemName = "updated";
        String bodyJson = "{ \"newName\" : \"" + updatedSystemName + "\" }";

        mockMvc.perform(
                MockMvcRequestBuilders.patch("/api/systems/update/{originalName}", system1.getName())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson)
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.systemId").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.registeredAt").isNotEmpty()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.name").value(updatedSystemName)
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(system1.getUser().getUsername())
        );
    }
}
