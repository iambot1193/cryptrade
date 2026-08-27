package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.PortfolioResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val orderService: OrderService,
    private val authService: AuthService
) {

    @GetMapping
    fun portfolio(@RequestHeader("Authorization") authHeader: String): PortfolioResponse {
        val claims = authService.requireAddress(authHeader)
        return orderService.portfolio(claims.address)
    }
}
