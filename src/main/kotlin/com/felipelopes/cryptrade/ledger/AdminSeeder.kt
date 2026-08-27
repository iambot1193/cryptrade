package com.felipelopes.cryptrade.ledger

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class AdminSeeder(
    private val accountRepository: AccountRepository,
    @Value("\${cryptrade.admin.address:}") private val adminAddress: String
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (adminAddress.isBlank()) return
        val account = accountRepository.findById(adminAddress).orElse(null) ?: return
        if (account.role != "ADMIN") {
            account.role = "ADMIN"
            accountRepository.save(account)
        }
    }
}
