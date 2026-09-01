package com.felipelopes.cryptrade.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TickerRegistryTest {

    @Test
    fun `ticker curto resolve pro id CoinGecko, case e espaco insensiveis`() {
        assertEquals("bitcoin", TickerRegistry.resolve("BTC"))
        assertEquals("ethereum", TickerRegistry.resolve("eth"))
        assertEquals("solana", TickerRegistry.resolve("  SOL  "))
    }

    @Test
    fun `id ja canonico passa direto`() {
        assertEquals("bitcoin", TickerRegistry.resolve("bitcoin"))
    }

    @Test
    fun `simbolo desconhecido passa direto normalizado - o provider decide se existe`() {
        assertEquals("dogecoin", TickerRegistry.resolve("DogeCoin"))
    }
}
