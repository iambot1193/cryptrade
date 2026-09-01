package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Preco emitido e assinado pelo validador, com validade curta. Cliente nao define preco - so
 * assina { quoteId, quantity } por cima do que o servidor propos.
 * quoteId dobra de chave de idempotencia: resultBlockIndex faz o reenvio do mesmo quoteId
 * devolver o bloco anterior em vez de executar de novo; o indice unico em
 * ledger_entries.quote_id fecha a corrida quando dois reenvios chegam concorrentes.
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

    @Column(precision = 28, scale = 8)
    val quantity: BigDecimal,

    @Column(precision = 19, scale = 2)
    val price: BigDecimal,
    val expiresAt: Instant,
    val validatorSignature: String,

    var resultBlockIndex: Long? = null
)
