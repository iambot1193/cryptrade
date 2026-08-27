package com.felipelopes.cryptrade.ledger

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CanonicalSerializerTest {

    @Test
    fun `known vector produces stable hash`() {
        val bytes = CanonicalSerializer.canonicalize(listOf("CREATE_ACCOUNT", "abc123", "100000.00000000"))

        assertEquals(
            "7446e35407cea93f28232582da50d80ced572ed34e557fefe971c66fcc207578",
            CanonicalSerializer.sha256Hex(bytes)
        )
    }

    @Test
    fun `same fields always produce same bytes`() {
        val a = CanonicalSerializer.canonicalize(listOf("ORDER", "addr1", "bitcoin"))
        val b = CanonicalSerializer.canonicalize(listOf("ORDER", "addr1", "bitcoin"))

        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `field boundaries are unambiguous`() {
        // sem prefixo de tamanho, ("ab","c") e ("a","bc") colidiriam concatenados
        val a = CanonicalSerializer.canonicalize(listOf("ab", "c"))
        val b = CanonicalSerializer.canonicalize(listOf("a", "bc"))

        assert(!a.contentEquals(b))
    }
}
