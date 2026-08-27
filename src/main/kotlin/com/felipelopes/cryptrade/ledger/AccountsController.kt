package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.dto.CreateAccountRequest
import com.felipelopes.cryptrade.dto.CreateAccountResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/accounts")
class AccountsController(private val accountService: AccountService) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateAccountRequest): ResponseEntity<CreateAccountResponse> {
        val account = accountService.createAccount(request.publicKey, request.signature)
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateAccountResponse(account.address))
    }
}
