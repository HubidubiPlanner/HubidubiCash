package com.hubidubicash.mobile

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.hubidubicash.mobile.capture.SpendingShortcut
import com.hubidubicash.ui.CashModel
import com.hubidubicash.ui.HubidubiCashApp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The screens' state, kept across a turn of the phone or a change of language. */
class CashViewModel(app: Application) : AndroidViewModel(app) {
    val model = CashModel(app.cash.graph, viewModelScope)
}

class MainActivity : ComponentActivity() {
    private val vm: CashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HubidubiCashApp(vm.model) }
        // The shortcut's name follows the app's language.
        lifecycleScope.launch {
            cash.graph.settings.state.map { it.language }.distinctUntilChanged()
                .collect { SpendingShortcut.publish(this@MainActivity) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Back after a night away: today has moved, and a regular spending may be due.
        vm.model.checkDay()
    }
}
