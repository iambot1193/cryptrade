package com.felipelopes.cryptrade.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class ChallengeResponse(val nonce: String, val expiresAt: Instant)

data class VerifyRequest(
    @field:NotBlank
    val address: String,

    @field:NotBlank
    val signature: String
)

data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresAt: Instant)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String
)

data class AccessTokenResponse(val accessToken: String, val expiresAt: Instant)

data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String
)
