package io.github.kormcrete.orderdesk.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Started with `-Dorderdesk.screenshot=<file.png>`: once the window has settled, captures it from the screen,
 * title bar included, at the display's full resolution, writes the PNG and quits. CI takes the README pictures so.
 */
object WindowShot {
    @Composable
    fun takeIfRequested(window: java.awt.Window, exit: () -> Unit) {
        val target = System.getProperty("orderdesk.screenshot")?.takeIf { it.isNotBlank() } ?: return
        LaunchedEffect(Unit) {
            delay(4000)
            // Compose runs this on Swing's event thread: the window can be raised directly.
            window.toFront()
            delay(800)
            val bounds = Rectangle(window.locationOnScreen, window.size)
            // The largest variant is the one in physical pixels on HiDPI displays.
            val shot = Robot().createMultiResolutionScreenCapture(bounds).resolutionVariants
                .maxBy { it.getWidth(null) * it.getHeight(null) }
            val image = BufferedImage(shot.getWidth(null), shot.getHeight(null), BufferedImage.TYPE_INT_RGB)
            image.createGraphics().apply { drawImage(shot, 0, 0, null); dispose() }
            File(target).absoluteFile.parentFile.mkdirs()
            ImageIO.write(image, "png", File(target))
            exit()
        }
    }
}
