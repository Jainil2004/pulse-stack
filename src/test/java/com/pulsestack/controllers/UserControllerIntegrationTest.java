package com.pulsestack.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsestack.TestDataUtil;
import com.pulsestack.model.User;
import com.pulsestack.service.AuthenticationService;
import com.pulsestack.service.JwtService;
import com.pulsestack.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springdoc.webmvc.core.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testThatUserCanCheckHisProfile() throws Exception{
        User user1 = TestDataUtil.createNormalUser1();
        user1.setId(null);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/register")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/user/profile")
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

    @Test
    public void testThatUserCanUpdateHisProfile() throws Exception {
        User user1 = TestDataUtil.createNormalUser1();
        user1.setId(null);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/register")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/user/profile/update-profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newUsername", "updated")
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }

    @Test
    public void testThatUserCanDeleteHisAccount() throws Exception{
        User user1 = TestDataUtil.createNormalUser1();
        user1.setId(null);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/register")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );

        String jwtToken = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .param("username", user1.getUsername())
                        .param("password", user1.getPassword())
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/user/delete-account")
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(
                MockMvcResultMatchers.status().isOk()
        );
    }
}
