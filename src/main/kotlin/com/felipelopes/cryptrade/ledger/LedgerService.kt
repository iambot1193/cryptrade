package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

data class VerificationResult(
    val valid: Boolean,
    val brokenAtBlock: Long?,
    val reason: String?
)

@Service
class LedgerService(
    private val blockRepository: LedgerBlockRepository,
    private val entryRepository: LedgerEntryRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val validatorKeys: ValidatorKeyProvider,
    transactionManager: PlatformTransactionManager
) {
    data class PendingEntry(
        val type: EntryType,
        val fields: List<String>,
        val quoteId: String? = null,
        val authorAddress: String? = null,
        val signature: String
    )

    private val appendLock = Any()
    private val transactionTemplate = TransactionTemplate(transactionManager)

    /**
     * Lock precisa cobrir o commit, nao so o corpo do metodo - por isso TransactionTemplate
     * explicito em vez de @Transactional (proxy commitaria DEPOIS de sair do synchronized).
     */
    fun append(entries: List<PendingEntry>): LedgerBlock =
        synchronized(appendLock) {
            transactionTemplate.execute { doAppend(entries) }!!
        }

    private fun doAppend(entries: List<PendingEntry>): LedgerBlock {
        val previous = blockRepository.findTopByOrderByBlockIndexDesc()
        val nextIndex = (previous?.blockIndex ?: -1L) + 1
        val prevHash = previous?.hash ?: GENESIS_HASH

        val entryBytesList = entries.map { canonicalEntryBytes(it) }
        // truncar pra microssegundo: coluna timestamp do banco arredonda nanos no round-trip,
        // e o hash tem que bater identico entre o que foi assinado e o que sera recalculado no verify()
        val timestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val hash = computeBlockHash(prevHash, entryBytesList, timestamp)
        val signature = Base64.getEncoder()
            .encodeToString(SignatureVerifier.sign(validatorKeys.privateKey, hash.toByteArray(StandardCharsets.UTF_8)))

        val block = blockRepository.save(
            LedgerBlock(
                blockIndex = nextIndex,
                prevHash = prevHash,
                hash = hash,
                createdAt = timestamp,
                validatorSignature = signature
            )
        )

        entries.forEachIndexed { index, pending ->
            entryRepository.save(
                LedgerEntry(
                    blockIndex = block.blockIndex,
                    sequenceInBlock = index,
                    type = pending.type,
                    payload = entryBytesList[index],
                    quoteId = pending.quoteId,
                    authorAddress = pending.authorAddress,
                    signature = pending.signature
                )
            )
            applyProjection(pending)
        }

        return block
    }

    private fun applyProjection(entry: PendingEntry) {
        when (entry.type) {
            EntryType.CREATE_ACCOUNT -> {
                val (address, publicKey) = entry.fields
                accountRepository.save(Account(address = address, publicKey = publicKey))
            }
            EntryType.MINT -> {
                val (address, amount) = entry.fields
                val account = accountRepository.findById(address).orElseThrow()
                account.balance += BigDecimal(amount)
                accountRepository.save(account)
            }
            EntryType.ORDER -> {
                val (address, symbol, side, quantityStr, priceStr) = entry.fields
                val quantity = BigDecimal(quantityStr)
                val price = BigDecimal(priceStr)
                val account = accountRepository.findById(address).orElseThrow()
                val position = positionRepository.findByAddressAndSymbol(address, symbol)
                    ?: Position(address = address, symbol = symbol)

                when (OrderSide.valueOf(side)) {
                    OrderSide.BUY -> {
                        account.balance -= quantity * price
                        val totalCost = position.quantity * position.averagePrice + quantity * price
                        position.quantity += quantity
                        position.averagePrice = if (position.quantity.signum() != 0) {
                            totalCost.divide(position.quantity, 8, RoundingMode.HALF_UP)
                        } else {
                            BigDecimal.ZERO
                        }
                    }
                    OrderSide.SELL -> {
                        account.balance += quantity * price
                        position.quantity -= quantity
                        if (position.quantity.signum() == 0) position.averagePrice = BigDecimal.ZERO
                    }
                }

                accountRepository.save(account)
                positionRepository.save(position)
            }
        }
    }

    fun verify(): VerificationResult {
        var expectedPrevHash = GENESIS_HASH
        for (block in blockRepository.findAllByOrderByBlockIndexAsc()) {
            if (block.prevHash != expectedPrevHash) {
                return VerificationResult(false, block.blockIndex, "prevHash mismatch")
            }

            val entries = entryRepository.findByBlockIndexOrderBySequenceInBlockAsc(block.blockIndex)
            val recomputedHash = computeBlockHash(block.prevHash, entries.map { it.payload }, block.createdAt)
            if (recomputedHash != block.hash) {
                return VerificationResult(false, block.blockIndex, "hash mismatch")
            }

            val signatureValid = SignatureVerifier.verify(
                validatorKeys.publicKey,
                block.hash.toByteArray(StandardCharsets.UTF_8),
                Base64.getDecoder().decode(block.validatorSignature)
            )
            if (!signatureValid) {
                return VerificationResult(false, block.blockIndex, "invalid validator signature")
            }

            expectedPrevHash = block.hash
        }
        return VerificationResult(true, null, null)
    }

    private fun canonicalEntryBytes(entry: PendingEntry): ByteArray =
        CanonicalSerializer.canonicalize(
            listOf(entry.type.name) + entry.fields + listOf(
                entry.quoteId ?: "",
                entry.authorAddress ?: "",
                entry.signature
            )
        )

    private fun computeBlockHash(prevHash: String, entryBytesList: List<ByteArray>, timestamp: Instant): String {
        val out = ByteArrayOutputStream()
        out.write(prevHash.toByteArray(StandardCharsets.UTF_8))
        entryBytesList.forEach(out::write)
        out.write(timestamp.toString().toByteArray(StandardCharsets.UTF_8))
        return CanonicalSerializer.sha256Hex(out.toByteArray())
    }

    companion object {
        val GENESIS_HASH = "0".repeat(64)
    }
}
