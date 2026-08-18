package com.felipelopes.cryptrade.controller

import com.felipelopes.cryptrade.dto.OrderResponse
import com.felipelopes.cryptrade.dto.PlaceOrderRequest
import com.felipelopes.cryptrade.repository.OrderRepository
import com.felipelopes.cryptrade.service.TradingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val tradingService: TradingService,
    private val orderRepository: OrderRepository
) {

    @PostMapping
    fun placeOrder(@Valid @RequestBody request: PlaceOrderRequest): ResponseEntity<OrderResponse> {
        val order = tradingService.placeOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order))
    }

    @GetMapping
    fun listOrders(): List<OrderResponse> =
        orderRepository.findAll().map(OrderResponse::from)
}
