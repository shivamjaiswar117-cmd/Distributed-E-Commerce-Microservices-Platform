package com.distributed.ecommerce.user.dto;

public record AuthResponse(String token, Long id, String fullName, String email) {
}