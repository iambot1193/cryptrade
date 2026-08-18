package com.felipelopes.cryptrade.controller

import com.felipelopes.cryptrade.dto.PortfolioResponse
import com.felipelopes.cryptrade.service.TradingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(private val tradingService: TradingService) {

    @GetMapping
    fun portfolio(): PortfolioResponse = tradingService.portfolio()
}
