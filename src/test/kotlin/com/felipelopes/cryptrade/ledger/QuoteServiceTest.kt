package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class QuoteServiceTest {

    @Autowired
    lateinit var accountService: AccountService

    @Autowired
    lateinit var quoteService: QuoteService

    @Autowired
    lateinit var validatorKeys: ValidatorKeyProvider

    private fun fundedAddress(): String {
        val keyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        return accountService.createAccount(
            publicKeyBase64,
            TestClientKeys.signRawBase64(keyPair.private, publicKeyBytes)
        ).address
    }

    @Test
    fun `cotacao pedida com ticker curto guarda o id CoinGecko e assina sobre ele`() {
        val address = fundedAddress()

        val quote = quoteService.createQuote(address, "BTC", OrderSide.BUY, BigDecimal("0.01"))

        // simbolo canonico persistido: a ordem executa e o replay decodifica sempre "bitcoin",
        // nunca "BTC".
        assertEquals("bitcoin", quote.symbol)

        // a assinatura do validador tem que fechar sobre o simbolo canonico - senao a ordem
        // assinada pelo cliente bateria contra bytes diferentes dos que o servidor assinou.
        val signedBytes = CanonicalSerializer.canonicalize(
            listOf(
                quote.quoteId,
                address,
                "bitcoin",
                OrderSide.BUY.name,
                CanonicalSerializer.decimalField(quote.quantity, 8),
                CanonicalSerializer.decimalField(quote.price, 2),
                quote.expiresAt.toString()
            )
        )
        assertTrue(
            SignatureVerifier.verify(
                validatorKeys.publicKey,
                signedBytes,
                Base64.getDecoder().decode(quote.validatorSignature)
            )
        )
    }
}
