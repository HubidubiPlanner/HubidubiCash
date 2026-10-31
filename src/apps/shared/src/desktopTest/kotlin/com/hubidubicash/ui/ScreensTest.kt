package com.hubidubicash.ui

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.hubidubicash.core.Chart
import com.hubidubicash.core.Kind
import com.hubidubicash.core.Settings
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.Language
import com.hubidubicash.data.inMemoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult

/**
 * The app on screen, phone-sized, with the spendings the design canvas shows.
 * With -Pshots=<folder> (REVIEW_SHOTS) each tab is also saved as a PNG there:
 * for the website, and to hold the app against the designs.
 */
@OptIn(ExperimentalTestApi::class)
class ScreensTest {
    private val today = LocalDate.of(2026, 10, 31)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val shots = System.getenv("REVIEW_SHOTS")?.let(::File)

    @AfterTest fun end() { scope.cancel(); I18n.language = Language.EN }

    private fun model(language: Language = Language.EN, chart: Chart = Chart.BARS, theme: String? = null): CashModel {
        val settings = Settings(File.createTempFile("settings", ".properties").also { it.delete(); it.deleteOnExit() })
        settings.setCurrency(Currency.getInstance("PLN"))
        settings.setLanguage(language)
        settings.setChart(chart)
        theme?.let(settings::setTheme)
        val graph = CashGraph(inMemoryDatabase(), settings, "test")
        val repo = graph.repo
        runBlocking {
            val since = LocalDate.of(2026, 10, 1)
            val salary = repo.addRegular(true, Kind.OTHER, 10_000_00, "Salary", 10, since)
            val garage = repo.addRegular(true, Kind.OTHER, 400_00, "Garage rented out", 15, since)
            val rent = repo.addRegular(false, Kind.HOME, 3_000_00, "Rent", 1, since)
            val bus = repo.addRegular(false, Kind.TRANSPORT, 48_00, "Bus pass", 1, since)
            val gym = repo.addRegular(false, Kind.HEALTH, 129_00, "Gym", 5, since)
            val power = repo.addRegular(false, Kind.UTILITIES, 180_00, "Electricity", 12, since)
            repo.addRegular(false, Kind.UTILITIES, 100_00, "Internet", 15, since)
            val phone = repo.addRegular(false, Kind.UTILITIES, 60_00, "Phone plan", 20, since)
            repo.addRegular(false, Kind.FUN, 49_00, "Streaming", 25, since)
            repo.catchUp(today)
            val october = YearMonth.of(2026, 10)
            listOf(salary, garage, rent, bus, gym, power, phone).forEach { repo.setPaid(it, october, true) }
            repo.addSpending(Kind.FOOD, 81_60, today, "Groceries")
            repo.addSpending(Kind.FOOD, 42_50, today, "Lunch")
            repo.addSpending(Kind.FUN, 64_00, today.minusDays(1), "Cinema, two tickets")
            repo.addSpending(Kind.TRANSPORT, 142_00, today.minusDays(1), "Fuel")
            repo.addSpending(Kind.HEALTH, 84_00, today.minusDays(3), "Pharmacy")
            repo.addSpending(Kind.SHOPPING, 183_00, today.minusDays(4), "Shoes")
            repo.addSpending(Kind.FOOD, 488_30, today.minusDays(5), "Big shop")
            repo.addSpending(Kind.FOOD, 2_098_00, today.minusDays(9), "Before the week")
        }
        return CashModel(graph, scope) { today }
    }

    /** Polish amounts are grouped and suffixed with no-break spaces. */
    private fun zl(s: String) = s.replace(" ", "\u00a0")

    /** A window [width] by [height] dp, drawn at twice that in pixels, as a phone draws it. */
    private fun ui(width: Int = 390, height: Int = 844, block: SkikoComposeUiTest.() -> Unit) =
        runSkikoComposeUiTest(Size(width * 2f, height * 2f), Density(2f), block = block)

    private fun SkikoComposeUiTest.show(model: CashModel, dark: Boolean = false) {
        setContent { HubidubiCashApp(model, systemDark = dark) }
        waitUntil(timeoutMillis = 10_000) { model.state.value != null }
        waitForIdle()
    }

    private fun SkikoComposeUiTest.shot(name: String) {
        val dir = shots ?: return
        dir.mkdirs()
        // The pointer off the page, so no hover shows in the picture.
        runCatching { onAllNodes(isRoot()).onFirst().performMouseInput { exit() } }
        waitForIdle()
        ImageIO.write(captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.tab(label: String): SemanticsNodeInteraction =
        onAllNodesWithText(label).onLast()

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onLast(): SemanticsNodeInteraction {
        val n = fetchSemanticsNodes().size
        return get(n - 1)
    }

    @Test
    fun `the app opens on Statistics with five tabs, and every tab shows`() = ui {
        show(model())
        onNodeWithText("SPENT").assertIsDisplayed()
        // The week's spendings, and Streaming, a regular one added on the 25th.
        onNodeWithText(zl("1 134,40 zł")).assertIsDisplayed()
        shot("statistics")
        for (t in listOf("Spendings", "Regular", "Bills", "Settings")) {
            tab(t).performClick()
            waitForIdle()
            shot(t.lowercase())
        }
        onNodeWithText("Life costs").assertIsDisplayed()
    }

    @Test
    fun `plus offers the eight kinds, and income first on Regular and Bills`() = ui {
        show(model())
        onNodeWithContentDescription("New spending").performClick()
        waitForIdle()
        Kind.entries.forEach { k -> assertTrue(onAllNodesWithText(k.label).fetchSemanticsNodes().isNotEmpty(), k.label) }
        assertTrue(onAllNodesWithText("Income").fetchSemanticsNodes().isEmpty())
        shot("plus")
        onAllNodesWithText("Food").onLast().performClick()
        waitForIdle()
        onNodeWithText("Amount").assertExists()
        onNodeWithText("Every month").assertExists()
        shot("amount")
    }

    @Test
    fun `Bills shows the month's lines and their total`() = ui {
        show(model())
        tab("Bills").performClick()
        waitForIdle()
        shot("bills")
        onNodeWithText("Total").assertExists()
        // October: 10 400 in, 3 566 out, 161 of life costs left on the 31st.
        onNodeWithText(zl("6 673,00 zł")).assertIsDisplayed()
        onNodeWithText("October 2026 · 2 still to tick").assertIsDisplayed()
    }

    @Test
    fun `the circle, in the dark, and the computer's rail`() = ui {
        show(model(chart = Chart.CIRCLE, theme = "night"))
        onNodeWithContentDescription("Spending by kind", substring = true).assertExists()
        shot("statistics-circle-night")
    }

    @Test
    fun `on a wide window the tabs sit on the left`() = ui(1080, 720) {
        show(model(chart = Chart.CIRCLE))
        assertTrue(onAllNodesWithText("Statistics").fetchSemanticsNodes().size == 2)
        shot("desktop-statistics")
        tab("Bills").performClick()
        waitForIdle()
        shot("desktop-bills")
    }

    @Test
    fun `the website's pictures, light and dark`() {
        for (dark in listOf(false, true)) {
            val suffix = if (dark) "-dark" else ""
            ui {
                show(model(), dark)
                shot("site-statistics$suffix")
                onNodeWithContentDescription("New spending").performClick()
                waitForIdle()
                shot("site-plus$suffix")
            }
            ui {
                show(model(), dark)
                tab("Bills").performSemanticsAction(SemanticsActions.OnClick)
                waitForIdle()
                shot("site-bills$suffix")
                tab("Spendings").performSemanticsAction(SemanticsActions.OnClick)
                waitForIdle()
                shot("site-spendings$suffix")
            }
            ui(1080, 720) {
                show(model(chart = Chart.CIRCLE), dark)
                shot("site-desktop$suffix")
            }
        }
    }

    @Test
    fun `the tab labels fit five across a 360dp phone in every language`() {
        for (l in Language.entries) ui(360, 720) {
            show(model(language = l))
            shot("tabs-${l.code}")
            for (label in Tab.entries.map { I18n.t(it.label) }) {
                // The label in the tab bar, along the bottom 80dp of the 720dp window.
                val node = onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes()
                    .single { it.boundsInRoot.top > (720 - 80) * 2 }
                val layout = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layout)
                val r = layout.first()
                val drawn = r.getLineRight(0) - r.getLineLeft(0)
                assertTrue(r.lineCount == 1 && drawn <= node.size.width + 1 && !r.isLineEllipsized(0),
                    "${l.label}: \"$label\" is ${drawn}px in a ${node.size.width}px label")
            }
            tab(I18n.t("Bills")).performClick()
            waitForIdle()
            shot("bills-${l.code}")
        }
    }
}
