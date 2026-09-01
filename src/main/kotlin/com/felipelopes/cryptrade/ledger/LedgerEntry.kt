package com.felipelopes.cryptrade.ledger

import jakarta.persistence.Column
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

    @Column(length = 4000)
    val payload: ByteArray,

    // unico: uma cotacao gera no maximo um lancamento no ledger. E o que impede duas
    // requisicoes concorrentes com o mesmo quoteId de executarem as duas - o segundo append
    // falha aqui e faz rollback da transacao inteira. NULL (CREATE_ACCOUNT/MINT) e distinto
    // em unique tanto no H2 quanto no Postgres, entao multiplos entries sem quoteId convivem.
    @Column(unique = true)
    val quoteId: String? = null,
    val authorAddress: String? = null,
    val signature: String
)
