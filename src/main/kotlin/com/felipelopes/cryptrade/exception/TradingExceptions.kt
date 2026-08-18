package com.felipelopes.cryptrade.exception

class InsufficientFundsException(message: String) : RuntimeException(message)

class InsufficientPositionException(message: String) : RuntimeException(message)

class UnknownSymbolException(message: String) : RuntimeException(message)
