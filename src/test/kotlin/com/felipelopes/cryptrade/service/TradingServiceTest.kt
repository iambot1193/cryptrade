package com.felipelopes.cryptrade.service

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.dto.PlaceOrderRequest
import com.felipelopes.cryptrade.exception.InsufficientFundsException
import com.felipelopes.cryptrade.exception.InsufficientPositionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@SpringBootTest
class TradingServiceTest {

    @Autowired
    lateinit var tradingService: TradingService

    @MockitoBean
    lateinit var priceService: PriceService

    private fun assertMoneyEquals(expected: BigDecimal, actual: BigDecimal) =
        assertTrue(expected.compareTo(actual) == 0, "expected $expected but was $actual")

    @Test
    fun `buy then sell updates wallet and closes position`() {
        whenever(priceService.currentPrice("bitcoin")).thenReturn(BigDecimal("100"))

        tradingService.placeOrder(PlaceOrderRequest("bitcoin", OrderSide.BUY, BigDecimal("2")))
        var portfolio = tradingService.portfolio()
        assertMoneyEquals(BigDecimal("99800.00"), portfolio.cashBalance)
        assertMoneyEquals(BigDecimal("2"), portfolio.positions.single().quantity)

        tradingService.placeOrder(PlaceOrderRequest("bitcoin", OrderSide.SELL, BigDecimal("2")))
        portfolio = tradingService.portfolio()
        assertMoneyEquals(BigDecimal("100000.00"), portfolio.cashBalance)
        assertEquals(emptyList(), portfolio.positions)
    }

    @Test
    fun `buy beyond balance is rejected`() {
        whenever(priceService.currentPrice("bitcoin")).thenReturn(BigDecimal("1000000"))

        assertThrows(InsufficientFundsException::class.java) {
            tradingService.placeOrder(PlaceOrderRequest("bitcoin", OrderSide.BUY, BigDecimal("1")))
        }
    }

    @Test
    fun `sell without position is rejected`() {
        whenever(priceService.currentPrice("ethereum")).thenReturn(BigDecimal("100"))

        assertThrows(InsufficientPositionException::class.java) {
            tradingService.placeOrder(PlaceOrderRequest("ethereum", OrderSide.SELL, BigDecimal("1")))
        }
    }
}
