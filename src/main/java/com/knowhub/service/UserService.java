package com.knowhub.service;

import com.knowhub.dto.LoginRequest;
import com.knowhub.dto.RegisterRequest;
import com.knowhub.vo.LoginResponse;

public interface UserService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
