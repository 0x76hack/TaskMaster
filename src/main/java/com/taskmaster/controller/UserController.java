package com.taskmaster.controller;

import com.taskmaster.dto.UserDto;
import com.taskmaster.dto.UserProfileRequest;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.model.User;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UserProfileRequest profileRequest) {
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        if (profileRequest.username() != null && !profileRequest.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(profileRequest.username())) {
                throw new BadRequestException("Username is already taken.");
            }
            user.setUsername(profileRequest.username());
        }

        if (profileRequest.email() != null && !profileRequest.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(profileRequest.email())) {
                throw new BadRequestException("Email Address already in use.");
            }
            user.setEmail(profileRequest.email());
        }

        if (profileRequest.password() != null && !profileRequest.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(profileRequest.password()));
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }
}
