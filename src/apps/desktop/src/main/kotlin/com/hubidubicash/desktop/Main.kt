package com.hubidubicash.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hubidubicash.core.Settings
import com.hubidubicash.data.desktopDatabase
import com.hubidubicash.ui.CashGraph
import com.hubidubicash.ui.CashModel
import com.hubidubicash.ui.HubidubiCashApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

fun main() {
    // The Mac's title bar follows its light or dark mode, as the page does.
    System.setProperty("apple.awt.application.appearance", "system")
    val dir = Platform.dataDir()
    val graph = CashGraph(desktopDatabase(File(dir, "hubidubicash.db")), Settings(File(dir, "settings.properties")),
        System.getProperty("hubidubicash.version") ?: "dev")
    // The model's own clock looks at the date every minute and adds any regular spending that falls due.
    val model = CashModel(graph, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    val icon = runCatching {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
            ?.use { org.jetbrains.skia.Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap() }
    }.getOrNull()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "HubidubiCash",
            icon = icon?.let(::BitmapPainter),
            state = rememberWindowState(size = DpSize(1080.dp, 780.dp)),
        ) {
            window.minimumSize = java.awt.Dimension(420, 560)
            HubidubiCashApp(model)
        }
    }
}
