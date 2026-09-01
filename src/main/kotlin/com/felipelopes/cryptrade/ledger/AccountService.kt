package com.felipelopes.cryptrade.ledger

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.util.Base64

@Service
class AccountService(
    private val ledgerService: LedgerService,
    private val accountRepository: AccountRepository,
    private val validatorKeys: ValidatorKeyProvider,
    private val rateLimiter: RateLimiter,
    @Value("\${cryptrade.account.starting-balance}") private val startingBalance: BigDecimal,
    @Value("\${cryptrade.admin.address:}") private val adminAddress: String
) {
    /**
     * Endereco = hash da chave publica. Signature prova posse da privada - sem isso
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

        // Limite global: cada conta criada grava um bloco e minta o saldo inicial. Chave unica
        // (nao por address) porque o address vem de chave publica arbitraria - por-address so
        // encheria o mapa do limiter. Depois da verificacao de assinatura pra forja nao gastar
        // o orcamento.
        if (!rateLimiter.allow("create-account", MAX_ACCOUNTS_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("criacao de contas esta temporariamente limitada, tente de novo em instantes")
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

        val account = accountRepository.findById(address).orElseThrow()

        // Promove no ato da criacao se o address bate com cryptrade.admin.address. O AdminSeeder
        // so roda no boot, entao sem isto a conta admin criada depois do start so viraria ADMIN
        // num restart - e o fluxo documentado (preencher a config apos criar a 1a conta) e
        // justamente esse.
        if (adminAddress.isNotBlank() && address == adminAddress && account.role != "ADMIN") {
            account.role = "ADMIN"
            accountRepository.save(account)
        }

        return account
    }

    private fun decodeBase64(value: String, field: String): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            throw InvalidSignatureException("$field nao e base64 valido")
        }

    companion object {
        // ponytail: teto fixo, global. Vira propriedade em application.yml se a demo precisar
        // de outro valor ou o teste de suite passar de ~50 contas/minuto.
        private const val MAX_ACCOUNTS_PER_MIN = 60
    }
}
