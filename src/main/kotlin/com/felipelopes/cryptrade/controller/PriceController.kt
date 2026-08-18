package com.felipelopes.cryptrade.controller

import com.felipelopes.cryptrade.service.PriceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/prices")
class PriceController(private val priceService: PriceService) {

    @GetMapping("/{symbol}")
    fun currentPrice(@PathVariable symbol: String): Map<String, BigDecimal> =
        mapOf(symbol to priceService.currentPrice(symbol))
}
