package com.hubidubicash.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.hubidubicash.core.i18n.t
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream
import java.io.OutputStream

actual val PlexSans: FontFamily = FontFamily(
    Font(resource = "font/plex_sans_regular.ttf", weight = FontWeight.Normal),
    Font(resource = "font/plex_sans_medium.ttf", weight = FontWeight.Medium),
    Font(resource = "font/plex_sans_semibold.ttf", weight = FontWeight.SemiBold),
    Font(resource = "font/plex_sans_semibold.ttf", weight = FontWeight.Bold),
)

@Composable
actual fun SystemBarsFollow(dark: Boolean) = Unit

@Composable
actual fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun rememberFilePicker(): FilePicker = remember {
    object : FilePicker {
        override suspend fun create(name: String): OutputStream? {
            val dialog = FileDialog(null as Frame?, t("Export"), FileDialog.SAVE).apply { file = name; isVisible = true }
            val chosen = dialog.file ?: return null
            return File(dialog.directory, chosen).outputStream()
        }

        override suspend fun open(): InputStream? {
            val dialog = FileDialog(null as Frame?, t("Import"), FileDialog.LOAD).apply { isVisible = true }
            val chosen = dialog.file ?: return null
            return File(dialog.directory, chosen).inputStream()
        }
    }
}
