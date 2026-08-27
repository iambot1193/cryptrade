package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.CreateQuoteRequest
import com.felipelopes.cryptrade.dto.QuoteResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/quotes")
class QuoteController(
    private val quoteService: QuoteService,
    private val authService: AuthService
) {

    @PostMapping
    fun create(
        @RequestHeader("Authorization") authHeader: String,
        @Valid @RequestBody request: CreateQuoteRequest
    ): ResponseEntity<QuoteResponse> {
        val claims = authService.requireAddress(authHeader)
        val quote = quoteService.createQuote(claims.address, request.symbol, request.side, request.quantity)
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponse.from(quote))
    }
}
