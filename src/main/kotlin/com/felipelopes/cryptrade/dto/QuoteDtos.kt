package com.felipelopes.cryptrade.dto

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.ledger.Quote
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class CreateQuoteRequest(
    @field:NotBlank
    val symbol: String,

    val side: OrderSide,

    @field:DecimalMin(value = "0.00000001")
    val quantity: BigDecimal
)

data class QuoteResponse(
    val quoteId: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val expiresAt: Instant,
    val signature: String
) {
    companion object {
        fun from(quote: Quote) = QuoteResponse(
            quoteId = quote.quoteId,
            symbol = quote.symbol,
            side = quote.side,
            quantity = quote.quantity,
            price = quote.price,
            expiresAt = quote.expiresAt,
            signature = quote.validatorSignature
        )
    }
}
