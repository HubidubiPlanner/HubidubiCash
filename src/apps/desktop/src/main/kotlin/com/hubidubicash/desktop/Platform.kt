package com.hubidubicash.desktop

import java.io.File

/** The few things that differ between the Mac and Linux builds of the desktop app. */
object Platform {
    private val os = System.getProperty("os.name").lowercase()
    val isMac = os.contains("mac")

    /** Where the app keeps its database and settings, as each system expects. */
    fun dataDir(home: String = System.getProperty("user.home"), xdg: String? = System.getenv("XDG_DATA_HOME"),
                mac: Boolean = isMac): File =
        if (mac) File(home, "Library/Application Support/HubidubiCash")
        else File(xdg?.takeIf { it.isNotBlank() } ?: "$home/.local/share", "hubidubicash")
}
