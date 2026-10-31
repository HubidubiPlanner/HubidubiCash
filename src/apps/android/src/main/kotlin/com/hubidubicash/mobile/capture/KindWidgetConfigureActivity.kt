package com.hubidubicash.mobile.capture

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.mobile.cash
import com.hubidubicash.ui.ColorTheme
import com.hubidubicash.ui.HubidubiCashTheme
import com.hubidubicash.ui.KindSheet

/** Placing a kind widget: which kind is it for? The same list "+" shows. */
class KindWidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Until a kind is chosen, backing out leaves no widget behind.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val settings = cash.graph.settings
        I18n.language = settings.current.language
        setContent {
            HubidubiCashTheme(ColorTheme.of(settings.current.theme)) {
                KindSheet(showIncome = false, onIncome = {}, onKind = { kind ->
                    settings.setWidgetKind(id, kind)
                    KindWidgetProvider.update(this, AppWidgetManager.getInstance(this), id)
                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                    finish()
                }, onDismiss = ::finish)
            }
        }
    }
}
