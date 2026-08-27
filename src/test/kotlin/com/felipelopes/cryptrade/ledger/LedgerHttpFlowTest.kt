package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.ChallengeResponse
import com.felipelopes.cryptrade.dto.CreateAccountRequest
import com.felipelopes.cryptrade.dto.CreateAccountResponse
import com.felipelopes.cryptrade.dto.CreateQuoteRequest
import com.felipelopes.cryptrade.dto.OrderResponse
import com.felipelopes.cryptrade.dto.PlaceOrderRequest
import com.felipelopes.cryptrade.dto.PortfolioResponse
import com.felipelopes.cryptrade.dto.QuoteResponse
import com.felipelopes.cryptrade.dto.TokenResponse
import com.felipelopes.cryptrade.dto.VerifyRequest
import com.felipelopes.cryptrade.domain.OrderSide
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Item 23 do plan.md: fluxo completo via HTTP, do jeito que um cliente de verdade bateria -
 * criar conta -> challenge -> login -> cotacao -> ordem assinada -> portfolio.
 */
// Contexto proprio (webEnvironment=RANDOM_PORT) -> ValidatorKeyProvider efemero proprio, diferente
// do usado pelos testes @SpringBootTest padrao. Precisa de um H2 nomeado so seu, senao os blocos
// que esse teste appenda (assinados com ESSA chave) aparecem pro outro contexto compartilhando o
// mesmo "jdbc:h2:mem:cryptrade" e verify() rejeita a assinatura por vir de chave diferente.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:cryptrade-http;DB_CLOSE_DELAY=-1"])
class LedgerHttpFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `create account, login, quote and signed order over HTTP`() {
        val keyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)

        val createAccountResponse = restTemplate.postForEntity(
            "/accounts",
            CreateAccountRequest(publicKeyBase64, TestClientKeys.signRawBase64(keyPair.private, publicKeyBytes)),
            CreateAccountResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, createAccountResponse.statusCode)
        val address = createAccountResponse.body!!.address

        val challenge = restTemplate.getForObject(
            "/auth/challenge?address=$address",
            ChallengeResponse::class.java
        )!!

        val loginSignature = TestClientKeys.signRawBase64(keyPair.private, challenge.nonce.toByteArray(Charsets.UTF_8))
        val tokens = restTemplate.postForEntity(
            "/auth/verify",
            VerifyRequest(address, loginSignature),
            TokenResponse::class.java
        ).body!!

        val authHeaders = HttpHeaders().apply { setBearerAuth(tokens.accessToken) }

        val portfolioBefore = restTemplate.exchange(
            "/api/portfolio",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            PortfolioResponse::class.java
        ).body!!
        assertEquals(0, BigDecimal("100000.00").compareTo(portfolioBefore.cashBalance))

        val quoteRequest = HttpEntity(CreateQuoteRequest("bitcoin", OrderSide.BUY, BigDecimal("0.1")), authHeaders)
        val quoteResponse = restTemplate.postForEntity("/quotes", quoteRequest, QuoteResponse::class.java)
        assertEquals(HttpStatus.CREATED, quoteResponse.statusCode)
        val quote = quoteResponse.body!!

        val orderSignature = TestClientKeys.signCanonicalBase64(
            keyPair.private,
            quote.quoteId,
            CanonicalSerializer.decimalField(quote.quantity, 8)
        )
        val orderRequest = HttpEntity(PlaceOrderRequest(quote.quoteId, orderSignature), authHeaders)
        val orderResponse = restTemplate.postForEntity("/api/orders", orderRequest, OrderResponse::class.java)
        assertEquals(HttpStatus.CREATED, orderResponse.statusCode)

        val portfolioAfter = restTemplate.exchange(
            "/api/portfolio",
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders),
            PortfolioResponse::class.java
        ).body!!
        assertEquals(0, BigDecimal("95000.00").compareTo(portfolioAfter.cashBalance))
        assertEquals(0, BigDecimal("0.1").compareTo(portfolioAfter.positions.single().quantity))

        val verifyResponse = restTemplate.getForObject("/ledger/verify", VerificationResult::class.java)!!
        assertTrue(verifyResponse.valid)
    }

    @Test
    fun `order with a wrong bearer token is rejected`() {
        val headers = HttpHeaders().apply { setBearerAuth("not-a-real-token") }
        val request = HttpEntity(PlaceOrderRequest("whatever", "whatever"), headers)
        val response = restTemplate.postForEntity("/api/orders", request, String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
