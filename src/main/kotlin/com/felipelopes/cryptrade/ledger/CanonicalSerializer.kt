package com.felipelopes.cryptrade.ledger

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Bytes deterministicos pra assinatura/hash: campos concatenados com prefixo de tamanho
 * (4 bytes big-endian) + UTF-8. JSON nao garante isso entre linguagens (ordem de chave,
 * formato de decimal) - ver plan.md "Riscos conhecidos".
 */
object CanonicalSerializer {

    fun canonicalize(fields: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        for (field in fields) {
            val bytes = field.toByteArray(StandardCharsets.UTF_8)
            out.write((bytes.size ushr 24) and 0xFF)
            out.write((bytes.size ushr 16) and 0xFF)
            out.write((bytes.size ushr 8) and 0xFF)
            out.write(bytes.size and 0xFF)
            out.write(bytes)
        }
        return out.toByteArray()
    }

    /** Inverso de canonicalize - usado so pelo replay, pra reconstruir os campos do log. */
    fun decode(bytes: ByteArray): List<String> {
        val fields = mutableListOf<String>()
        var offset = 0
        while (offset < bytes.size) {
            val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
            offset += 4
            fields.add(String(bytes, offset, length, StandardCharsets.UTF_8))
            offset += length
        }
        return fields
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun decimalField(value: BigDecimal, scale: Int = 8): String = value.setScale(scale).toPlainString()
}
