package io.github.kormcrete.orderdesk.model

import kotlin.math.abs

/** An amount in euros, held as whole cents. Formats the same on every machine: `€1,234.50`. */
data class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(factor: Int) = Money(cents * factor)
    operator fun unaryMinus() = Money(-cents)

    /** [percent] percent of this amount, rounded half up to a whole cent. */
    fun percent(percent: Int) = Money(floorDiv(cents * percent + 50, 100))

    val isZero get() = cents == 0L
    val isPositive get() = cents > 0

    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    /** `€1,234.50`; a negative amount starts with a minus sign: `−€0.60`. */
    fun format(): String {
        val a = abs(cents)
        val euros = (a / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        val rest = (a % 100).toString().padStart(2, '0')
        return (if (cents < 0) "−" else "") + "€" + euros + "." + rest
    }

    override fun toString() = format()

    companion object {
        val ZERO = Money(0)

        /** `Money.of("4.90")` is €4.90; a fraction of a cent is an error. */
        fun of(euros: String): Money {
            val negative = euros.startsWith("-")
            val parts = euros.removePrefix("-").split('.')
            require(parts.size <= 2 && parts[0].isNotEmpty()) { "Not an amount: $euros" }
            val fraction = parts.getOrElse(1) { "" }
            require(fraction.length <= 2 || fraction.substring(2).all { it == '0' }) { "A fraction of a cent: $euros" }
            val cents = parts[0].toLong() * 100 + fraction.take(2).padEnd(2, '0').toLong()
            return Money(if (negative) -cents else cents)
        }

        private fun floorDiv(a: Long, b: Long): Long {
            val q = a / b
            return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q
        }
    }
}
