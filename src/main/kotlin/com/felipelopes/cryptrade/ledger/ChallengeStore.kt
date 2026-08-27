package com.felipelopes.cryptrade.ledger

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Nonce de login por address, em memoria - single-node (ver plan.md), TTL curto o suficiente
 * pra nao precisar sobreviver restart. Um pending challenge por address (o novo substitui o
 * anterior).
 */
@Component
class ChallengeStore {
    private data class Entry(val nonce: String, val expiresAt: Instant)

    private val challenges = ConcurrentHashMap<String, Entry>()
    private val random = SecureRandom()

    fun issue(address: String, ttl: Duration = Duration.ofSeconds(60)): Pair<String, Instant> {
        val nonceBytes = ByteArray(24).also { random.nextBytes(it) }
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes)
        val expiresAt = Instant.now().plus(ttl)
        challenges[address] = Entry(nonce, expiresAt)
        return nonce to expiresAt
    }

    /** Devolve o nonce pendente pro address, ou null se nao houver um ainda valido. */
    fun peek(address: String): String? {
        val entry = challenges[address] ?: return null
        if (Instant.now().isAfter(entry.expiresAt)) {
            challenges.remove(address)
            return null
        }
        return entry.nonce
    }

    fun consume(address: String) {
        challenges.remove(address)
    }
}
