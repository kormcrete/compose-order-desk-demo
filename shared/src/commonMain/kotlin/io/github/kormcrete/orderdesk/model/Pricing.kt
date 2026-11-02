package io.github.kormcrete.orderdesk.model

/** A line that can be priced: a cart line or an order line. */
interface PricedLine {
    val name: String
    val price: Money
    val quantity: Int
}

/** How an order ships. Standard is free from [FREE_FROM]; Express goes to Germany and Austria only. */
enum class Shipping(val label: String, val timing: String, val price: Money) {
    STANDARD("Standard", "3–5 business days", Money.of("4.90")),
    EXPRESS("Express", "next business day", Money.of("14.90"));

    /** "Standard · 3–5 business days". */
    fun describe() = "$label · $timing"

    fun cost(discountedSubtotal: Money): Money =
        if (this == STANDARD && discountedSubtotal >= FREE_FROM) Money.ZERO else price

    fun allowedFor(country: Country) = this == STANDARD || country == Country.GERMANY || country == Country.AUSTRIA

    override fun toString() = label

    companion object {
        val FREE_FROM = Money.of("100.00")
    }
}

/** The money of a cart or an order, as a totals block shows it. */
data class Totals(val subtotal: Money, val discount: Money, val shipping: Money, val vat: Money) {
    val discounted: Money get() = subtotal - discount
    val total: Money get() = discounted + shipping + vat
}

/**
 * The price rules: 10 or more of one product take 5% off that line; VAT is 20% of the discounted subtotal plus
 * shipping, rounded once; everything rounds half up to the cent.
 */
object Pricing {
    const val VAT_PERCENT = 20
    const val VOLUME_QUANTITY = 10
    const val VOLUME_PERCENT = 5

    /** Quantity times price, before any discount. */
    fun lineTotal(line: PricedLine): Money = line.price * line.quantity

    fun lineDiscount(line: PricedLine): Money =
        if (line.quantity >= VOLUME_QUANTITY) lineTotal(line).percent(VOLUME_PERCENT) else Money.ZERO

    /** [shipping] is null before checkout: then there is no shipping and VAT is on the goods only. */
    fun of(lines: List<PricedLine>, shipping: Shipping?): Totals {
        var subtotal = Money.ZERO
        var discount = Money.ZERO
        for (line in lines) {
            subtotal += lineTotal(line)
            discount += lineDiscount(line)
        }
        val discounted = subtotal - discount
        val shippingCost = shipping?.cost(discounted) ?: Money.ZERO
        val vat = (discounted + shippingCost).percent(VAT_PERCENT)
        return Totals(subtotal, discount, shippingCost, vat)
    }

    /** How much more makes Standard shipping free, or null if it already is. */
    fun moreForFreeShipping(totals: Totals): Money? =
        if (totals.discounted >= Shipping.FREE_FROM) null else Shipping.FREE_FROM - totals.discounted
}
