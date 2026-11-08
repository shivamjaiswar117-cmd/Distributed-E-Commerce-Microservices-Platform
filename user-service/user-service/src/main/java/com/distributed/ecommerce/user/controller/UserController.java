package com.distributed.ecommerce.user.controller;

import com.distributed.ecommerce.user.dto.AuthResponse;
import com.distributed.ecommerce.user.dto.LoginRequest;
import com.distributed.ecommerce.user.dto.RegisterRequest;
import com.distributed.ecommerce.user.entity.User;
import com.distributed.ecommerce.user.security.JwtService;
import com.distributed.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userService.emailExists(request.email())) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered"));
        }
        User user = userService.registerUser(
                new User(request.email(), request.password(), request.fullName()));
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public java.util.List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.authenticate(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(new AuthResponse(
                        jwtService.generateToken(user),
                        user.getId(),
                        user.getFullName(),
                        user.getEmail())))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("message", "Invalid email or password")));
    }
}