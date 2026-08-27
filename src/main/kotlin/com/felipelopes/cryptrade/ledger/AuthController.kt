package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.AccessTokenResponse
import com.felipelopes.cryptrade.dto.ChallengeResponse
import com.felipelopes.cryptrade.dto.LogoutRequest
import com.felipelopes.cryptrade.dto.RefreshRequest
import com.felipelopes.cryptrade.dto.TokenResponse
import com.felipelopes.cryptrade.dto.VerifyRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @GetMapping("/challenge")
    fun challenge(@RequestParam address: String): ChallengeResponse {
        val (nonce, expiresAt) = authService.issueChallenge(address)
        return ChallengeResponse(nonce, expiresAt)
    }

    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: VerifyRequest): TokenResponse {
        val tokens = authService.verify(request.address, request.signature)
        return TokenResponse(tokens.accessToken, tokens.refreshToken, tokens.expiresAt)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AccessTokenResponse {
        val (accessToken, expiresAt) = authService.refresh(request.refreshToken)
        return AccessTokenResponse(accessToken, expiresAt)
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Void> {
        authService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }
}
