package com.felipelopes.cryptrade.service

import com.felipelopes.cryptrade.exception.UnknownSymbolException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.BigDecimal

@Component
@Profile("!test & !demo")
class CoinGeckoPriceProvider(private val coinGeckoRestClient: RestClient) : PriceProvider {

    override fun currentPrice(symbol: String): BigDecimal {
        val coinId = symbol.lowercase()
        val response = coinGeckoRestClient.get()
            .uri("/simple/price?ids={id}&vs_currencies=usd", coinId)
            .retrieve()
            .body<Map<String, Map<String, BigDecimal>>>()
            ?: throw UnknownSymbolException("No price data for '$symbol'")

        return response[coinId]?.get("usd")
            ?: throw UnknownSymbolException("Unknown symbol '$symbol'")
    }
}
