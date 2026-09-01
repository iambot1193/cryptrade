package com.felipelopes.cryptrade.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun cryptradeOpenApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("CryptRade API")
            .description(
                "Simulador de trading de cripto: ledger event-sourced com hash chain, " +
                    "contas por chave Ed25519 e ordens assinadas pelo cliente. Swagger UI em /swagger-ui.html."
            )
            .version("v1")
    )
}
