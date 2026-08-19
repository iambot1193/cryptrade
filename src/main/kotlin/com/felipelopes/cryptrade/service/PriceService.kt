package com.felipelopes.cryptrade.service

import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PriceService(private val priceProvider: PriceProvider) {

    fun currentPrice(symbol: String): BigDecimal = priceProvider.currentPrice(symbol)
}
