package com.felipelopes.cryptrade.ledger

import java.security.KeyPair
import java.security.PrivateKey
import java.util.Base64

/** Simula o cliente (tweetnacl-js): chave publica e assinatura crus, sem X.509. */
object TestClientKeys {
    fun rawPublicKeyBase64(keyPair: KeyPair): String {
        val x509 = keyPair.public.encoded
        return Base64.getEncoder().encodeToString(x509.copyOfRange(x509.size - 32, x509.size))
    }

    fun signRawBase64(privateKey: PrivateKey, message: ByteArray): String =
        Base64.getEncoder().encodeToString(SignatureVerifier.sign(privateKey, message))

    fun signCanonicalBase64(privateKey: PrivateKey, vararg fields: String): String =
        signRawBase64(privateKey, CanonicalSerializer.canonicalize(fields.toList()))
}
