package com.linktrack.dto.response;

import com.linktrack.model.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String name, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
