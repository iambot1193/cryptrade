package com.felipelopes.cryptrade.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val status: Int, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsException::class, InsufficientPositionException::class)
    fun handleTradingConflict(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(HttpStatus.CONFLICT.value(), ex.message ?: "Trading conflict"))

    @ExceptionHandler(UnknownSymbolException::class)
    fun handleUnknownSymbol(ex: UnknownSymbolException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError(HttpStatus.NOT_FOUND.value(), ex.message ?: "Unknown symbol"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ApiError(HttpStatus.BAD_REQUEST.value(), message))
    }
}
