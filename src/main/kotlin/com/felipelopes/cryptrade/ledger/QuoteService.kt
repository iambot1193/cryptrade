package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.service.PriceProvider
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Cliente nao define preco - servidor busca, assina, e so essa assinatura e o preco final
 * que a ordem executa contra.
 */
@Service
class QuoteService(
    private val priceProvider: PriceProvider,
    private val quoteRepository: QuoteRepository,
    private val validatorKeys: ValidatorKeyProvider,
    private val rateLimiter: RateLimiter
) {
    fun createQuote(address: String, symbol: String, side: OrderSide, quantity: BigDecimal): Quote {
        if (!rateLimiter.allow("quote:$address", MAX_QUOTES_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("muitos pedidos de cotacao, tente de novo em instantes")
        }

        val price = priceProvider.currentPrice(symbol)
        val quoteId = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plus(QUOTE_TTL)

        val fields = listOf(
            quoteId,
            address,
            symbol,
            side.name,
            CanonicalSerializer.decimalField(quantity, 8),
            CanonicalSerializer.decimalField(price, 2),
            expiresAt.toString()
        )
        val signature = Base64.getEncoder().encodeToString(
            SignatureVerifier.sign(validatorKeys.privateKey, CanonicalSerializer.canonicalize(fields))
        )

        return quoteRepository.save(
            Quote(
                quoteId = quoteId,
                address = address,
                symbol = symbol,
                side = side,
                quantity = quantity,
                price = price,
                expiresAt = expiresAt,
                validatorSignature = signature
            )
        )
    }

    companion object {
        private val QUOTE_TTL = Duration.ofSeconds(30)
        private const val MAX_QUOTES_PER_MIN = 30
    }
}
