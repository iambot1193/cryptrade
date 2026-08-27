package com.felipelopes.cryptrade.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, String>

interface LedgerPositionRepository : JpaRepository<Position, Long> {
    fun findByAddressAndSymbol(address: String, symbol: String): Position?
}

interface LedgerBlockRepository : JpaRepository<LedgerBlock, Long> {
    fun findTopByOrderByBlockIndexDesc(): LedgerBlock?
    fun findAllByOrderByBlockIndexAsc(): List<LedgerBlock>
}

interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {
    fun findByBlockIndexOrderBySequenceInBlockAsc(blockIndex: Long): List<LedgerEntry>
}
