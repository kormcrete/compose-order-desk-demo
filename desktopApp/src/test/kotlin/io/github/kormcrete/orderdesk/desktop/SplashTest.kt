package io.github.kormcrete.orderdesk.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** The k on the splash, frame by frame; the checks of the Swing demo's SplashTest. */
class SplashTest {
    private val navy = 0xFF1A4A6B.toInt()
    private val cyan = 0xFF76D8FF.toInt()
    private val white = 0xFFFFFFFF.toInt()

    private fun paint(ms: Int): Bitmap {
        val scene = ImageComposeScene(Splash.WIDTH, Splash.HEIGHT, Density(1f)) { Splash.Content(ms) }
        try {
            return Bitmap.makeFromImage(scene.render())
        } finally {
            scene.close()
        }
    }

    /** The pixel at a point of the k's 64-unit grid. */
    private fun at(frame: Bitmap, gridX: Float, gridY: Float) =
        frame.getColor((Splash.K_X + gridX * Splash.K_SCALE).toInt(), (Splash.K_Y + gridY * Splash.K_SCALE).toInt())

    @Test
    fun theKIsNotThereYetAtTheStart() {
        val frame = paint(0)
        assertEquals(white, at(frame, 16f, 30f), "stem")
        assertEquals(white, at(frame, 46f, 22f), "upper arm")
        assertEquals(white, at(frame, 31f, 37f), "block")
    }

    @Test
    fun theWholeKAtTheEnd() {
        val frame = paint(Splash.MOTION_MS)
        assertEquals(navy, at(frame, 16f, 30f), "stem")
        assertEquals(navy, at(frame, 46f, 22f), "upper arm")
        assertEquals(navy, at(frame, 48f, 52f), "lower arm")
        assertEquals(cyan, at(frame, 31f, 37f), "block")
        assertEquals(white, at(frame, 23.5f, 37f), "the gap between stem and block")
        assertEquals(white, at(frame, 32f, 1f), "above the k")
    }

    @Test
    fun theLowerArmIsStillOnItsWayAt600Ms() = assertNotEquals(navy, at(paint(600), 48f, 52f))

    @Test
    fun theKDipsWhileItSettles() {
        assertEquals(white, at(paint(1300), 16f, 5f), "the stem's top is lower at 1.3 s")
        assertEquals(navy, at(paint(Splash.MOTION_MS), 16f, 5f))
    }
}
