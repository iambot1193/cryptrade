package com.felipelopes.cryptrade.dto

import com.felipelopes.cryptrade.domain.Order
import com.felipelopes.cryptrade.domain.OrderSide
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class PlaceOrderRequest(
    @field:NotBlank
    val symbol: String,

    val side: OrderSide,

    @field:DecimalMin(value = "0.00000001")
    val quantity: BigDecimal
)

data class OrderResponse(
    val id: Long,
    val symbol: String,
    val side: OrderSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val total: BigDecimal,
    val executedAt: Instant
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id!!,
            symbol = order.symbol,
            side = order.side,
            quantity = order.quantity,
            price = order.price,
            total = order.total,
            executedAt = order.executedAt
        )
    }
}
