package com.felipelopes.cryptrade.ledger

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Ed25519 nativo do JDK (java.security.Signature, suporte desde Java 15) - sem Bouncy Castle. */
object SignatureVerifier {
    private const val ALGORITHM = "Ed25519"

    // SubjectPublicKeyInfo fixo pra Ed25519 (RFC 8410): so o algoritmo muda, nunca os parametros.
    // Prefixo antes dos 32 bytes crus que tweetnacl-js (e qualquer client JS) produz direto -
    // KeyFactory.generatePublic exige X.509, cliente nao precisa saber disso.
    private val ED25519_X509_PREFIX = byteArrayOf(
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    )

    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair()

    fun sign(privateKey: PrivateKey, message: ByteArray): ByteArray =
        Signature.getInstance(ALGORITHM).apply {
            initSign(privateKey)
            update(message)
        }.sign()

    fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean =
        Signature.getInstance(ALGORITHM).apply {
            initVerify(publicKey)
            update(message)
        }.verify(signature)

    /** Chave publica Ed25519 crua (32 bytes) -> java.security.PublicKey. */
    fun publicKeyFromRawBytes(raw: ByteArray): PublicKey {
        require(raw.size == 32) { "chave publica Ed25519 deve ter 32 bytes, tinha ${raw.size}" }
        val x509 = ED25519_X509_PREFIX + raw
        return KeyFactory.getInstance(ALGORITHM).generatePublic(X509EncodedKeySpec(x509))
    }

    fun verifyRaw(rawPublicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        try {
            verify(publicKeyFromRawBytes(rawPublicKey), message, signature)
        } catch (_: Exception) {
            false
        }
}
