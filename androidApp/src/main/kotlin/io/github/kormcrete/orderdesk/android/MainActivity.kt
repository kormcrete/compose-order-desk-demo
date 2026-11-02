package io.github.kormcrete.orderdesk.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.kormcrete.orderdesk.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark icons on the light Lagoon ground; the app pads itself for the bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // `adb shell am start … --es scene new-order` opens a prepared scene (screenshots, demos).
        val scene = intent?.getStringExtra("scene")
        setContent { App(scene = scene) }
    }
}
