package com.example.demo.service;

import com.example.demo.dto.UserRegistrationRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.model.User;

public interface UserService {
    User registerUser(UserRegistrationRequestDto dto);
    UserResponseDto findByEmail(String email);
}