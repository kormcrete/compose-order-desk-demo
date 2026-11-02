package io.github.kormcrete.orderdesk.model

/** The fields of the Address step, in form order. */
enum class AddressField { NAME, STREET, POSTAL_CODE, CITY, PHONE }

/** The card fields of the Payment step. */
enum class CardField { NUMBER, EXPIRY, NAME }

/** Checks a delivery address; the texts are what the Address step shows under each field. */
object AddressRules {
    private val PHONE = Regex("[+\\d ]{6,20}")

    /** The problem per field; empty when the address can be used. */
    fun validate(address: Address): Map<AddressField, String> = buildMap {
        if (address.name.isBlank()) put(AddressField.NAME, "Name is required")
        if (address.street.isBlank()) put(AddressField.STREET, "Street is required")
        val postalCode = address.postalCode.trim()
        if (postalCode.isEmpty()) put(AddressField.POSTAL_CODE, "Postal code is required")
        else if (!address.country.acceptsPostalCode(postalCode)) put(AddressField.POSTAL_CODE, address.country.postalCodeRule)
        if (address.city.isBlank()) put(AddressField.CITY, "City is required")
        val phone = address.phone.trim()
        if (phone.isNotEmpty() && !PHONE.matches(phone)) put(AddressField.PHONE, "Only +, digits and spaces, 6–20 characters")
    }
}

/** Card details as the Payment step checks and shows them. Any 16 digits are a card number: no checksum. */
object CardRules {
    private val EXPIRY = Regex("(\\d{2})/(\\d{2})")

    /** The number without the spaces and dashes people type; any other character stays, and fails the check. */
    fun digits(number: String) = number.replace(Regex("[\\s-]"), "")

    /** Up to 16 digits in groups of four: `4242 4242 4242 4242`. */
    fun format(raw: String): String = raw.filter { it.isDigit() }.take(16).chunked(4).joinToString(" ")

    /** `MM/YY` from what was typed: digits only, a slash after the month. */
    fun formatExpiry(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        return if (digits.length <= 2) digits else digits.take(2) + "/" + digits.drop(2)
    }

    /** The problem per field; empty when the card can be used. A card is valid through its expiry month. */
    fun validate(number: String, expiry: String, name: String, today: Day): Map<CardField, String> = buildMap {
        if (!Regex("\\d{16}").matches(digits(number))) put(CardField.NUMBER, "Enter the 16 digits of the card")
        val m = EXPIRY.matchEntire(expiry.trim())
        if (m == null) {
            put(CardField.EXPIRY, "Use MM/YY")
        } else {
            val month = m.groupValues[1].toInt()
            val year = 2000 + m.groupValues[2].toInt()
            if (month !in 1..12) put(CardField.EXPIRY, "Month must be 01–12")
            else if (year * 12 + month < today.year * 12 + today.month) put(CardField.EXPIRY, "This card has expired")
        }
        if (name.isBlank()) put(CardField.NAME, "Name on card is required")
    }
}

/** The plain-text receipt. */
object ReceiptText {
    fun of(order: Order): String = buildString {
        appendLine("Order Desk — receipt")
        appendLine("${order.number} · ${order.date.format()} · ${order.status.label}")
        appendLine()
        appendLine("Customer:  ${order.customer.name}")
        appendLine("Deliver to:")
        order.address.lines().forEach { appendLine("  $it") }
        appendLine("Shipping:  ${order.shipping.describe()}")
        if (order.note.isNotEmpty()) appendLine("Note:      ${order.note}")
        appendLine("Payment:   ${order.payment.describe()}")
        appendLine()
        for (l in order.lines) {
            appendLine(
                l.quantity.toString().padStart(4) + " × " + l.name.padEnd(32) + " " +
                    l.price.format().padStart(10) + " " + l.total.format().padStart(11)
            )
        }
        appendLine()
        val t = order.totals
        total("Subtotal", t.subtotal)
        if (t.discount.isPositive) total("Volume discount", -t.discount)
        total("Shipping", t.shipping)
        total("VAT (${Pricing.VAT_PERCENT}%)", t.vat)
        total("Total", t.total)
    }

    private fun StringBuilder.total(label: String, amount: Money) {
        appendLine(label.padEnd(50) + " " + amount.format().padStart(11))
    }
}
