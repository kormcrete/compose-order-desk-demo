package io.github.kormcrete.orderdesk.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import org.jetbrains.skia.Image

/**
 * The splash: a small window where the kormcrete k builds itself, as on the website's home page and in the Swing demo,
 * with the same picture and timing. Compose draws it, with what the app loads anyway: the JVM's own splash screen
 * (`-splash:`) shows sooner, but painting on it frame by frame doubled what the app uses for as long as it runs.
 */
internal object Splash {
    const val WIDTH = 500
    const val HEIGHT = 400

    /** The last move (the settle) ends at 1.55 s; the finished k then stays 0.3 s before the window opens. */
    const val MOTION_MS = 1550
    const val HOLD_MS = 300L

    /** The k's 64-unit grid: 200 px, its letter centred left to right and a little above the middle. */
    const val K_SCALE = 3.125f
    const val K_X = 144f
    const val K_Y = 90f

    private val NAVY = Color(0xFF1A4A6B)
    private val CYAN = Color(0xFF76D8FF)
    private val PIECE = Bezier(0.2, 0.7, 0.2, 1.0)
    private val POP = Bezier(0.3, 1.6, 0.5, 1.0)
    private val EASE_OUT = Bezier(0.0, 0.0, 0.58, 1.0)

    private val STEM = Path().apply { addRoundRect(RoundRect(Rect(10f, 4f, 22f, 56f), CornerRadius(3f))) }
    private val BLOCK = Path().apply { addRect(Rect(25f, 31f, 37f, 43f)) }
    private val BLOCK_CENTRE = Offset(31f, 37f)
    private val UPPER_ARM = polygon(33f, 31f, 45f, 18f, 56f, 18f, 40f, 35f)
    private val LOWER_ARM = polygon(33f, 43f, 47f, 56f, 58f, 56f, 40f, 39f)

    /** System.nanoTime() of the splash's first frame, 0 before it; the memory probe reports it. */
    @Volatile
    var firstFrameNanos = 0L
        private set

    /** Whether this run has a splash: not when taking a picture of the window, nor in development runs. */
    val wanted: Boolean
        get() = System.getProperty("orderdesk.screenshot") == null && System.getProperty("orderdesk.splash") != "off"

    /** Plays once, then calls [onDone]. */
    @Composable
    fun Window(onDone: () -> Unit) {
        val state = rememberWindowState(size = DpSize(WIDTH.dp, HEIGHT.dp), position = WindowPosition(Alignment.Center))
        Window(onCloseRequest = onDone, state = state, title = "Order Desk", undecorated = true, resizable = false) {
            var ms by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                // The first frame is on screen when the next one starts: that's when the k starts, so all of it is seen.
                withFrameMillis { }
                val start = withFrameMillis { it }
                firstFrameNanos = System.nanoTime()
                while (ms < MOTION_MS) ms = withFrameMillis { (it - start).toInt() }.coerceAtMost(MOTION_MS)
                delay(HOLD_MS)
                onDone()
            }
            Content(ms)
        }
    }

    /** The picture with the k as it is [ms] into the animation. */
    @Composable
    fun Content(ms: Int) {
        val picture = remember { picture() }
        Canvas(Modifier.fillMaxSize()) {
            drawImage(picture, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.High)
            k(ms)
        }
    }

    /** White with the website's hairline around it and its wordmark bottom-right. Made for the Swing demo. */
    private fun picture(): ImageBitmap {
        val bytes = requireNotNull(Splash::class.java.getResourceAsStream("/splash@2x.png")) { "no splash@2x.png" }.use { it.readBytes() }
        return Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }

    /**
     * Stem, block, upper arm, lower arm in the SVG's order (the arms cover the block's corners). The pieces move in grid
     * units, as the browser moves the SVG's parts, so they come in from outside the picture, fading in.
     */
    private fun DrawScope.k(ms: Int) {
        val unit = size.width / WIDTH * K_SCALE
        translate(K_X * size.width / WIDTH, K_Y * size.width / WIDTH + settle(ms) * unit) {
            scale(unit, unit, Offset.Zero) {
                piece(STEM, ms, 0, 0f, -90f)
                pop(ms, 1050)
                piece(UPPER_ARM, ms, 350, 70f, -40f)
                piece(LOWER_ARM, ms, 500, 70f, 40f)
            }
        }
    }

    /** build-come: from (dx, dy) and transparent to its place, 0.5 s from delayMs. */
    private fun DrawScope.piece(shape: Path, ms: Int, delayMs: Int, dx: Float, dy: Float) {
        val e = PIECE.at(progress(ms, delayMs, 500))
        val rest = 1 - e
        translate(rest * dx, rest * dy) { drawPath(shape, NAVY, alpha = e.coerceIn(0f, 1f)) }
    }

    /** build-pop: the cyan block grows from its centre with an overshoot, 0.4 s from delayMs. */
    private fun DrawScope.pop(ms: Int, delayMs: Int) {
        val e = POP.at(progress(ms, delayMs, 400))
        scale(e, e, BLOCK_CENTRE) { drawPath(BLOCK, CYAN, alpha = e.coerceIn(0f, 1f)) }
    }

    /** build-k-settle: the whole k dips 3 units and comes back, ease-out per keyframe (0, 40, 100 %), 0.35 s from 1.2 s. */
    private fun settle(ms: Int): Float {
        val p = progress(ms, 1200, 350)
        return if (p <= 0.4) 3 * EASE_OUT.at(p / 0.4) else 3 * (1 - EASE_OUT.at((p - 0.4) / 0.6))
    }

    private fun progress(ms: Int, delayMs: Int, durationMs: Int) = ((ms - delayMs) / durationMs.toDouble()).coerceIn(0.0, 1.0)

    private fun polygon(vararg xy: Float) = Path().apply {
        moveTo(xy[0], xy[1])
        for (i in 2 until xy.size step 2) lineTo(xy[i], xy[i + 1])
        close()
    }

    /** A CSS cubic-bezier timing function: y at the x given, the curve's parameter found by bisection. */
    private class Bezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
        fun at(x: Double): Float {
            if (x <= 0) return 0f
            if (x >= 1) return 1f
            var lo = 0.0
            var hi = 1.0
            var u = x
            repeat(40) {
                u = (lo + hi) / 2
                if (curve(u, x1, x2) < x) lo = u else hi = u
            }
            return curve(u, y1, y2).toFloat()
        }

        private fun curve(u: Double, p1: Double, p2: Double): Double {
            val v = 1 - u
            return 3 * v * v * u * p1 + 3 * v * u * u * p2 + u * u * u
        }
    }
}
