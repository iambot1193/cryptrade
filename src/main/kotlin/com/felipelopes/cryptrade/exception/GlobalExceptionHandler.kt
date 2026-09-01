package com.felipelopes.cryptrade.exception

import com.felipelopes.cryptrade.ledger.AccountAlreadyExistsException
import com.felipelopes.cryptrade.ledger.AccountNotFoundException
import com.felipelopes.cryptrade.ledger.InvalidSignatureException
import com.felipelopes.cryptrade.ledger.QuoteExpiredException
import com.felipelopes.cryptrade.ledger.QuoteNotFoundException
import com.felipelopes.cryptrade.ledger.RateLimitedException
import com.felipelopes.cryptrade.ledger.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val status: Int, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Toda requisicao recusada passa por aqui - assinatura invalida, cotacao expirada, saldo
     * insuficiente, rate limit. Um log estruturado por rejeicao (vai pro JSON ECS em arquivo).
     */
    private fun reject(ex: Exception, status: HttpStatus): ResponseEntity<ApiError> {
        log.atWarn()
            .addKeyValue("event", "request_rejected")
            .addKeyValue("reason", ex.javaClass.simpleName)
            .addKeyValue("status", status.value())
            .setMessage(ex.message ?: status.reasonPhrase)
            .log()
        return ResponseEntity.status(status).body(ApiError(status.value(), ex.message ?: status.reasonPhrase))
    }

    @ExceptionHandler(InsufficientFundsException::class, InsufficientPositionException::class, QuoteExpiredException::class)
    fun handleConflict(ex: RuntimeException): ResponseEntity<ApiError> = reject(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(UnknownSymbolException::class, AccountNotFoundException::class, QuoteNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ApiError> = reject(ex, HttpStatus.NOT_FOUND)

    @ExceptionHandler(AccountAlreadyExistsException::class)
    fun handleAlreadyExists(ex: AccountAlreadyExistsException): ResponseEntity<ApiError> = reject(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(InvalidSignatureException::class, UnauthorizedException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ApiError> = reject(ex, HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(RateLimitedException::class)
    fun handleRateLimited(ex: RateLimitedException): ResponseEntity<ApiError> = reject(ex, HttpStatus.TOO_MANY_REQUESTS)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.atWarn()
            .addKeyValue("event", "request_rejected")
            .addKeyValue("reason", "MethodArgumentNotValidException")
            .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
            .setMessage(message)
            .log()
        return ResponseEntity.badRequest().body(ApiError(HttpStatus.BAD_REQUEST.value(), message))
    }
}
