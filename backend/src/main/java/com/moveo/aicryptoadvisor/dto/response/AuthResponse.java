package com.moveo.aicryptoadvisor.dto.response;

import java.util.UUID;

public record AuthResponse(String token, UserResponse user) {

    public record UserResponse(UUID id, String email, String name) {
    }
}
