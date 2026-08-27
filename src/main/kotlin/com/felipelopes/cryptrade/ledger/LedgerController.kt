package com.felipelopes.cryptrade.ledger

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LedgerController(private val ledgerService: LedgerService) {

    @GetMapping("/ledger/verify")
    fun verify(): VerificationResult = ledgerService.verify()
}
