package io.github.kormcrete.orderdesk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The countries Order Desk delivers to, with their postal code rule. */
enum class Country(val label: String, private val postalCode: Regex, val postalCodeRule: String) {
    GERMANY("Germany", Regex("\\d{5}"), "Germany uses a 5-digit postal code"),
    AUSTRIA("Austria", Regex("\\d{4}"), "Austria uses a 4-digit postal code"),
    NETHERLANDS("Netherlands", Regex("\\d{4} [A-Za-z]{2}"), "The Netherlands use the form 1234 AB"),
    FRANCE("France", Regex("\\d{5}"), "France uses a 5-digit postal code");

    /** Surrounding spaces do not count; letters may be either case. */
    fun acceptsPostalCode(code: String) = postalCode.matches(code.trim())

    override fun toString() = label
}

/** A delivery or contact address. Values are kept as typed; [AddressRules] checks them. */
data class Address(
    val name: String = "",
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val country: Country = Country.GERMANY,
    val phone: String = "",
) {
    /** As on a label: name, street, "10119 Berlin", country, then the phone if there is one. */
    fun lines(): List<String> = buildList {
        add(name.trim())
        add(street.trim())
        add("${postalCode.trim()} ${city.trim()}".trim())
        add(country.label)
        if (phone.isNotBlank()) add(phone.trim())
    }

    /** "Torstraße 118, 10119 Berlin": one line, for summaries. */
    fun oneLine(): String = listOf(street.trim(), "${postalCode.trim()} ${city.trim()}".trim()).filter { it.isNotEmpty() }.joinToString(", ")
}

/** Whether a customer may pay on invoice, up to which credit limit, and whether invoices need a PO number. */
data class PaymentTerms(val invoiceAllowed: Boolean, val creditLimit: Money, val poRequired: Boolean) {
    companion object {
        fun invoice(creditLimit: Money, poRequired: Boolean) = PaymentTerms(true, creditLimit, poRequired)
        fun cardOnly() = PaymentTerms(false, Money.ZERO, false)
    }
}

/** A customer of the shop. What they owe on open invoices changes as orders are placed and cancelled. */
class Customer(val name: String, val address: Address, val terms: PaymentTerms, owes: Money) {
    var owes: Money by mutableStateOf(owes)
        internal set

    /** Credit still open on account; zero for card-only customers. */
    val creditLeft: Money get() = if (terms.invoiceAllowed) terms.creditLimit - owes else Money.ZERO

    /** "AS" for Ada Shop Ltd: the avatar tile. */
    val initials: String
        get() = name.split(' ', '&').filter { it.isNotBlank() && it.first().isLetter() }
            .take(2).joinToString("") { it.first().uppercase() }

    /** The short facts shown with a customer: "Invoice · 30 days", "€1,550.00 credit left", "PO number required". */
    fun termTags(): List<String> =
        if (!terms.invoiceAllowed) listOf("Card only")
        else buildList {
            add("Invoice · 30 days")
            add("${creditLeft.format()} credit left")
            if (terms.poRequired) add("PO number required")
        }

    /** "Berlin · Account": the detail of a customer suggestion. */
    fun suggestionDetail() = address.city + " · " + if (terms.invoiceAllowed) "Account" else "Card only"

    /** Whether a search finds this customer: by name or city, any case. */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || q in name.lowercase() || q in address.city.lowercase()
    }

    override fun toString() = name
}
