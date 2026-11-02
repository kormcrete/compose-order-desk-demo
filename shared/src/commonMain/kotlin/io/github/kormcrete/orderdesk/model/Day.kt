package io.github.kormcrete.orderdesk.model

/** A calendar date without time or zone: an order date, today. Formats as the app shows it: `9 Oct 2026`. */
data class Day(val year: Int, val month: Int, val day: Int) : Comparable<Day> {
    init {
        require(month in 1..12 && day in 1..31) { "Not a date: $year-$month-$day" }
    }

    override fun compareTo(other: Day) = compareValuesBy(this, other, Day::year, Day::month, Day::day)

    /** `9 Oct 2026`, whatever the machine's language. */
    fun format() = "$day ${MONTHS[month - 1]} $year"

    /** `9 Oct`, for lists. */
    fun formatShort() = "$day ${MONTHS[month - 1]}"

    /** `October`, for list sections. */
    fun monthName() = MONTH_NAMES[month - 1]

    override fun toString() = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    companion object {
        private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        private val MONTH_NAMES = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )

        /** From `2026-10-12`. */
        fun parse(text: String): Day {
            val parts = text.trim().split('-')
            require(parts.size == 3) { "A date like 2026-10-12, was '$text'" }
            return Day(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }
}

/** Today on this device. */
expect fun systemToday(): Day
