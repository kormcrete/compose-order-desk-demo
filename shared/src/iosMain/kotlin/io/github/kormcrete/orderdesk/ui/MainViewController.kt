package io.github.kormcrete.orderdesk.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo

/** The iOS entry point, called from the Swift app. `ORDERDESK_SCENE` in the environment opens a prepared scene. */
fun MainViewController() = ComposeUIViewController {
    App(scene = NSProcessInfo.processInfo.environment["ORDERDESK_SCENE"] as? String)
}
