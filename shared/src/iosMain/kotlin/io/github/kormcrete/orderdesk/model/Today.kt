package io.github.kormcrete.orderdesk.model

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun systemToday(): Day {
    val c = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = NSDate())
    return Day(c.year.toInt(), c.month.toInt(), c.day.toInt())
}
