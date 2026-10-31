package com.hubidubicash.mobile.capture

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.hubidubicash.core.i18n.t
import com.hubidubicash.mobile.R

/**
 * "New spending" among the app icon's shortcuts (a long press on it), from
 * where it can be dragged out onto the home screen. Made by the app rather
 * than declared, so its name follows the language chosen in Settings.
 */
object SpendingShortcut {
    private const val ID = "new_spending"

    fun publish(context: Context) {
        val shortcut = ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel(t("New spending"))
            .setLongLabel(t("New spending"))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_spending))
            .setIntent(QuickSpendActivity.intent(context))
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }
}
