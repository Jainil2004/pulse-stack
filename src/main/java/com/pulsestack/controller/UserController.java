package com.pulsestack.controller;

import com.pulsestack.dto.UserDto;
import com.pulsestack.repository.UserRepository;
import com.pulsestack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/checkStatus")
    public String checkUserStatus() {
        return "this is a user only endpoint";
    }

    @GetMapping("/profile")
    public UserDto getUserProfile(Authentication authentication) { // the hell does this authentication means man?
        String username = authentication.getName();
        return userService.getUserProfile(username);
    }

    @PutMapping("/profile/update-profile")
    public ResponseEntity<?> updateUserProfile(Authentication authentication, @RequestParam String newUsername) {
        String username = authentication.getName();
        try {
            UserDto userDto = userService.updateProfileByName(username, newUsername);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        String username = authentication.getName();
        boolean deleted = userService.deleteAccount(username);

        if (deleted) {
            return ResponseEntity.ok("account deletion successful");
        } else {
            return ResponseEntity.status(404).body("user not found");
        }
    }

}
