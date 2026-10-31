package com.hubidubicash.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import java.io.InputStream
import java.io.OutputStream

// The few things the phone and the computer do differently; everything else
// on screen is written once.

/** IBM Plex Sans, from each platform's own resources. */
expect val PlexSans: FontFamily

/** The phone's status and navigation bar icons, light on a dark theme and dark on a light one. */
@Composable
expect fun SystemBarsFollow(dark: Boolean)

/** The phone's Back gesture, while [enabled]; the computer has none. */
@Composable
expect fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit)

/** Where a backup goes to and comes from: the system's file chooser. */
interface FilePicker {
    /** A new file named [name], chosen by the person; null when they cancel. */
    suspend fun create(name: String): OutputStream?
    /** A file the person picks; null when they cancel. */
    suspend fun open(): InputStream?
}

@Composable
expect fun rememberFilePicker(): FilePicker
