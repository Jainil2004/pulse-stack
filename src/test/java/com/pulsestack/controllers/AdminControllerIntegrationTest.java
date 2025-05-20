package com.pulsestack.controllers;

import com.pulsestack.TestDataUtil;
import com.pulsestack.model.Role;
import com.pulsestack.model.User;
import com.pulsestack.repository.UserRepository;
import com.pulsestack.service.JwtService;
import org.apache.kafka.common.config.types.Password;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springdoc.webmvc.core.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RequestService requestBuilder;

    @Test
    public void testThatAllTheUsersCanBeRetrieved() throws Exception {
        User adminUser1 = TestDataUtil.createAdminUser1();
        adminUser1.setRole(Role.ADMIN);
        String unencodedPassword = adminUser1.getPassword();
        adminUser1.setPassword(passwordEncoder.encode(adminUser1.getPassword()));
        adminUser1.setId(null);

        userRepository.save(adminUser1);

//        dummy users
        User user1 = TestDataUtil.createNormalUser1();
        user1.setPassword(passwordEncoder.encode(user1.getPassword()));
        User user2 = TestDataUtil.createNormalUser2();
        user2.setPassword(passwordEncoder.encode(user2.getPassword()));
        User user3 = TestDataUtil.createNormalUser3();
        user3.setPassword(passwordEncoder.encode(user3.getPassword()));

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", adminUser1.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/admin/users")
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$", hasSize(4))
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[0].username").value(adminUser1.getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[0].role").value(adminUser1.getRole().name())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[1].username").value(user1.getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[1].role").value(user1.getRole().name())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[2].username").value(user2.getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[2].role").value(user2.getRole().name())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[3].username").value(user3.getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$[3].role").value(user3.getRole().name())
        );
    }

    @Test
    public void testThatAdminCanRetrieveUserDetailsUsingSpecificId() throws Exception{
        User admin = TestDataUtil.createAdminUser1();
        String unencodedPassword = admin.getPassword();
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        userRepository.save(admin);

        User user1 = TestDataUtil.createNormalUser1();
        user1.setPassword(passwordEncoder.encode(user1.getPassword()));
        userRepository.save(user1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", admin.getUsername())
                        .param("password", unencodedPassword)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/admin/users/{id}", user1.getId())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(user1.getUsername())
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.role").value(user1.getRole().name())
        );
    }

    @Test
    public void testThatAdminCanUpdateUsernamesUsingId() throws Exception{
        User admin = TestDataUtil.createAdminUser1();
        String unencodedPassword = admin.getPassword();
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        userRepository.save(admin);

        User user1 = TestDataUtil.createNormalUser1();
        user1.setPassword(passwordEncoder.encode(user1.getPassword()));
        userRepository.save(user1);

        String updatedUsername = "updated";

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", admin.getUsername())
                        .param("password", unencodedPassword)
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/admin/users/{id}", user1.getId())
                        .param("username", updatedUsername)
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andExpect(
                MockMvcResultMatchers.jsonPath("$.username").value(updatedUsername)
        );
    }

    @Test
    public void testThatAdminCanDeleteAnUserUsingSpecificId() throws Exception{
        User admin = TestDataUtil.createAdminUser1();
        String unencodedPassword = admin.getPassword();
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        userRepository.save(admin);

        User user1 = TestDataUtil.createNormalUser1();
        user1.setPassword(passwordEncoder.encode(user1.getPassword()));
        userRepository.save(user1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", admin.getUsername())
                        .param("password", unencodedPassword)
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/admin/users/{id}", user1.getId())
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

    @Test
    public void testThatAdminCanChangeRolesOfUsers() throws Exception {
        User admin = TestDataUtil.createAdminUser1();
        String unencodedPassword = admin.getPassword();
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        userRepository.save(admin);

        User user1 = TestDataUtil.createNormalUser1();
        user1.setPassword(passwordEncoder.encode(user1.getPassword()));
        userRepository.save(user1);

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", admin.getUsername())
                        .param("password", unencodedPassword)
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/admin/users/{id}/role", user1.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newRole", "ADMIN")
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

}
