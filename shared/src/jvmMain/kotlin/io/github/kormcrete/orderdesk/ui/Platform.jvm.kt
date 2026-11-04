package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.Lagoon

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}

@Composable
actual fun ScrollBar(state: ScrollState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = barStyle())

@Composable
actual fun ScrollBar(state: LazyListState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = barStyle())

@Composable
private fun barStyle() = ScrollbarStyle(
    minimalHeight = 32.dp,
    thickness = 6.dp,
    shape = RoundedCornerShape(3.dp),
    hoverDurationMillis = 150,
    unhoverColor = Lagoon.colors.inkMuted.copy(alpha = 0.35f),
    hoverColor = Lagoon.colors.inkMuted.copy(alpha = 0.7f),
)
