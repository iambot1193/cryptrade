package com.felipelopes.cryptrade.exception

import com.felipelopes.cryptrade.ledger.AccountAlreadyExistsException
import com.felipelopes.cryptrade.ledger.AccountNotFoundException
import com.felipelopes.cryptrade.ledger.InvalidSignatureException
import com.felipelopes.cryptrade.ledger.QuoteExpiredException
import com.felipelopes.cryptrade.ledger.QuoteNotFoundException
import com.felipelopes.cryptrade.ledger.RateLimitedException
import com.felipelopes.cryptrade.ledger.UnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val status: Int, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsException::class, InsufficientPositionException::class, QuoteExpiredException::class)
    fun handleConflict(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(HttpStatus.CONFLICT.value(), ex.message ?: "Conflict"))

    @ExceptionHandler(UnknownSymbolException::class, AccountNotFoundException::class, QuoteNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError(HttpStatus.NOT_FOUND.value(), ex.message ?: "Not found"))

    @ExceptionHandler(AccountAlreadyExistsException::class)
    fun handleAlreadyExists(ex: AccountAlreadyExistsException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(HttpStatus.CONFLICT.value(), ex.message ?: "Already exists"))

    @ExceptionHandler(InvalidSignatureException::class, UnauthorizedException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError(HttpStatus.UNAUTHORIZED.value(), ex.message ?: "Unauthorized"))

    @ExceptionHandler(RateLimitedException::class)
    fun handleRateLimited(ex: RateLimitedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiError(HttpStatus.TOO_MANY_REQUESTS.value(), ex.message ?: "Too many requests"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ApiError(HttpStatus.BAD_REQUEST.value(), message))
    }
}
