package com.felipelopes.cryptrade.ledger

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class LedgerServiceTest {

    @Autowired
    lateinit var ledgerService: LedgerService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var positionRepository: PositionRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `tampering a committed block is detected by verify()`() {
        val block = ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf("addr-tamper", "pubkey-tamper"),
                    authorAddress = "addr-tamper",
                    signature = "sig-create-tamper"
                )
            )
        )
        assertTrue(ledgerService.verify().valid)

        // simula adulteracao direto no banco (fora da aplicacao) - a mesma coisa que um
        // UPDATE manual via psql faria.
        jdbcTemplate.update(
            "UPDATE ledger_blocks SET hash = ? WHERE block_index = ?",
            "0".repeat(64),
            block.blockIndex
        )

        val result = ledgerService.verify()
        assertTrue(!result.valid)
        assertEquals(block.blockIndex, result.brokenAtBlock)

        // restaura - o contexto Spring (e o H2 por tras dele) e compartilhado entre metodos de
        // teste, um hash corrompido aqui quebraria verify() em qualquer teste rodado depois.
        jdbcTemplate.update(
            "UPDATE ledger_blocks SET hash = ? WHERE block_index = ?",
            block.hash,
            block.blockIndex
        )
        assertTrue(ledgerService.verify().valid)
    }

    @Test
    fun `replay from zero matches the live projection`() {
        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf("addr-replay", "pubkey-replay"),
                    authorAddress = "addr-replay",
                    signature = "sig-create-replay"
                ),
                LedgerService.PendingEntry(
                    type = EntryType.MINT,
                    fields = listOf("addr-replay", "10000.00"),
                    authorAddress = "validator",
                    signature = "sig-mint-replay"
                )
            )
        )
        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.ORDER,
                    fields = listOf("addr-replay", "BTC", "BUY", "0.50000000", "100.00"),
                    authorAddress = "addr-replay",
                    signature = "sig-order-replay"
                )
            )
        )

        val replay = ledgerService.replay()
        val account = accountRepository.findById("addr-replay").orElseThrow()
        val position = positionRepository.findByAddressAndSymbol("addr-replay", "BTC")!!

        assertEquals(0, account.balance.compareTo(replay.balances["addr-replay"]))
        val (replayQuantity, replayAveragePrice) = replay.positions["addr-replay" to "BTC"]!!
        assertEquals(0, position.quantity.compareTo(replayQuantity))
        assertEquals(0, position.averagePrice.compareTo(replayAveragePrice))
    }

    @Test
    fun `append applies ORDER projection to balance and position`() {
        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf("addr-3", "pubkey-3"),
                    authorAddress = "addr-3",
                    signature = "sig-create-3"
                ),
                LedgerService.PendingEntry(
                    type = EntryType.MINT,
                    fields = listOf("addr-3", "10000.00"),
                    authorAddress = "validator",
                    signature = "sig-mint-3"
                )
            )
        )

        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.ORDER,
                    fields = listOf("addr-3", "BTC", "BUY", "0.50000000", "100.00"),
                    authorAddress = "addr-3",
                    signature = "sig-order-3"
                )
            )
        )

        val account = accountRepository.findById("addr-3").orElseThrow()
        assertEquals(0, BigDecimal("9950.00").compareTo(account.balance))

        val position = positionRepository.findByAddressAndSymbol("addr-3", "BTC")!!
        assertEquals(0, BigDecimal("0.50000000").compareTo(position.quantity))
        assertEquals(0, BigDecimal("100.00000000").compareTo(position.averagePrice))
    }

    @Test
    fun `append applies CREATE_ACCOUNT and MINT projection in the same call`() {
        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf("addr-1", "pubkey-1"),
                    authorAddress = "addr-1",
                    signature = "sig-create"
                ),
                LedgerService.PendingEntry(
                    type = EntryType.MINT,
                    fields = listOf("addr-1", "100000.00"),
                    authorAddress = "validator",
                    signature = "sig-mint"
                )
            )
        )

        val account = accountRepository.findById("addr-1").orElseThrow()
        assertEquals(0, BigDecimal("100000.00").compareTo(account.balance))
    }

    @Test
    fun `verify holds after several appends and blocks chain by hash`() {
        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf("addr-2", "pubkey-2"),
                    authorAddress = "addr-2",
                    signature = "sig-create-2"
                )
            )
        )
        val second = ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.MINT,
                    fields = listOf("addr-2", "500.00"),
                    authorAddress = "validator",
                    signature = "sig-mint-2"
                )
            )
        )

        assertTrue(ledgerService.verify().valid)
        assertTrue(second.prevHash.isNotBlank())
    }
}
