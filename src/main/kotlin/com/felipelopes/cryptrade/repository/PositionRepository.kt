package com.felipelopes.cryptrade.repository

import com.felipelopes.cryptrade.domain.Position
import org.springframework.data.jpa.repository.JpaRepository

interface PositionRepository : JpaRepository<Position, Long> {
    fun findBySymbol(symbol: String): Position?
}
