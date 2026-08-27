package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** So o que NAO vira bloco: login falho, assinatura rejeitada, cotacao expirada, acao de admin. */
@Entity
@Table(name = "audit_log")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val address: String,
    val action: String,
    val timestamp: Instant = Instant.now(),
    val metadata: String? = null
)
