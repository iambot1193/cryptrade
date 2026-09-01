package com.felipelopes.cryptrade

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fase 4: prova que Actuator e springdoc subiram e respondem. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:cryptrade-smoke;DB_CLOSE_DELAY=-1"])
class ActuatorAndOpenApiSmokeTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `actuator health responde UP`() {
        val response = restTemplate.getForEntity("/actuator/health", String::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.contains("\"status\":\"UP\""), "corpo: ${response.body}")
    }

    @Test
    fun `spec OpenAPI e servida em v3 api-docs`() {
        val response = restTemplate.getForEntity("/v3/api-docs", String::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.contains("\"openapi\""), "corpo: ${response.body}")
        assertTrue(response.body!!.contains("CryptRade API"), "titulo do OpenApiConfig ausente")
    }
}
