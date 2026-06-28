package com.risheek.resume_screener.dto;

import lombok.Data;
@Data
public class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String email;
        private String username;
        private String role;

        public AuthResponse(String accessToken, String refreshToken, String email, String username, String role) {
            this.email = email;
            this.username = username;
            this.role = role;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

