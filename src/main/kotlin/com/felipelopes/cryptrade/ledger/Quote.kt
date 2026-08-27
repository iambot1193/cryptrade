package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Preco emitido e assinado pelo validador, com validade curta. Cliente nao define preco (ver
 * plan.md "Cotacao + ordem") - so assina { quoteId, quantity } por cima do que o servidor propos.
 * quoteId dobra de chave de idempotencia: usedAt/resultBlockIndex fazem o replay do mesmo
 * quoteId devolver o resultado anterior em vez de executar de novo.
 */
@Entity
@Table(name = "quotes")
class Quote(
    @Id
    val quoteId: String,

    val address: String,
    val symbol: String,

    @Enumerated(EnumType.STRING)
    val side: OrderSide,

    val quantity: BigDecimal,
    val price: BigDecimal,
    val expiresAt: Instant,
    val validatorSignature: String,

    var usedAt: Instant? = null,
    var resultBlockIndex: Long? = null
)
