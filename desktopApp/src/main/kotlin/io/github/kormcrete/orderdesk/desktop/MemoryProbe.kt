package io.github.kormcrete.orderdesk.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.kormcrete.orderdesk.ui.OrderDeskState
import kotlinx.coroutines.delay
import java.io.File
import java.lang.management.ManagementFactory
import javax.management.ObjectName

/**
 * Started with `-Dorderdesk.probe=<file>`: walks through the app like a user (orders, an order, a new order with four
 * products, the checkout steps), settles, then writes what the process uses to the file and quits.
 * Add `-XX:NativeMemoryTracking=summary` for the JVM's own breakdown. CI runs it on the packaged app.
 */
object MemoryProbe {
    @Composable
    fun startIfRequested(state: OrderDeskState, exit: () -> Unit) {
        val target = System.getProperty("orderdesk.probe")?.takeIf { it.isNotBlank() } ?: return
        LaunchedEffect(Unit) {
            val started = ManagementFactory.getRuntimeMXBean().uptime
            delay(1500)
            state.selected = "ORD-1637"
            delay(400)
            val customer = state.store.customers.first()
            val draft = state.startDraft(customer)
            delay(400)
            for (sku in listOf("PAP-A4-500", "PEN-GEL-BLK", "PAP-NOTE-A5", "COF-BEANS-1K")) {
                val product = state.store.catalog.find(sku) ?: continue
                repeat(if (sku == "PEN-GEL-BLK") 10 else 2) { state.add(draft, product) }
                delay(150)
            }
            state.startCheckout(draft)
            delay(300)
            state.continueCheckout() // address
            delay(300)
            state.continueCheckout() // shipping
            delay(300)
            state.checkout?.poNumber = "PO-PROBE"
            state.continueCheckout() // payment
            delay(600) // review
            state.checkout = null
            state.activeDraft = null
            delay(800)
            System.gc()
            delay(1500)
            File(target).writeText(report(started))
            exit()
        }
    }

    fun report(startupMillis: Long): String = buildString {
        val rt = Runtime.getRuntime()
        val mem = ManagementFactory.getMemoryMXBean()
        val mb = 1024 * 1024
        appendLine("java ${System.getProperty("java.version")} (${System.getProperty("java.vm.name")}) on ${System.getProperty("os.name")}")
        appendLine("jvm args: ${ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ")}")
        appendLine("first frame + walk-through started after: ${startupMillis} ms uptime")
        appendLine("heap used: ${(rt.totalMemory() - rt.freeMemory()) / mb} MB, committed: ${rt.totalMemory() / mb} MB, max: ${rt.maxMemory() / mb} MB")
        appendLine("non-heap used: ${mem.nonHeapMemoryUsage.used / mb} MB, committed: ${mem.nonHeapMemoryUsage.committed / mb} MB")
        appendLine("loaded classes: ${ManagementFactory.getClassLoadingMXBean().loadedClassCount}, threads: ${ManagementFactory.getThreadMXBean().threadCount}")
        val status = File("/proc/self/status")
        if (status.exists()) {
            status.readLines().filter { it.startsWith("VmRSS") || it.startsWith("VmHWM") || it.startsWith("RssAnon") || it.startsWith("RssFile") }
                .forEach { appendLine(it.replace(Regex("\\s+"), " ")) }
        }
        val nmt = runCatching {
            ManagementFactory.getPlatformMBeanServer().invoke(
                ObjectName("com.sun.management:type=DiagnosticCommand"), "vmNativeMemory",
                arrayOf<Any>(arrayOf("summary", "scale=MB")), arrayOf(Array<String>::class.java.name),
            ) as String
        }.getOrNull()
        if (nmt != null && "not enabled" !in nmt) {
            appendLine()
            appendLine("native memory tracking (MB):")
            nmt.lines().filter { it.trim().startsWith("Total") || it.trim().startsWith("-") }.take(30).forEach { appendLine(it.trimEnd()) }
        }
    }
}
