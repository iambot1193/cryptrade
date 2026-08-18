package com.felipelopes.cryptrade.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.math.BigDecimal

@Entity
class Wallet(
    @Id
    val id: Long = 1L,
    var balance: BigDecimal
)
