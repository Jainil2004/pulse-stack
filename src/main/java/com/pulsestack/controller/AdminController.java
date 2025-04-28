package com.pulsestack.controller;

import com.pulsestack.dto.UserDto;
import com.pulsestack.service.AdminService;
import com.pulsestack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/checkStatus")
    public String checkAdminStatus() {
        return "this is a admin only endpoint";
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    @PutMapping("/users/{id}")
    public UserDto fullUpdateUser(@PathVariable Long id, @RequestParam Map<String, Object> updates) {
        UserDto userDto = adminService.getUserById(id);
        if (userDto != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "username":
                        userDto.setUsername((String) value);
                        break;
                    default:
                        throw new RuntimeException("un-identified key detected");
                }
            });

            adminService.updateUser(userDto);
        }

        return userDto;
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        boolean serviceUpdate = adminService.deleteUserById(id);
        if (serviceUpdate) {
            return ResponseEntity.ok("user deleted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user not found");
        }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestParam String newRole) {
        try {
            UserDto userDto = adminService.updateUserRole(id, newRole);
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
