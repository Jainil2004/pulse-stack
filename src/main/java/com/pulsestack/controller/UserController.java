package com.pulsestack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/checkStatus")
    public String checkUserStatus() {
        return "this is a user only endpoint";
    }

    @GetMapping("/profile")
    public String getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        return "profile information for user: " + username;
        
    }
}
