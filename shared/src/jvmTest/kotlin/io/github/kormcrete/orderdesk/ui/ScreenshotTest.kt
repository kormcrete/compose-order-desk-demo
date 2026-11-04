package io.github.kormcrete.orderdesk.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import io.github.kormcrete.lagoon.compose.LagoonMetrics
import io.github.kormcrete.orderdesk.model.Day
import io.github.kormcrete.orderdesk.model.Seed
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders the main screens headless into PNGs: `./gradlew :shared:jvmTest -Pscreenshots=<dir>`.
 * Phone screens use the mobile metrics at 390×844, desktop screens the desktop metrics at 1280×800.
 */
class ScreenshotTest {
    private val dir = System.getProperty("orderdesk.screenshots").orEmpty()

    private fun newState() = OrderDeskState(Seed.store { Day(2026, 10, 12) })

    private fun shoot(name: String, state: OrderDeskState, width: Int, height: Int, scale: Float, metrics: LagoonMetrics) {
        val scene = ImageComposeScene(
            width = (width * scale).toInt(),
            height = (height * scale).toInt(),
            density = Density(scale),
        ) { App(state = state, metrics = metrics) }
        try {
            // A few frames: fonts and layout settle before the picture is taken.
            var image = scene.render(0)
            for (t in 1..4) image = scene.render(t * 100_000_000L)
            val png = image.encodeToData(EncodedImageFormat.PNG) ?: error("Could not encode $name")
            File(dir).mkdirs()
            File(dir, "$name.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    private fun phone(name: String, state: OrderDeskState) = shoot(name, state, 390, 844, 2f, LagoonMetrics.Mobile)
    private fun desktop(name: String, state: OrderDeskState) = shoot(name, state, 1280, 800, 1.5f, LagoonMetrics.Desktop)

    /** The mockup's cart: 3 A4 paper, 10 gel pens, 5 notebooks, 2 coffee beans for Ada Shop Ltd. */
    private fun withDraft(state: OrderDeskState): Draft {
        val draft = state.startDraft(state.store.customers.first())
        mapOf("PAP-A4-500" to 3, "PEN-GEL-BLK" to 10, "PAP-NOTE-A5" to 5, "COF-BEANS-1K" to 2).forEach { (sku, qty) ->
            val p = state.store.catalog.find(sku)!!
            repeat(qty) { state.add(draft, p) }
        }
        return draft
    }

    @Test
    fun screens() {
        if (dir.isBlank()) return

        phone("phone-1-orders", newState())
        phone("phone-2-order", newState().apply { push(Screen.OrderDetail("ORD-1637")) })
        phone("phone-3-new-order", newState().apply { withDraft(this); push(Screen.Products) })
        phone("phone-4-cart", newState().apply { withDraft(this); push(Screen.Products); push(Screen.Cart) })
        phone("phone-5-shipping", newState().apply {
            startCheckout(withDraft(this)); checkout!!.step = 2; push(Screen.Products); push(Screen.Cart); push(Screen.Checkout)
        })
        phone("phone-6-review", newState().apply {
            startCheckout(withDraft(this)); checkout!!.poNumber = "PO-2026-140"; checkout!!.step = 4
            push(Screen.Products); push(Screen.Cart); push(Screen.Checkout)
        })

        desktop("desktop-1-orders", newState().apply { withDraft(this); activeDraft = null; selected = "ORD-1637" })
        desktop("desktop-2-new-order", newState().apply { withDraft(this) })
        desktop("desktop-3-checkout", newState().apply { startCheckout(withDraft(this)); checkout!!.step = 2 })
        // A small window (1000 wide): the compact desktop layout.
        shoot("desktop-small-1-orders", newState().apply { selected = "ORD-1637" }, 1000, 700, 1.5f, LagoonMetrics.Desktop)
        shoot("desktop-small-2-new-order", newState().apply { withDraft(this) }, 1000, 700, 1.5f, LagoonMetrics.Desktop)
        // A low window: the order on the right scrolls, and its scrollbar shows it.
        shoot("desktop-small-3-order-scrolls", newState().apply { selected = "ORD-1637" }, 1280, 560, 1.5f, LagoonMetrics.Desktop)
    }
}
