package com.felipelopes.cryptrade.ledger

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Token de sessao (conveniencia), nao a autorizacao do lancamento - isso e a assinatura Ed25519
 * do cliente. HMAC-SHA256 nativo (javax.crypto) em vez de puxar uma lib de JWT so pra isso.
 */
@Component
class JwtService(
    @Value("\${cryptrade.auth.jwt-secret-base64:}") secretBase64: String,
    private val objectMapper: ObjectMapper
) {
    private val secretKey: SecretKeySpec = resolveSecret(secretBase64)

    data class Claims(val address: String, val role: String, val expiresAt: Instant)

    private data class Payload(val sub: String, val role: String, val exp: Long)

    fun issue(address: String, role: String, ttl: Duration): String {
        val payload = Payload(address, role, Instant.now().plus(ttl).epochSecond)
        val payloadB64 = b64(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$HEADER_B64.$payloadB64"
        val signature = b64(hmac(signingInput))
        return "$signingInput.$signature"
    }

    fun parse(token: String): Claims? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val (header, payload, signature) = parts
        if (header != HEADER_B64) return null

        val expectedSignature = hmac("$header.$payload")
        val actualSignature = try {
            Base64.getUrlDecoder().decode(signature)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) return null

        val claims = try {
            objectMapper.readValue(Base64.getUrlDecoder().decode(payload), Payload::class.java)
        } catch (_: Exception) {
            return null
        }
        val expiresAt = Instant.ofEpochSecond(claims.exp)
        if (Instant.now().isAfter(expiresAt)) return null
        return Claims(claims.sub, claims.role, expiresAt)
    }

    private fun hmac(input: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(secretKey) }.doFinal(input.toByteArray(Charsets.UTF_8))

    private fun b64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    companion object {
        private val log = LoggerFactory.getLogger(JwtService::class.java)
        private val HEADER_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))

        private fun resolveSecret(base64: String): SecretKeySpec {
            if (base64.isNotBlank()) {
                return SecretKeySpec(Base64.getDecoder().decode(base64), "HmacSHA256")
            }
            val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
            log.warn(
                "cryptrade.auth.jwt-secret-base64 nao configurado - segredo efemero gerado para " +
                    "esta execucao (tokens emitidos agora invalidam num restart). Pra persistir, " +
                    "exporte CRYPTRADE_JWT_SECRET com o valor: {}",
                Base64.getEncoder().encodeToString(random)
            )
            return SecretKeySpec(random, "HmacSHA256")
        }
    }
}
