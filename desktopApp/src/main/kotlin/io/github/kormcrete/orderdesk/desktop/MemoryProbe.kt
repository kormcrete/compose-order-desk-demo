package io.github.kormcrete.orderdesk.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.kormcrete.orderdesk.ui.OrderDeskState
import kotlinx.coroutines.delay
import java.io.File
import java.lang.management.ManagementFactory

/** The process's memory as the operating system's own tools show it: ps on macOS and Linux, tasklist on Windows. */
private fun osResident(): String? = runCatching {
    val pid = ProcessHandle.current().pid().toString()
    val windows = System.getProperty("os.name").lowercase().startsWith("windows")
    val command = if (windows) listOf("tasklist", "/FI", "PID eq $pid", "/FO", "CSV", "/NH") else listOf("ps", "-o", "rss=", "-p", pid)
    val text = ProcessBuilder(command).redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
    if (windows) text.split("\",\"").last().trim('"') + " (working set)" else "${text.trim().toLong() / 1024} MB"
}.getOrNull()

/**
 * Started with `-Dorderdesk.probe=<file>`: walks through the app like a user (orders, an order, a new order with four
 * products, the checkout steps), settles, then writes what the process uses to the file and quits.
 * CI runs it on the packaged app on Linux, macOS and Windows.
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
        osResident()?.let { appendLine("resident (as the OS reports it): $it") }
    }
}
