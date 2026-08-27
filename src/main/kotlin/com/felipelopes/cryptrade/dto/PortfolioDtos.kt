package com.felipelopes.cryptrade.dto

import com.felipelopes.cryptrade.ledger.Position
import java.math.BigDecimal

data class PositionResponse(
    val symbol: String,
    val quantity: BigDecimal,
    val averagePrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val profitLoss: BigDecimal
) {
    companion object {
        fun from(position: Position, currentPrice: BigDecimal): PositionResponse {
            val marketValue = position.quantity * currentPrice
            val costBasis = position.quantity * position.averagePrice
            return PositionResponse(
                symbol = position.symbol,
                quantity = position.quantity,
                averagePrice = position.averagePrice,
                currentPrice = currentPrice,
                marketValue = marketValue,
                profitLoss = marketValue - costBasis
            )
        }
    }
}

data class PortfolioResponse(
    val cashBalance: BigDecimal,
    val positions: List<PositionResponse>,
    val totalEquity: BigDecimal
)
