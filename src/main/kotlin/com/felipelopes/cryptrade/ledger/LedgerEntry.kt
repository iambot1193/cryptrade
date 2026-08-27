package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

enum class EntryType { CREATE_ACCOUNT, MINT, ORDER }

@Entity
@Table(name = "ledger_entries")
class LedgerEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val blockIndex: Long,
    val sequenceInBlock: Int,

    @Enumerated(EnumType.STRING)
    val type: EntryType,

    val payload: ByteArray,

    val quoteId: String? = null,
    val authorAddress: String? = null,
    val signature: String
)
