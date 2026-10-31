package com.hubidubicash.ui

import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.hubidubicash.shared.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

actual val PlexSans: FontFamily = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plex_sans_semibold, FontWeight.Bold),
)

@Composable
actual fun SystemBarsFollow(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        var context = view.context
        while (context is ContextWrapper && context !is Activity) context = context.baseContext
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

@Composable
actual fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

/** The answer the system's chooser gives back, waited for by whoever asked. */
private class Pending { var uri: CompletableDeferred<Uri?>? = null }

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    val creating = remember { Pending() }
    val opening = remember { Pending() }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        creating.uri?.complete(it)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { opening.uri?.complete(it) }
    return remember(context) {
        object : FilePicker {
            override suspend fun create(name: String): OutputStream? {
                val answer = CompletableDeferred<Uri?>().also { creating.uri = it }
                create.launch(name)
                val uri = answer.await() ?: return null
                return withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt") }
            }

            override suspend fun open(): InputStream? {
                val answer = CompletableDeferred<Uri?>().also { opening.uri = it }
                open.launch(arrayOf("*/*"))
                val uri = answer.await() ?: return null
                return withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
            }
        }
    }
}
