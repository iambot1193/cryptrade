package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.exception.InsufficientFundsException
import com.felipelopes.cryptrade.exception.InsufficientPositionException
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

                // Checagem de saldo/posicao mora aqui, nao so no service layer: isso roda dentro
                // do synchronized+transacional do append(), entao e o unico lugar onde "ler saldo
                // + decidir + gravar" e atomico. Um check antes de chamar append() sozinho tem
                // TOCTOU - duas ordens concorrentes passariam no check antes de qualquer uma
                // commitar e gastariam o mesmo saldo.
                when (OrderSide.valueOf(side)) {
                    OrderSide.BUY -> {
                        val cost = quantity * price
                        if (account.balance < cost) {
                            throw InsufficientFundsException("saldo insuficiente: ${account.balance} < $cost")
                        }
                        account.balance -= cost
                        val totalCost = position.quantity * position.averagePrice + quantity * price
                        position.quantity += quantity
                        position.averagePrice = if (position.quantity.signum() != 0) {
                            totalCost.divide(position.quantity, 8, RoundingMode.HALF_UP)
                        } else {
                            BigDecimal.ZERO
                        }
                    }
                    OrderSide.SELL -> {
                        if (position.quantity < quantity) {
                            throw InsufficientPositionException("posicao insuficiente: ${position.quantity} < $quantity")
                        }
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

    data class ReplayResult(
        val balances: Map<String, BigDecimal>,
        val positions: Map<Pair<String, String>, Pair<BigDecimal, BigDecimal>>
    )

    /**
     * Recomputa saldo/posicao do zero decodificando o log, sem tocar nas tabelas accounts/
     * positions - prova que a projecao materializada bate com o que o log realmente contem.
     * Duplica a logica de negocio do applyProjection de proposito: se comparasse contra si mesmo
     * um bug ali nao apareceria aqui.
     */
    fun replay(): ReplayResult {
        val balances = mutableMapOf<String, BigDecimal>()
        val positions = mutableMapOf<Pair<String, String>, Pair<BigDecimal, BigDecimal>>()

        for (block in blockRepository.findAllByOrderByBlockIndexAsc()) {
            for (entry in entryRepository.findByBlockIndexOrderBySequenceInBlockAsc(block.blockIndex)) {
                val decoded = CanonicalSerializer.decode(entry.payload)
                val fields = decoded.subList(1, decoded.size - 3)

                when (entry.type) {
                    EntryType.CREATE_ACCOUNT -> {
                        val (address, _) = fields
                        balances.putIfAbsent(address, BigDecimal.ZERO)
                    }
                    EntryType.MINT -> {
                        val (address, amount) = fields
                        balances[address] = (balances[address] ?: BigDecimal.ZERO) + BigDecimal(amount)
                    }
                    EntryType.ORDER -> {
                        val (address, symbol, side, quantityStr, priceStr) = fields
                        val quantity = BigDecimal(quantityStr)
                        val price = BigDecimal(priceStr)
                        val key = address to symbol
                        val (posQuantity, posAveragePrice) = positions[key] ?: (BigDecimal.ZERO to BigDecimal.ZERO)

                        when (OrderSide.valueOf(side)) {
                            OrderSide.BUY -> {
                                balances[address] = (balances[address] ?: BigDecimal.ZERO) - quantity * price
                                val totalCost = posQuantity * posAveragePrice + quantity * price
                                val newQuantity = posQuantity + quantity
                                val newAveragePrice = if (newQuantity.signum() != 0) {
                                    totalCost.divide(newQuantity, 8, RoundingMode.HALF_UP)
                                } else {
                                    BigDecimal.ZERO
                                }
                                positions[key] = newQuantity to newAveragePrice
                            }
                            OrderSide.SELL -> {
                                balances[address] = (balances[address] ?: BigDecimal.ZERO) + quantity * price
                                val newQuantity = posQuantity - quantity
                                val newAveragePrice = if (newQuantity.signum() == 0) BigDecimal.ZERO else posAveragePrice
                                positions[key] = newQuantity to newAveragePrice
                            }
                        }
                    }
                }
            }
        }

        return ReplayResult(balances, positions)
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
