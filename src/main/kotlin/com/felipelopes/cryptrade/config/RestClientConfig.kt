package com.felipelopes.cryptrade.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun coinGeckoRestClient(
        @Value("\${cryptrade.pricing.coingecko-base-url}") baseUrl: String
    ): RestClient = RestClient.builder().baseUrl(baseUrl).build()
}
