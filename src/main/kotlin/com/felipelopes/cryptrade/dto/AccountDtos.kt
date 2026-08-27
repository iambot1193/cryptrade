package com.felipelopes.cryptrade.dto

import jakarta.validation.constraints.NotBlank

data class CreateAccountRequest(
    @field:NotBlank
    val publicKey: String,

    @field:NotBlank
    val signature: String
)

data class CreateAccountResponse(val address: String)
