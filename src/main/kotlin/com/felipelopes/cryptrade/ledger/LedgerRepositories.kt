package com.felipelopes.cryptrade.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, String>

interface PositionRepository : JpaRepository<Position, Long> {
    fun findByAddressAndSymbol(address: String, symbol: String): Position?
    fun findByAddress(address: String): List<Position>
}

interface LedgerBlockRepository : JpaRepository<LedgerBlock, Long> {
    fun findTopByOrderByBlockIndexDesc(): LedgerBlock?
    fun findAllByOrderByBlockIndexAsc(): List<LedgerBlock>
}

interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {
    fun findByBlockIndexOrderBySequenceInBlockAsc(blockIndex: Long): List<LedgerEntry>
    fun findByQuoteId(quoteId: String): LedgerEntry?
}

interface QuoteRepository : JpaRepository<Quote, String>

interface RefreshTokenRepository : JpaRepository<RefreshToken, String>

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findAllByOrderByTimestampDesc(): List<AuditLog>
}
