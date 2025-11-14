package org.example.javamusicapp.controller.authController.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String email;
}
