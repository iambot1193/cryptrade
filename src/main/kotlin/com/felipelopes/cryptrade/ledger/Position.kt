package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/** Projecao derivada do ledger, por (address, symbol) - recalculavel via replay dos ORDER. */
@Entity(name = "LedgerPosition")
@Table(name = "ledger_positions")
class Position(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val address: String,
    val symbol: String,
    var quantity: BigDecimal = BigDecimal.ZERO,
    var averagePrice: BigDecimal = BigDecimal.ZERO
)
