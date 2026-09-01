package com.felipelopes.cryptrade.service

/**
 * Aceita ticker curto (BTC) alem do id CoinGecko (bitcoin). Entrada desconhecida passa
 * direto - CoinGecko/FakePriceProvider e quem decide se existe.
 *
 * ponytail: mapa fixo em memoria. O conjunto suportado nao muda em runtime; se um dia
 * precisar de catalogo editavel, vira tabela + migration Flyway.
 */
object TickerRegistry {

    private val aliases = mapOf(
        "btc" to "bitcoin",
        "eth" to "ethereum",
        "sol" to "solana"
    )

    fun resolve(symbol: String): String {
        val key = symbol.trim().lowercase()
        return aliases[key] ?: key
    }
}
