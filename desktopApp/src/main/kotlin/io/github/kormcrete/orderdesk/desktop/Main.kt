package io.github.kormcrete.orderdesk.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.kormcrete.orderdesk.model.Seed
import io.github.kormcrete.orderdesk.ui.App
import io.github.kormcrete.orderdesk.ui.OrderDeskState
import java.awt.Dimension

private val isMac = System.getProperty("os.name").lowercase().startsWith("mac")

fun main() = application {
    val state = remember { OrderDeskState(Seed.store()) }
    val window = rememberWindowState(size = DpSize(1280.dp, 800.dp))
    Window(onCloseRequest = ::exitApplication, title = "Order Desk", state = window) {
        this.window.minimumSize = Dimension(400, 560)
        // On macOS this is the system menu bar; on Windows and Linux it sits in the window.
        MenuBar {
            Menu("Order") {
                Item("New order", shortcut = shortcut(Key.N), onClick = { state.pickingCustomer = true })
                Item("Orders", shortcut = shortcut(Key.One), onClick = { state.activeDraft = null })
            }
        }
        App(state)
        MemoryProbe.startIfRequested(state, ::exitApplication)
    }
}

private fun shortcut(key: Key) = if (isMac) KeyShortcut(key, meta = true) else KeyShortcut(key, ctrl = true)
