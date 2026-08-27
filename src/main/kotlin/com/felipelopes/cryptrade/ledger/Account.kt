package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/** Projecao derivada do ledger - balance nao e fonte de verdade, e cache recalculavel via replay. */
@Entity
@Table(name = "accounts")
class Account(
    @Id
    val address: String,
    val publicKey: String,
    var role: String = "USER",
    @Column(precision = 19, scale = 2)
    var balance: BigDecimal = BigDecimal.ZERO
)
