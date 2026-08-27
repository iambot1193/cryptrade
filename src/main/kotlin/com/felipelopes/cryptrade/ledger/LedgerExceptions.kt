package com.felipelopes.cryptrade.ledger

class AccountAlreadyExistsException(message: String) : RuntimeException(message)
class AccountNotFoundException(message: String) : RuntimeException(message)
class InvalidSignatureException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String) : RuntimeException(message)
class QuoteNotFoundException(message: String) : RuntimeException(message)
class QuoteExpiredException(message: String) : RuntimeException(message)
class RateLimitedException(message: String) : RuntimeException(message)
