package com.pulsestack.service;

import com.pulsestack.dto.UserDto;
import com.pulsestack.model.User;
import com.pulsestack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found"));
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }

    public UserDto updateProfileByName(String username, String newUsername) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found"));

        user.setUsername(newUsername);
        userRepository.save(user);

        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }

    public boolean deleteAccount(String username) {
        return userRepository.findByUsername(username).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }
}
