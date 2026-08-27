package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Revogavel - JWT de acesso e conveniencia de sessao, isso aqui e o que pode ser derrubado. */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    val token: String,

    val address: String,
    val expiresAt: Instant,
    var revokedAt: Instant? = null
) {
    fun isValid(now: Instant): Boolean = revokedAt == null && now.isBefore(expiresAt)
}
