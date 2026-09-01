package com.felipelopes.cryptrade.ledger

import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val accountRepository: AccountRepository,
    private val challengeStore: ChallengeStore,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val auditLogRepository: AuditLogRepository,
    private val rateLimiter: RateLimiter
) {
    data class TokenPair(val accessToken: String, val refreshToken: String, val expiresAt: Instant)

    fun issueChallenge(address: String): Pair<String, Instant> {
        // Checa a conta ANTES do rate limiter: senao um address arbitrario (nao ha conta pra
        // ele) cria uma chave nova no mapa em memoria do limiter a cada request e o mapa cresce
        // sem limite. Com a ordem invertida, so addresses de contas reais viram chave.
        if (!accountRepository.existsById(address)) {
            throw AccountNotFoundException("conta $address nao encontrada")
        }
        if (!rateLimiter.allow("challenge:$address", MAX_CHALLENGE_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("muitos pedidos de challenge, tente de novo em instantes")
        }
        return challengeStore.issue(address)
    }

    fun verify(address: String, signatureBase64: String): TokenPair {
        // Conta antes do rate limiter, mesmo motivo do issueChallenge: nao deixar address
        // desconhecido criar chave no mapa do limiter.
        val account = accountRepository.findById(address)
            .orElseThrow { AccountNotFoundException("conta $address nao encontrada") }
        if (!rateLimiter.allow("verify:$address", MAX_VERIFY_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("muitas tentativas de login, tente de novo em instantes")
        }

        val nonce = challengeStore.peek(address) ?: run {
            audit(address, "login_failed", "challenge ausente ou expirado")
            throw UnauthorizedException("challenge ausente ou expirado, peca um novo em GET /auth/challenge")
        }

        val signatureBytes = try {
            Base64.getDecoder().decode(signatureBase64)
        } catch (_: IllegalArgumentException) {
            audit(address, "login_failed", "signature nao e base64 valido")
            throw UnauthorizedException("signature invalida")
        }

        val publicKeyBytes = Base64.getDecoder().decode(account.publicKey)
        val valid = SignatureVerifier.verifyRaw(publicKeyBytes, nonce.toByteArray(Charsets.UTF_8), signatureBytes)
        if (!valid) {
            audit(address, "signature_rejected", "login verify")
            throw UnauthorizedException("assinatura nao confere com o challenge")
        }
        challengeStore.consume(address)

        val accessToken = jwtService.issue(address, account.role, ACCESS_TTL)
        val refreshToken = generateRefreshToken()
        val refreshExpiresAt = Instant.now().plus(REFRESH_TTL)
        refreshTokenRepository.save(RefreshToken(token = refreshToken, address = address, expiresAt = refreshExpiresAt))

        return TokenPair(accessToken, refreshToken, Instant.now().plus(ACCESS_TTL))
    }

    fun refresh(refreshToken: String): Pair<String, Instant> {
        val stored = refreshTokenRepository.findById(refreshToken)
            .orElseThrow { UnauthorizedException("refresh token invalido") }
        if (!stored.isValid(Instant.now())) {
            throw UnauthorizedException("refresh token expirado ou revogado")
        }
        val account = accountRepository.findById(stored.address)
            .orElseThrow { AccountNotFoundException("conta ${stored.address} nao encontrada") }

        val accessToken = jwtService.issue(account.address, account.role, ACCESS_TTL)
        return accessToken to Instant.now().plus(ACCESS_TTL)
    }

    fun logout(refreshToken: String) {
        val stored = refreshTokenRepository.findById(refreshToken).orElse(null) ?: return
        stored.revokedAt = Instant.now()
        refreshTokenRepository.save(stored)
    }

    fun requireAddress(authHeader: String?): JwtService.Claims {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw UnauthorizedException("Authorization header ausente ou invalido")
        }
        return jwtService.parse(authHeader.removePrefix("Bearer "))
            ?: throw UnauthorizedException("token invalido ou expirado")
    }

    private fun audit(address: String, action: String, metadata: String) {
        auditLogRepository.save(AuditLog(address = address, action = action, metadata = metadata))
    }

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private val ACCESS_TTL = Duration.ofMinutes(15)
        private val REFRESH_TTL = Duration.ofDays(7)
        private const val MAX_CHALLENGE_PER_MIN = 10
        private const val MAX_VERIFY_PER_MIN = 10
    }
}
