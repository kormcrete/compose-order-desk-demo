package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The system back gesture or button where the platform has one (Android); nothing elsewhere. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** A scrollbar on computers, whose scrolling isn't otherwise visible; touch screens show none. */
@Composable
expect fun ScrollBar(state: ScrollState, modifier: Modifier = Modifier)

/** A scrollbar on computers, whose scrolling isn't otherwise visible; touch screens show none. */
@Composable
expect fun ScrollBar(state: LazyListState, modifier: Modifier = Modifier)
