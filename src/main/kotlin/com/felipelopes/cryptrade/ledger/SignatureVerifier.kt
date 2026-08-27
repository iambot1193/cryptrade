package com.felipelopes.cryptrade.ledger

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

/** Ed25519 nativo do JDK (java.security.Signature, suporte desde Java 15) - sem Bouncy Castle. */
object SignatureVerifier {
    private const val ALGORITHM = "Ed25519"

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
}
