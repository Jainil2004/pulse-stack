package com.pulsestack.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsestack.TestDataUtil;
import com.pulsestack.model.User;
import com.pulsestack.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthenticationControllerIntegrationTest {

    @Autowired
    private AuthenticationService authService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testThatUserCanRegisterInTheSystem() throws Exception {
        User user1 = TestDataUtil.createNormalUser1();
        user1.setId(null);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

    @Test
    public void testThatUserCanLoginIntoTheSystem() throws Exception {
        User user1 = TestDataUtil.createNormalUser1();
        user1.setId(null);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }
}
