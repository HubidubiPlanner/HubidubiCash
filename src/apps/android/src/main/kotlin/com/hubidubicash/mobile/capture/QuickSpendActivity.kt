package com.hubidubicash.mobile.capture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.hubidubicash.core.Kind
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import com.hubidubicash.mobile.cash
import com.hubidubicash.ui.AmountFor
import com.hubidubicash.ui.AmountSheet
import com.hubidubicash.ui.ColorTheme
import com.hubidubicash.ui.HubidubiCashTheme
import com.hubidubicash.ui.KindSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * A spending straight from the home screen. The widgets and the shortcut open
 * the app's own sheets over whatever is there: the kind (unless the widget is
 * for one kind), then the amount with the keyboard up. Saved, it says so, and
 * the app itself never opens.
 */
class QuickSpendActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = cash.graph
        I18n.language = graph.settings.current.language
        val kind = intent.getStringExtra(EXTRA_KIND)?.let(Kind::of)
        lifecycleScope.launch(Dispatchers.IO) { graph.repo.catchUp(LocalDate.now()) }
        setContent {
            val settings by graph.settings.state.collectAsState()
            HubidubiCashTheme(ColorTheme.of(settings.theme)) {
                var chosen by remember { mutableStateOf(kind) }
                val k = chosen
                if (k == null) {
                    KindSheet(showIncome = false, onIncome = {}, onKind = { chosen = it }, onDismiss = ::finish)
                } else {
                    AmountSheet(AmountFor.NewSpending(k, monthly = false), settings.currency, LocalDate.now(),
                        onSave = { a ->
                            val app = cash
                            app.scope.launch {
                                val today = LocalDate.now()
                                if (a.monthly) graph.repo.addRegular(false, a.kind, a.amount, a.note, a.dayOfMonth, today)
                                else graph.repo.addSpending(a.kind, a.amount, a.day, a.note)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(app, t("Spending saved"), Toast.LENGTH_SHORT).show()
                                }
                            }
                            finish()
                        },
                        onDelete = null, onDismiss = ::finish)
                }
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"

        fun intent(context: Context, kind: Kind? = null): Intent =
            Intent(context, QuickSpendActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .apply { kind?.let { putExtra(EXTRA_KIND, it.id) } }
    }
}
