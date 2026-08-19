package com.felipelopes.cryptrade.service

import com.felipelopes.cryptrade.domain.Wallet
import com.felipelopes.cryptrade.repository.WalletRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class WalletInitializer(
    private val walletRepository: WalletRepository,
    @Value("\${cryptrade.wallet.starting-balance}") private val startingBalance: BigDecimal
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (walletRepository.count() == 0L) {
            walletRepository.save(Wallet(id = 1L, balance = startingBalance))
        }
    }
}
