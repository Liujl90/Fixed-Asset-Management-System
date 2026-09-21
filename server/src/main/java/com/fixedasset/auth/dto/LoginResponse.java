package com.fixedasset.auth.dto;

public record LoginResponse(String token, UserProfileResponse user) {
}
