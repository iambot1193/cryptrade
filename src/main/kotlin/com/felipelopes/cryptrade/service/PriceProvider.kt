package com.felipelopes.cryptrade.service

import java.math.BigDecimal

interface PriceProvider {
    fun currentPrice(symbol: String): BigDecimal
}
