package com.felipelopes.cryptrade.ledger

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class ValidatorKeyProvider(
    @Value("\${cryptrade.validator.private-key-base64:}") privateKeyBase64: String,
    @Value("\${cryptrade.validator.public-key-base64:}") publicKeyBase64: String
) {
    private val keyPair: KeyPair = resolveKeyPair(privateKeyBase64, publicKeyBase64)
    val privateKey: PrivateKey get() = keyPair.private
    val publicKey: PublicKey get() = keyPair.public

    companion object {
        private val log = LoggerFactory.getLogger(ValidatorKeyProvider::class.java)

        private fun resolveKeyPair(privateKeyBase64: String, publicKeyBase64: String): KeyPair {
            if (privateKeyBase64.isNotBlank() && publicKeyBase64.isNotBlank()) {
                val keyFactory = KeyFactory.getInstance("Ed25519")
                val privateKey = keyFactory.generatePrivate(
                    PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64))
                )
                val publicKey = keyFactory.generatePublic(
                    X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))
                )
                return KeyPair(publicKey, privateKey)
            }

            val keyPair = SignatureVerifier.generateKeyPair()
            log.warn(
                "cryptrade.validator.private-key-base64 nao configurado - keypair efemero gerado " +
                    "para esta execucao. Blocos assinados agora vao falhar verify() apos um restart " +
                    "sem essa config. Pra persistir, exporte CRYPTRADE_VALIDATOR_PRIVATE_KEY e " +
                    "CRYPTRADE_VALIDATOR_PUBLIC_KEY com os valores abaixo:\nprivate: {}\npublic: {}",
                Base64.getEncoder().encodeToString(keyPair.private.encoded),
                Base64.getEncoder().encodeToString(keyPair.public.encoded)
            )
            return keyPair
        }
    }
}
