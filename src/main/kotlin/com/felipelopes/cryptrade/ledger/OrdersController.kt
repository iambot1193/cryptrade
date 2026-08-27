package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.OrderResponse
import com.felipelopes.cryptrade.dto.PlaceOrderRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrdersController(
    private val orderService: OrderService,
    private val authService: AuthService
) {

    @PostMapping
    fun placeOrder(
        @RequestHeader("Authorization") authHeader: String,
        @Valid @RequestBody request: PlaceOrderRequest
    ): ResponseEntity<OrderResponse> {
        val claims = authService.requireAddress(authHeader)
        val block = orderService.placeOrder(claims.address, request.quoteId, request.signature)
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse(block.blockIndex, block.hash, request.quoteId))
    }
}
