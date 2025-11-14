package org.example.javamusicapp.controller.authController.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String tokenType = "Bearer";
}
