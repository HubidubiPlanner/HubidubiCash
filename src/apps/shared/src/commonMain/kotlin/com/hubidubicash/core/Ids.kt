package com.hubidubicash.core

import java.security.MessageDigest
import java.security.SecureRandom

/** Ids: a letter for what it is, then 16 base32 characters (80 bits). */
object Ids {
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz234567"
    private val random = SecureRandom()

    /** A fresh id, "s_…" for a spending, "r_…" for a regular one. */
    fun new(prefix: String): String = prefix + "_" + base32(ByteArray(10).also(random::nextBytes))

    /** The same id every time for the same [text]: SHA-256, its first 80 bits. */
    fun derived(prefix: String, text: String): String =
        prefix + "_" + base32(MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).copyOf(10))

    private fun base32(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return out.toString()
    }
}
