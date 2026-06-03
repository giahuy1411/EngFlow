package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.model.dto.response.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);
    UserResponse login(LoginRequest request);
    UserResponse getProfile(String email);
    void changePassword(String email, com.datn.engflow.model.dto.request.ChangePasswordRequest request);
}
