package com.projects.distributed_lovable.account_service.service.impl;

import java.util.ArrayList;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.projects.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.projects.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.projects.distributed_lovable.account_service.entity.User;
import com.projects.distributed_lovable.account_service.mapper.UserMapper;
import com.projects.distributed_lovable.account_service.repository.UserRepository;
import com.projects.distributed_lovable.account_service.service.AuthService;
import com.projects.distributed_lovable.common_lib.error.BadRequestException;
import com.projects.distributed_lovable.common_lib.event.UserSignedUpEvent;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.common_lib.security.JwtUserPrincipal;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AuthUtil authUtil;
    AuthenticationManager authenticationManager;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public AuthResponse signup(SignupRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BadRequestException("User already exists with username: " + request.username());
        });
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        kafkaTemplate.send("user-signed-up-event", new UserSignedUpEvent(user.getId(), user.getUsername()));

        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(user.getId(), user.getName(), user.getUsername(), null,
                new ArrayList<>());

        String token = authUtil.generateAccessToken(jwtUserPrincipal);
        return new AuthResponse(token, userMapper.toUserProfileResponse(jwtUserPrincipal));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        JwtUserPrincipal user = (JwtUserPrincipal) authentication.getPrincipal();

        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));
    }

}
