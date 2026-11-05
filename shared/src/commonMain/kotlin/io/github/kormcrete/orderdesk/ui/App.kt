package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.LagoonMetrics
import io.github.kormcrete.lagoon.compose.LagoonSurface
import io.github.kormcrete.lagoon.compose.LagoonTheme
import io.github.kormcrete.lagoon.compose.platformLagoonMetrics
import io.github.kormcrete.orderdesk.model.ReceiptText
import io.github.kormcrete.orderdesk.model.Seed
import kotlinx.coroutines.delay

/** Wide enough for the desktop layout: sidebar, list and detail side by side. Tablets in landscape get it too. */
val WIDE_LAYOUT = 840.dp

/** Order Desk on every platform: the phone layout on narrow windows, the desktop layout on wide ones. */
@Composable
fun App(
    scene: String? = null,
    state: OrderDeskState = remember { OrderDeskState(Seed.store()).also { Scenes.show(it, scene) } },
    metrics: LagoonMetrics = platformLagoonMetrics,
) {
    LagoonTheme(metrics) {
        LagoonSurface(Modifier.fillMaxSize().onPreviewKeyEvent { shortcut(state, it) }) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (maxWidth >= WIDE_LAYOUT) DesktopLayout(state) else PhoneLayout(state)

                state.confirmCancel?.let { order -> ConfirmCancel(order, { state.cancel(order) }, { state.confirmCancel = null }) }
                state.receipt?.let { order -> ReceiptView(ReceiptText.of(order), { state.receipt = null }) }
                if (state.showingAbout) AboutDialog { state.showingAbout = false }
                state.message?.let { text ->
                    MessageBar(text, { state.message = null }, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
                    LaunchedEffect(text) {
                        delay(3500)
                        state.message = null
                    }
                }
            }
        }
    }
}

/** Esc closes the top dialog; Ctrl+N (⌘N on macOS) starts a new order. */
private fun shortcut(state: OrderDeskState, event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when {
        event.key == Key.Escape -> when {
            state.showingAbout -> { state.showingAbout = false; true }
            state.receipt != null -> { state.receipt = null; true }
            state.confirmCancel != null -> { state.confirmCancel = null; true }
            state.checkout != null && state.backStack.last() != Screen.Checkout -> { state.checkout = null; true }
            state.pickingCustomer -> { state.pickingCustomer = false; true }
            else -> false
        }
        event.key == Key.N && (event.isCtrlPressed || event.isMetaPressed) -> { state.pickingCustomer = true; true }
        else -> false
    }
}
