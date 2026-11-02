package io.github.kormcrete.orderdesk.ui

import androidx.compose.runtime.Composable

/** The system back gesture or button where the platform has one (Android); nothing elsewhere. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
