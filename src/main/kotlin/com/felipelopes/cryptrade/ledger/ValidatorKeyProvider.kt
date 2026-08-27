package com.felipelopes.cryptrade.ledger

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64

@Component
class ValidatorKeyProvider {
    private val keyPair = SignatureVerifier.generateKeyPair()
    val privateKey: PrivateKey get() = keyPair.private
    val publicKey: PublicKey get() = keyPair.public

    init {
        // ponytail: keypair efemero por boot -> validatorSignature so verifica dentro da mesma
        // instancia em execucao. Persistir via cryptrade.validator.private-key-base64 quando
        // identidade estavel entre restarts importar (Fase 2/deploy).
        log.warn(
            "Validator Ed25519 keypair gerado para esta execucao. Public key (base64): {}",
            Base64.getEncoder().encodeToString(publicKey.encoded)
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ValidatorKeyProvider::class.java)
    }
}
