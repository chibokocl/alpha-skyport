package com.alphaskyport.iam.controller;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.iam.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    @NonNull
    public User createUser(@RequestBody User user) {
        @SuppressWarnings("null")
        User savedUser = userRepository.save(user);
        return savedUser;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        @SuppressWarnings("null")
        java.util.Optional<User> user = userRepository.findById(id);
        return user
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
