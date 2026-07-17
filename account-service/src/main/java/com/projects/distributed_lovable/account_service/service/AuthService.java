package com.projects.distributed_lovable.account_service.service;

import com.projects.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.projects.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.projects.distributed_lovable.account_service.dto.auth.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

}
