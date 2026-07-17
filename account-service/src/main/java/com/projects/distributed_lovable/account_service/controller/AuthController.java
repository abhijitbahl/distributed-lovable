package com.projects.distributed_lovable.account_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projects.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.projects.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.projects.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.projects.distributed_lovable.account_service.service.AuthService;
// import com.projects.distributed_lovable.account_service.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    // private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // @GetMapping("/me")
    // public ResponseEntity<UserProfileResponse> getProfile() {
    // Long userId = 1L;
    // return ResponseEntity.ok(userService.getProfile(userId));
    // } TODO

}
