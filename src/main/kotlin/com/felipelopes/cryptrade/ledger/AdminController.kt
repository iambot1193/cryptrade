package com.felipelopes.cryptrade.ledger

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class AuditLogResponse(val address: String, val action: String, val timestamp: Instant, val metadata: String?)

@RestController
@RequestMapping("/admin")
class AdminController(
    private val authService: AuthService,
    private val auditLogRepository: AuditLogRepository
) {

    @GetMapping("/audit")
    fun audit(@RequestHeader("Authorization") authHeader: String): List<AuditLogResponse> {
        val claims = authService.requireAddress(authHeader)
        if (claims.role != "ADMIN") {
            throw UnauthorizedException("acesso restrito a administradores")
        }
        return auditLogRepository.findAllByOrderByTimestampDesc().map {
            AuditLogResponse(it.address, it.action, it.timestamp, it.metadata)
        }
    }
}
