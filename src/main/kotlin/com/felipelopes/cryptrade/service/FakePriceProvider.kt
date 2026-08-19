package com.felipelopes.cryptrade.service

import com.felipelopes.cryptrade.exception.UnknownSymbolException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@Profile("test | demo")
class FakePriceProvider : PriceProvider {

    private val prices = mapOf(
        "bitcoin" to BigDecimal("50000.00"),
        "ethereum" to BigDecimal("3000.00"),
        "solana" to BigDecimal("150.00")
    )

    override fun currentPrice(symbol: String): BigDecimal =
        prices[symbol.lowercase()] ?: throw UnknownSymbolException("Unknown symbol '$symbol'")
}
