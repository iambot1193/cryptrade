package com.felipelopes.cryptrade.service

import com.felipelopes.cryptrade.domain.Order
import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.domain.Position
import com.felipelopes.cryptrade.dto.PlaceOrderRequest
import com.felipelopes.cryptrade.dto.PortfolioResponse
import com.felipelopes.cryptrade.dto.PositionResponse
import com.felipelopes.cryptrade.exception.InsufficientFundsException
import com.felipelopes.cryptrade.exception.InsufficientPositionException
import com.felipelopes.cryptrade.repository.OrderRepository
import com.felipelopes.cryptrade.repository.PositionRepository
import com.felipelopes.cryptrade.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TradingService(
    private val orderRepository: OrderRepository,
    private val positionRepository: PositionRepository,
    private val walletRepository: WalletRepository,
    private val priceService: PriceService
) {

    @Transactional
    fun placeOrder(request: PlaceOrderRequest): Order {
        val price = priceService.currentPrice(request.symbol)
        val total = price * request.quantity
        val wallet = walletRepository.findById(1L).orElseThrow()
        val position = positionRepository.findBySymbol(request.symbol)

        when (request.side) {
            OrderSide.BUY -> {
                if (wallet.balance < total) {
                    throw InsufficientFundsException(
                        "Insufficient funds: balance ${wallet.balance} < required $total"
                    )
                }
                wallet.balance -= total
                applyBuy(request.symbol, request.quantity, price, position)
            }
            OrderSide.SELL -> {
                val heldQuantity = position?.quantity ?: BigDecimal.ZERO
                if (heldQuantity < request.quantity) {
                    throw InsufficientPositionException(
                        "Insufficient position: held $heldQuantity < requested ${request.quantity}"
                    )
                }
                wallet.balance += total
                applySell(request.quantity, position!!)
            }
        }

        walletRepository.save(wallet)
        val order = Order(
            symbol = request.symbol,
            side = request.side,
            quantity = request.quantity,
            price = price,
            total = total
        )
        return orderRepository.save(order)
    }

    private fun applyBuy(symbol: String, quantity: BigDecimal, price: BigDecimal, existing: Position?) {
        if (existing == null) {
            positionRepository.save(Position(symbol = symbol, quantity = quantity, averagePrice = price))
            return
        }
        val newQuantity = existing.quantity + quantity
        val newCostBasis = (existing.quantity * existing.averagePrice) + (quantity * price)
        existing.averagePrice = newCostBasis / newQuantity
        existing.quantity = newQuantity
        positionRepository.save(existing)
    }

    private fun applySell(quantity: BigDecimal, position: Position) {
        position.quantity -= quantity
        if (position.quantity == BigDecimal.ZERO) {
            positionRepository.delete(position)
        } else {
            positionRepository.save(position)
        }
    }

    @Transactional(readOnly = true)
    fun portfolio(): PortfolioResponse {
        val wallet = walletRepository.findById(1L).orElseThrow()
        val positions = positionRepository.findAll().map { position ->
            val currentPrice = priceService.currentPrice(position.symbol)
            PositionResponse.from(position, currentPrice)
        }
        val totalEquity = wallet.balance + positions.sumOf { it.marketValue }
        return PortfolioResponse(
            cashBalance = wallet.balance,
            positions = positions,
            totalEquity = totalEquity
        )
    }
}
