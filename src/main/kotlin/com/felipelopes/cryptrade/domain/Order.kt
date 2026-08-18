package com.felipelopes.cryptrade.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.Instant

@Entity
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val symbol: String,

    @Enumerated(EnumType.STRING)
    val side: OrderSide,

    val quantity: BigDecimal,

    val price: BigDecimal,

    val total: BigDecimal,

    val executedAt: Instant = Instant.now()
)
