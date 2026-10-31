package com.hubidubicash.core

import com.hubidubicash.core.i18n.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Currency
import java.util.Properties

/** How Statistics draws spending by kind. */
enum class Chart(val id: String) {
    BARS("bars"), CIRCLE("circle");

    companion object {
        fun of(id: String?): Chart = entries.firstOrNull { it.id == id } ?: BARS
    }
}

/** What Settings holds. None of it goes into a backup: it belongs to this device. */
data class SettingsState(
    val period: Period = Period.WEEK,
    val chart: Chart = Chart.BARS,
    /** Life costs a month, in hundredths: the Bills row that runs down as the month goes. */
    val lifeCosts: Long = DEFAULT_LIFE_COSTS,
    val currency: Currency = Money.defaultCurrency(),
    val language: Language = Language.system(),
    /** A colour theme's id; the screens know the themes. Null: the default. */
    val theme: String? = null,
) {
    companion object {
        const val DEFAULT_LIFE_COSTS = 5_000L * Money.SCALE
    }
}

/**
 * Settings, kept in one small properties file next to the database and read
 * once at start. Every change is written at once, through a temporary file
 * and a rename, so a crash never leaves half a file.
 */
class Settings(private val file: File) {
    private val props = Properties()
    private val _state: MutableStateFlow<SettingsState>
    val state: StateFlow<SettingsState>

    init {
        runCatching { if (file.exists()) file.inputStream().use(props::load) }
        _state = MutableStateFlow(read())
        state = _state.asStateFlow()
    }

    val current: SettingsState get() = _state.value

    private fun read() = SettingsState(
        period = Period.of(props.getProperty("period")),
        chart = Chart.of(props.getProperty("chart")),
        lifeCosts = props.getProperty("life_costs")?.toLongOrNull()?.takeIf { it in 0..Money.MAX }
            ?: SettingsState.DEFAULT_LIFE_COSTS,
        currency = props.getProperty("currency")?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
            ?: Money.defaultCurrency(),
        language = Language.of(props.getProperty("language")) ?: Language.system(),
        theme = props.getProperty("theme"),
    )

    @Synchronized
    private fun put(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.outputStream().use { props.store(it, "HubidubiCash settings") }
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        _state.value = read()
    }

    fun setPeriod(p: Period) = put("period", p.id)
    fun setChart(c: Chart) = put("chart", c.id)
    fun setLifeCosts(amount: Long) = put("life_costs", amount.coerceIn(0, Money.MAX).toString())
    fun setCurrency(c: Currency) = put("currency", c.currencyCode)
    fun setLanguage(l: Language) = put("language", l.code)
    fun setTheme(id: String) = put("theme", id)

    /** The kind a home-screen widget was set to when it was placed. */
    fun widgetKind(widgetId: Int): Kind? = props.getProperty("widget.$widgetId.kind")?.let(Kind::of)
    fun setWidgetKind(widgetId: Int, kind: Kind) = put("widget.$widgetId.kind", kind.id)
    fun forgetWidget(widgetId: Int) = put("widget.$widgetId.kind", null)
}
