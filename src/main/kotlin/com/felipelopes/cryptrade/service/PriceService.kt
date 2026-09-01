package com.felipelopes.cryptrade.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PriceService(private val priceProvider: PriceProvider) {

    /**
     * Resolve o ticker (BTC -> bitcoin) e cacheia. Cotacao e endpoint de preco passam os dois
     * por aqui, entao e o unico ponto que bate no CoinGecko. TTL/tamanho em
     * spring.cache.caffeine.spec.
     * ponytail: chave = argumento cru, entao "BTC" e "bitcoin" ocupam slots separados. Com
     * TTL de 30s e 500 entradas isso e irrelevante; se incomodar, key = resolve(#symbol).
     */
    @Cacheable("prices")
    fun currentPrice(symbol: String): BigDecimal =
        priceProvider.currentPrice(TickerRegistry.resolve(symbol))
}
