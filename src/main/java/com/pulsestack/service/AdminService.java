package com.pulsestack.service;

import com.pulsestack.dto.UserDto;
import com.pulsestack.model.Role;
import com.pulsestack.model.User;
import com.pulsestack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

//    it is preferable to return DTOs directly from the service layer instead of performing the conversion in the controller
//    for scalability as it promotes clearer separation of concerns, reduces redundancy and aligns well with
//    larger-scale projects where multiple controllers may depend on consistent data formatting.

    private final UserRepository userRepository;

    public UserDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found"));
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(user.getId(),
                        user.getUsername(),
                        user.getRole()))
                .collect(Collectors.toList());

        return userDtos;
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user with the defined ID not found"));
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }


    public void updateUser(UserDto userDto) {
        User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new RuntimeException("user not found"));
        if (user != null) {
            user.setUsername(userDto.getUsername());
            userRepository.save(user);
        }
    }

    public boolean deleteUserById(Long id) {
        userRepository.deleteById(id);
        return !userRepository.existsById(id); // updated based on the issue detected in integration testing
    }

    public UserDto updateUserRole(Long id, String newRole) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user not found"));

        if (!newRole.equalsIgnoreCase("USER") && !newRole.equalsIgnoreCase("ADMIN")) {
            throw new IllegalArgumentException("invalid role specified");
        }

        if (newRole.equalsIgnoreCase("USER")) {
            user.setRole(Role.USER);
        } else {
            user.setRole(Role.ADMIN);
        }

        userRepository.save(user);
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }
}
