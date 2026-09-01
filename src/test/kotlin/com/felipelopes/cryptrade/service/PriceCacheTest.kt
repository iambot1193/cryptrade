package com.felipelopes.cryptrade.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

// O perfil de teste desliga o cache (spring.cache.type=none); aqui religa Caffeine so pra este
// contexto pra provar que o @Cacheable de PriceService pega o proxy (auto-invocacao dentro do
// mesmo bean nao pegaria).
@SpringBootTest(
    properties = [
        "spring.cache.type=caffeine",
        "spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=1m"
    ]
)
class PriceCacheTest {

    @MockitoSpyBean
    lateinit var priceProvider: PriceProvider

    @Autowired
    lateinit var priceService: PriceService

    @Test
    fun `segunda consulta do mesmo simbolo sai do cache, nao do provider`() {
        priceService.currentPrice("bitcoin")
        priceService.currentPrice("bitcoin")

        verify(priceProvider, times(1)).currentPrice("bitcoin")
    }
}
