package io.github.kormcrete.orderdesk.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.kormcrete.orderdesk.model.Seed
import io.github.kormcrete.orderdesk.ui.App
import io.github.kormcrete.orderdesk.ui.OrderDeskState
import io.github.kormcrete.orderdesk.ui.Scenes
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GraphicsEnvironment

private val isMac = System.getProperty("os.name").lowercase().startsWith("mac")

fun main() = application {
    // The window shows when the k has built itself; behind the splash, the app gets ready.
    var splash by remember { mutableStateOf(Splash.wanted) }
    if (splash) Splash.Window(onDone = { splash = false })
    // -Dorderdesk.scene=new-order opens a prepared scene (screenshots, demos).
    val state = remember { OrderDeskState(Seed.store()).also { Scenes.show(it, System.getProperty("orderdesk.scene")) } }
    // The About item macOS puts in the application menu shows the same dialog, not the JVM's default.
    LaunchedEffect(Unit) { if (isMac) Desktop.getDesktop().setAboutHandler { state.showingAbout = true } }
    val window = rememberWindowState(size = startSize(), position = WindowPosition(Alignment.Center))
    Window(onCloseRequest = ::exitApplication, title = "Order Desk", state = window, visible = !splash) {
        this.window.minimumSize = Dimension(400, 560)
        // On macOS this is the system menu bar; on Windows and Linux it sits in the window.
        MenuBar {
            Menu("Order") {
                Item("New order", shortcut = shortcut(Key.N), onClick = { state.pickingCustomer = true })
                Item("Orders", shortcut = shortcut(Key.One), onClick = { state.activeDraft = null })
            }
            Menu("Help") {
                Item("About Order Desk", onClick = { state.showingAbout = true })
            }
        }
        App(state = state)
        if (!splash) {
            MemoryProbe.startIfRequested(state, ::exitApplication)
            WindowShot.takeIfRequested(this.window, ::exitApplication)
        }
    }
}

/** 1280×800, or a little less than the screen when it is smaller. */
private fun startSize(): DpSize {
    val screen = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }.getOrNull()
    val width = minOf(1280, (screen?.width ?: 1280) - 40)
    val height = minOf(800, (screen?.height ?: 800) - 40)
    return DpSize(width.dp, height.dp)
}

private fun shortcut(key: Key) = if (isMac) KeyShortcut(key, meta = true) else KeyShortcut(key, ctrl = true)
