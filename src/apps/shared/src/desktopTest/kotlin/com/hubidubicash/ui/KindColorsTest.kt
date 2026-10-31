package com.hubidubicash.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.hubidubicash.core.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The circle's eight kind colours stand out from every theme's cards at 3:1
 * or better, light colours on light themes and dark on dark. Their order was
 * chosen with a colour-blindness check around the whole ring, slot 8 beside
 * slot 1 (docs/CONTRIBUTING: "Kind colours"); this keeps the count and the
 * contrast from drifting.
 */
class KindColorsTest {
    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test fun `every kind has a colour in each set, all different`() {
        for (dark in listOf(false, true)) {
            val set = kindColorsHex(dark)
            assertEquals(Kind.entries.size, set.size)
            assertEquals(set.size, set.toSet().size)
        }
    }

    @Test fun `the kind colours stand out from every theme's cards`() {
        val low = mutableListOf<String>()
        for (theme in ColorTheme.entries) for (systemDark in listOf(false, true)) {
            val (c, dark) = paletteOf(theme, systemDark)
            kindColorsHex(dark).forEachIndexed { i, hex ->
                val r = ratio(Color(hex), c.raised)
                if (r < 3.0) low += "$theme${if (systemDark) " (dark)" else ""} ${Kind.entries[i]} ${"%.2f".format(r)}"
            }
        }
        assertTrue(low.isEmpty(), low.joinToString("\n"))
    }
}
