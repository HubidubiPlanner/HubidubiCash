package com.hubidubicash.mobile.capture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.hubidubicash.mobile.R

/** The "New spending" widget: one tap asks the kind, then the amount. */
class NewSpendingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = PendingIntent.getActivity(context, 0, QuickSpendActivity.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val views = RemoteViews(context.packageName, R.layout.widget_spending).apply {
            setOnClickPendingIntent(R.id.widget_spending, pending)
        }
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }
}
