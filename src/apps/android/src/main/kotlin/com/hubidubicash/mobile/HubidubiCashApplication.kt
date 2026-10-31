package com.hubidubicash.mobile

import android.app.Application
import com.hubidubicash.core.Settings
import com.hubidubicash.data.androidDatabase
import com.hubidubicash.ui.CashGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** One database, one settings file and one repository for everything in the process: app, widgets, shortcut. */
class HubidubiCashApplication : Application() {
    /** Work that outlives the screen that asked for it: a spending saved from the home screen. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val graph: CashGraph by lazy {
        CashGraph(androidDatabase(this), Settings(File(filesDir, "settings.properties")), BuildConfig.VERSION_NAME)
    }
}

val android.content.Context.cash: HubidubiCashApplication get() = applicationContext as HubidubiCashApplication
