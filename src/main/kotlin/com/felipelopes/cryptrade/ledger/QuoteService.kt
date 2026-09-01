package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.service.PriceService
import com.felipelopes.cryptrade.service.TickerRegistry
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
    private val priceService: PriceService,
    private val quoteRepository: QuoteRepository,
    private val validatorKeys: ValidatorKeyProvider,
    private val rateLimiter: RateLimiter
) {
    fun createQuote(address: String, symbol: String, side: OrderSide, quantity: BigDecimal): Quote {
        if (!rateLimiter.allow("quote:$address", MAX_QUOTES_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("muitos pedidos de cotacao, tente de novo em instantes")
        }

        // canonicaliza aqui: a Quote guarda "bitcoin" mesmo se o cliente pediu "BTC", entao a
        // ordem que executa contra ela e o replay do ledger veem sempre o mesmo simbolo.
        val coinId = TickerRegistry.resolve(symbol)
        val price = priceService.currentPrice(coinId)
        val quoteId = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plus(QUOTE_TTL)

        val fields = listOf(
            quoteId,
            address,
            coinId,
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
                symbol = coinId,
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
