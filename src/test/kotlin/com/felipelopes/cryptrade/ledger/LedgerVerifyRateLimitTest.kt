package com.felipelopes.cryptrade.ledger

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

// Contexto proprio (datasource unico) -> RateLimiter proprio, senao gastar o teto aqui faria
// outros testes que chamam verify() no contexto padrao pegarem 429.
@SpringBootTest
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:cryptrade-verifylimit;DB_CLOSE_DELAY=-1"])
class LedgerVerifyRateLimitTest {

    @Autowired
    lateinit var ledgerService: LedgerService

    @Test
    fun `verify recusa depois do teto de 60 por minuto`() {
        repeat(60) { ledgerService.verify() } // MAX_VERIFY_PER_MIN
        assertThrows(RateLimitedException::class.java) { ledgerService.verify() }
    }
}
