package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}

@Composable
actual fun ScrollBar(state: ScrollState, modifier: Modifier) {
}

@Composable
actual fun ScrollBar(state: LazyListState, modifier: Modifier) {
}
