package org.example.javamusicapp.controller.authController.dto;

import lombok.Data;

@Data
public class AuthResponse {
    //private String token;
    private String username;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
}
