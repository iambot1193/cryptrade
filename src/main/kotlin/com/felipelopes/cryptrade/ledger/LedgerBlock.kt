package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "ledger_blocks")
class LedgerBlock(
    @Id
    val blockIndex: Long,
    val prevHash: String,
    val hash: String,
    val createdAt: Instant,
    val validatorSignature: String
)
