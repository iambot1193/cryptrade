package com.felipelopes.cryptrade.ledger

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.Base64
import kotlin.test.assertEquals

@SpringBootTest
class AccountServiceTest {

    @Autowired
    lateinit var accountService: AccountService

    @Test
    fun `create account with a genuine signature succeeds`() {
        val keyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val signature = TestClientKeys.signRawBase64(keyPair.private, publicKeyBytes)

        val account = accountService.createAccount(publicKeyBase64, signature)

        assertEquals(CanonicalSerializer.sha256Hex(publicKeyBytes), account.address)
    }

    @Test
    fun `create account with a forged signature is rejected`() {
        val keyPair = SignatureVerifier.generateKeyPair()
        val otherKeyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        // assina com uma chave que NAO corresponde a publicKey enviada
        val forgedSignature = TestClientKeys.signRawBase64(otherKeyPair.private, publicKeyBytes)

        assertThrows(InvalidSignatureException::class.java) {
            accountService.createAccount(publicKeyBase64, forgedSignature)
        }
    }

    @Test
    fun `create account twice for the same key is rejected`() {
        val keyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val signature = TestClientKeys.signRawBase64(keyPair.private, publicKeyBytes)

        accountService.createAccount(publicKeyBase64, signature)

        assertThrows(AccountAlreadyExistsException::class.java) {
            accountService.createAccount(publicKeyBase64, signature)
        }
    }
}
