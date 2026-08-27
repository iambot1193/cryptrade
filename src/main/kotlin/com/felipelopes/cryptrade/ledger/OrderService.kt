package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.dto.PortfolioResponse
import com.felipelopes.cryptrade.dto.PositionResponse
import com.felipelopes.cryptrade.exception.InsufficientFundsException
import com.felipelopes.cryptrade.exception.InsufficientPositionException
import com.felipelopes.cryptrade.service.PriceProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class OrderService(
    private val ledgerService: LedgerService,
    private val ledgerBlockRepository: LedgerBlockRepository,
    private val quoteRepository: QuoteRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val priceProvider: PriceProvider,
    private val rateLimiter: RateLimiter
) {
    @Transactional
    fun placeOrder(address: String, quoteId: String, signatureBase64: String): LedgerBlock {
        if (!rateLimiter.allow("order:$address", MAX_ORDERS_PER_MIN, Duration.ofMinutes(1))) {
            throw RateLimitedException("muitas ordens, tente de novo em instantes")
        }

        val quote = quoteRepository.findById(quoteId)
            .orElseThrow { QuoteNotFoundException("cotacao $quoteId nao encontrada") }
        if (quote.address != address) {
            throw InvalidSignatureException("cotacao nao pertence a essa conta")
        }

        val account = accountRepository.findById(address)
            .orElseThrow { AccountNotFoundException("conta $address nao encontrada") }
        val publicKeyBytes = Base64.getDecoder().decode(account.publicKey)
        val signatureBytes = try {
            Base64.getDecoder().decode(signatureBase64)
        } catch (_: IllegalArgumentException) {
            throw InvalidSignatureException("signature invalida")
        }
        val signedFields = CanonicalSerializer.canonicalize(
            listOf(quoteId, CanonicalSerializer.decimalField(quote.quantity, 8))
        )
        if (!SignatureVerifier.verifyRaw(publicKeyBytes, signedFields, signatureBytes)) {
            throw InvalidSignatureException("assinatura da ordem nao confere com { quoteId, quantity }")
        }

        // Idempotencia: mesmo quoteId reenviado (retry, duplo clique) devolve o bloco ja gerado
        // em vez de executar de novo.
        quote.resultBlockIndex?.let { blockIndex ->
            return ledgerBlockRepository.findById(blockIndex).orElseThrow()
        }

        if (Instant.now().isAfter(quote.expiresAt)) {
            throw QuoteExpiredException("cotacao $quoteId expirou")
        }

        when (quote.side) {
            OrderSide.BUY -> {
                val cost = quote.quantity * quote.price
                if (account.balance < cost) {
                    throw InsufficientFundsException("saldo insuficiente: ${account.balance} < $cost")
                }
            }
            OrderSide.SELL -> {
                val held = positionRepository.findByAddressAndSymbol(address, quote.symbol)?.quantity ?: BigDecimal.ZERO
                if (held < quote.quantity) {
                    throw InsufficientPositionException("posicao insuficiente: $held < ${quote.quantity}")
                }
            }
        }

        val block = ledgerService.append(
            listOf(
                LedgerService.PendingEntry(
                    type = EntryType.ORDER,
                    fields = listOf(
                        address,
                        quote.symbol,
                        quote.side.name,
                        CanonicalSerializer.decimalField(quote.quantity, 8),
                        CanonicalSerializer.decimalField(quote.price, 2)
                    ),
                    quoteId = quoteId,
                    authorAddress = address,
                    signature = signatureBase64
                )
            )
        )

        quote.usedAt = Instant.now()
        quote.resultBlockIndex = block.blockIndex
        quoteRepository.save(quote)

        return block
    }

    @Transactional(readOnly = true)
    fun portfolio(address: String): PortfolioResponse {
        val account = accountRepository.findById(address)
            .orElseThrow { AccountNotFoundException("conta $address nao encontrada") }
        val positions = positionRepository.findByAddress(address).map { position ->
            val currentPrice = priceProvider.currentPrice(position.symbol)
            PositionResponse.from(position, currentPrice)
        }
        val totalEquity = account.balance + positions.sumOf { it.marketValue }
        return PortfolioResponse(cashBalance = account.balance, positions = positions, totalEquity = totalEquity)
    }

    companion object {
        private const val MAX_ORDERS_PER_MIN = 20
    }
}
