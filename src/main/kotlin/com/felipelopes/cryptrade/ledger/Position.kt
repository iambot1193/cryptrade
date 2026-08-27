package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/** Projecao derivada do ledger, por (address, symbol) - recalculavel via replay dos ORDER. */
@Entity
@Table(name = "positions")
class Position(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val address: String,
    val symbol: String,

    @Column(precision = 28, scale = 8)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(precision = 19, scale = 2)
    var averagePrice: BigDecimal = BigDecimal.ZERO
)
