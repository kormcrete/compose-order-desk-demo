package io.github.kormcrete.orderdesk.model

actual fun systemToday(): Day {
    // -Dorderdesk.today=2026-10-12 fixes the date, for screenshots and tests.
    System.getProperty("orderdesk.today")?.takeIf { it.isNotBlank() }?.let { return Day.parse(it) }
    val now = java.time.LocalDate.now()
    return Day(now.year, now.monthValue, now.dayOfMonth)
}
