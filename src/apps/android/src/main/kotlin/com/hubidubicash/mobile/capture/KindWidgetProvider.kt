package com.hubidubicash.mobile.capture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import com.hubidubicash.core.Kind
import com.hubidubicash.core.i18n.t
import com.hubidubicash.mobile.R
import com.hubidubicash.mobile.cash
import com.hubidubicash.ui.icon
import com.hubidubicash.ui.title

/**
 * A widget for one kind, chosen when it is placed: one tap goes straight to
 * the amount. It shows the kind's glyph as the app shows it, in the round,
 * dark look of the "New spending" widget.
 */
class KindWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val settings = context.cash.graph.settings
        appWidgetIds.forEach(settings::forgetWidget)
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val kind = context.cash.graph.settings.widgetKind(id) ?: return
            val views = RemoteViews(context.packageName, R.layout.widget_kind).apply {
                setImageViewBitmap(R.id.widget_kind_icon, roundIcon(context, kind))
                setContentDescription(R.id.widget_kind_icon, t("New spending: %s", kind.title))
                // Each widget its own request code, so each keeps its own kind.
                setOnClickPendingIntent(R.id.widget_kind, PendingIntent.getActivity(context, 1000 + id,
                    QuickSpendActivity.intent(context, kind),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.updateAppWidget(id, views)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, KindWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        /** A dark circle with the kind's Material glyph in light, as the widget list's other icons are drawn. */
        private fun roundIcon(context: Context, kind: Kind): Bitmap {
            val size = (61 * context.resources.displayMetrics.density).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0xFF1E1B19.toInt()
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            val vector = kind.icon
            val glyph = size * 0.46f
            canvas.save()
            canvas.translate((size - glyph) / 2, (size - glyph) / 2)
            canvas.scale(glyph / vector.viewportWidth, glyph / vector.viewportHeight)
            paint.color = 0xFFF7EFEC.toInt()
            draw(canvas, vector.root, paint)
            canvas.restore()
            return bitmap
        }

        private fun draw(canvas: Canvas, group: VectorGroup, paint: Paint) {
            for (node in group) when (node) {
                is VectorGroup -> draw(canvas, node, paint)
                is VectorPath -> {
                    val path = node.pathData.toPath().asAndroidPath()
                    path.fillType = if (node.pathFillType == PathFillType.EvenOdd)
                        android.graphics.Path.FillType.EVEN_ODD else android.graphics.Path.FillType.WINDING
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
