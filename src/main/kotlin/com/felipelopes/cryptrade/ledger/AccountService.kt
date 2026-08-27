package com.felipelopes.cryptrade.ledger

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Base64

@Service
class AccountService(
    private val ledgerService: LedgerService,
    private val accountRepository: AccountRepository,
    private val validatorKeys: ValidatorKeyProvider,
    @Value("\${cryptrade.account.starting-balance}") private val startingBalance: BigDecimal
) {
    /**
     * Endereco = hash da chave publica (plan.md). Signature prova posse da privada - sem isso
     * qualquer um poderia registrar conta em nome de uma chave publica que nao controla.
     */
    fun createAccount(publicKeyBase64: String, signatureBase64: String): Account {
        val publicKeyBytes = decodeBase64(publicKeyBase64, "publicKey")
        val signatureBytes = decodeBase64(signatureBase64, "signature")

        val address = CanonicalSerializer.sha256Hex(publicKeyBytes)
        if (accountRepository.existsById(address)) {
            throw AccountAlreadyExistsException("conta $address ja existe")
        }
        if (!SignatureVerifier.verifyRaw(publicKeyBytes, publicKeyBytes, signatureBytes)) {
            throw InvalidSignatureException("assinatura nao confere com a chave publica informada")
        }

        val mintFields = listOf(address, CanonicalSerializer.decimalField(startingBalance, 2))
        val mintSignature = Base64.getEncoder().encodeToString(
            SignatureVerifier.sign(validatorKeys.privateKey, CanonicalSerializer.canonicalize(mintFields))
        )

        ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.CREATE_ACCOUNT,
                    fields = listOf(address, publicKeyBase64),
                    authorAddress = address,
                    signature = signatureBase64
                ),
                LedgerService.PendingEntry(
                    type = EntryType.MINT,
                    fields = mintFields,
                    authorAddress = "validator",
                    signature = mintSignature
                )
            )
        )

        return accountRepository.findById(address).orElseThrow()
    }

    private fun decodeBase64(value: String, field: String): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            throw InvalidSignatureException("$field nao e base64 valido")
        }
}
