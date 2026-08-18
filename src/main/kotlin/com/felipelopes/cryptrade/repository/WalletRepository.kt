package com.felipelopes.cryptrade.repository

import com.felipelopes.cryptrade.domain.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository : JpaRepository<Wallet, Long>
