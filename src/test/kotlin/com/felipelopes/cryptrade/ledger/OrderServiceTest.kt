package com.felipelopes.cryptrade.ledger

import com.felipelopes.cryptrade.domain.OrderSide
import com.felipelopes.cryptrade.exception.InsufficientFundsException
import com.felipelopes.cryptrade.exception.InsufficientPositionException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.security.PrivateKey
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class OrderServiceTest {

    @Autowired
    lateinit var accountService: AccountService

    @Autowired
    lateinit var quoteService: QuoteService

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var accountRepository: AccountRepository

    private fun createFundedAccount(): Pair<String, PrivateKey> {
        val keyPair = SignatureVerifier.generateKeyPair()
        val publicKeyBase64 = TestClientKeys.rawPublicKeyBase64(keyPair)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val account = accountService.createAccount(
            publicKeyBase64,
            TestClientKeys.signRawBase64(keyPair.private, publicKeyBytes)
        )
        return account.address to keyPair.private
    }

    private fun signOrder(privateKey: PrivateKey, quote: Quote): String =
        TestClientKeys.signCanonicalBase64(privateKey, quote.quoteId, CanonicalSerializer.decimalField(quote.quantity, 8))

    private fun assertMoneyEquals(expected: BigDecimal, actual: BigDecimal) =
        assertTrue(expected.compareTo(actual) == 0, "expected $expected but was $actual")

    @Test
    fun `buy then sell updates balance and closes position`() {
        val (address, privateKey) = createFundedAccount()

        val buyQuote = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("0.002"))
        orderService.placeOrder(address, buyQuote.quoteId, signOrder(privateKey, buyQuote))

        var portfolio = orderService.portfolio(address)
        assertMoneyEquals(BigDecimal("99900.00"), portfolio.cashBalance)
        assertMoneyEquals(BigDecimal("0.002"), portfolio.positions.single().quantity)

        val sellQuote = quoteService.createQuote(address, "bitcoin", OrderSide.SELL, BigDecimal("0.002"))
        orderService.placeOrder(address, sellQuote.quoteId, signOrder(privateKey, sellQuote))

        portfolio = orderService.portfolio(address)
        assertMoneyEquals(BigDecimal("100000.00"), portfolio.cashBalance)
        assertTrue(portfolio.positions.isEmpty())
    }

    @Test
    fun `buy beyond balance is rejected`() {
        val (address, privateKey) = createFundedAccount()
        val quote = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("10"))

        assertThrows(InsufficientFundsException::class.java) {
            orderService.placeOrder(address, quote.quoteId, signOrder(privateKey, quote))
        }
    }

    @Test
    fun `sell without position is rejected`() {
        val (address, privateKey) = createFundedAccount()
        val quote = quoteService.createQuote(address, "ethereum", OrderSide.SELL, BigDecimal("1"))

        assertThrows(InsufficientPositionException::class.java) {
            orderService.placeOrder(address, quote.quoteId, signOrder(privateKey, quote))
        }
    }

    @Test
    fun `forged order signature is rejected`() {
        val (address, _) = createFundedAccount()
        val otherKeyPair = SignatureVerifier.generateKeyPair()
        val quote = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("0.1"))
        val forgedSignature = signOrder(otherKeyPair.private, quote)

        assertThrows(InvalidSignatureException::class.java) {
            orderService.placeOrder(address, quote.quoteId, forgedSignature)
        }
    }

    @Test
    fun `same quoteId submitted twice executes only once`() {
        val (address, privateKey) = createFundedAccount()
        val quote = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("0.1"))
        val signature = signOrder(privateKey, quote)

        val first = orderService.placeOrder(address, quote.quoteId, signature)
        val second = orderService.placeOrder(address, quote.quoteId, signature)

        assertEquals(first.blockIndex, second.blockIndex)
        val account = accountRepository.findById(address).orElseThrow()
        assertMoneyEquals(BigDecimal("95000.00"), account.balance)
    }

    @Test
    fun `concurrent orders on the same account do not double-spend`() {
        val (address, privateKey) = createFundedAccount()
        // saldo inicial 100000.00; duas cotacoes de 1.5 BTC a 50000 = 75000 cada -
        // juntas estourariam o saldo, so uma pode passar.
        val quoteA = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("1.5"))
        val quoteB = quoteService.createQuote(address, "bitcoin", OrderSide.BUY, BigDecimal("1.5"))
        val signatureA = signOrder(privateKey, quoteA)
        val signatureB = signOrder(privateKey, quoteB)

        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)
        val tasks = listOf(
            Callable { barrier.await(); orderService.placeOrder(address, quoteA.quoteId, signatureA) },
            Callable { barrier.await(); orderService.placeOrder(address, quoteB.quoteId, signatureB) }
        )
        val outcomes = executor.invokeAll(tasks).map { runCatching { it.get() } }
        executor.shutdown()

        assertEquals(1, outcomes.count { it.isSuccess })
        val failure = outcomes.single { it.isFailure }
        assertTrue(failure.exceptionOrNull()?.cause is InsufficientFundsException)

        val account = accountRepository.findById(address).orElseThrow()
        assertTrue(account.balance.signum() >= 0)
        assertMoneyEquals(BigDecimal("25000.00"), account.balance)
    }
}
