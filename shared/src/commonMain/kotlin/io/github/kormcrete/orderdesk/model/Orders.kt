package io.github.kormcrete.orderdesk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How an order is paid: on invoice (with an optional PO number) or by card (only the last four digits are kept). */
class Payment private constructor(val isInvoice: Boolean, val poNumber: String, val cardLast4: String) {
    /** "Invoice (30 days) · PO PO-2026-131" or "Card ending 4242". */
    fun describe(): String =
        if (isInvoice) (if (poNumber.isEmpty()) "Invoice (30 days)" else "Invoice (30 days) · PO $poNumber")
        else "Card ending $cardLast4"

    companion object {
        fun invoice(poNumber: String = "") = Payment(true, poNumber.trim(), "")

        /** From the number as typed. */
        fun card(cardNumber: String): Payment {
            val digits = CardRules.digits(cardNumber)
            return Payment(false, "", if (digits.length <= 4) digits else digits.takeLast(4))
        }
    }
}

enum class OrderStatus(val label: String) {
    PLACED("Placed"), SHIPPED("Shipped"), CANCELLED("Cancelled");

    override fun toString() = label
}

/** A line of a placed order, with the price it was placed at. */
class OrderLine internal constructor(val product: Product, override val price: Money, override val quantity: Int) : PricedLine {
    override val name get() = product.name
    val sku get() = product.sku
    val total get() = Pricing.lineTotal(this)

    companion object {
        internal fun of(line: CartLine) = OrderLine(line.product, line.price, line.quantity)
    }
}

/** A placed order. Everything but its status is fixed when it is placed. */
class Order internal constructor(
    /** "ORD-1640". */
    val number: String,
    val date: Day,
    val customer: Customer,
    lines: List<OrderLine>,
    /** Where it is delivered. */
    val address: Address,
    val shipping: Shipping,
    val note: String,
    val payment: Payment,
    status: OrderStatus,
) {
    val lines: List<OrderLine> = lines.toList()
    val totals: Totals = Pricing.of(this.lines, shipping)

    var status: OrderStatus by mutableStateOf(status)
        internal set

    /** The number of units: "7 items". */
    val units get() = lines.sumOf { it.quantity }

    val cancellable get() = status == OrderStatus.PLACED
}

/** All orders, the next order number, and what placing and cancelling do to stock and credit. */
class OrderBook internal constructor(private val catalog: Catalog, private var nextNumberValue: Int) {
    private val _orders = mutableStateListOf<Order>()
    val orders: List<Order> get() = _orders

    /** An order from before the app started (seed data): no effect on stock or credit. */
    internal fun addExisting(order: Order) {
        _orders += order
    }

    fun find(number: String): Order? = _orders.firstOrNull { it.number == number }

    /** The number the next placed order gets. */
    val nextNumber get() = "ORD-$nextNumberValue"

    /** Checks the rules, takes the stock, adds an invoice total to what the customer owes. All or nothing. */
    fun place(customer: Customer, cart: Cart, address: Address, shipping: Shipping, note: String, payment: Payment, date: Day): Order {
        check(!cart.isEmpty) { "The cart is empty" }
        cart.stockProblems().firstOrNull()?.let { throw IllegalStateException(it) }
        check(shipping.allowedFor(address.country)) { "Express delivers to Germany and Austria only" }
        check(!payment.isInvoice || customer.terms.invoiceAllowed) { "${customer.name} pays by card" }
        val lines = cart.lines.map { OrderLine.of(it) }
        for (line in lines) catalog.take(line.product, line.quantity)
        val order = Order(nextNumber, date, customer, lines, address, shipping, note.trim(), payment, OrderStatus.PLACED)
        nextNumberValue++
        if (payment.isInvoice) customer.owes = customer.owes + order.totals.total
        _orders += order
        return order
    }

    /** Gives the stock back and takes an invoice total off what the customer owes. */
    fun cancel(order: Order) {
        check(order.cancellable) { "Only placed orders can be cancelled: ${order.number}" }
        for (line in order.lines) catalog.giveBack(line.product, line.quantity)
        if (order.payment.isInvoice) order.customer.owes = order.customer.owes - order.totals.total
        order.status = OrderStatus.CANCELLED
    }

    /** Placed orders: "4 open". */
    val openCount get() = _orders.count { it.status == OrderStatus.PLACED }

    /** What the open orders add up to: "€533.64 to ship". */
    val openTotal get() = _orders.filter { it.status == OrderStatus.PLACED }.fold(Money.ZERO) { sum, o -> sum + o.totals.total }

    /** Newest first, by date then number. */
    fun newestFirst(): List<Order> =
        _orders.sortedWith(compareByDescending<Order> { it.date }.thenByDescending { it.number })

    /** Customers by their latest order, newest first: the customer picker's "Recent". */
    fun recentCustomers(max: Int): List<Customer> =
        newestFirst().map { it.customer }.distinct().take(max)
}

/** The warning before an invoice order that takes a customer over their credit limit. */
object CreditCheck {
    fun exceeds(customer: Customer, orderTotal: Money): Boolean =
        customer.terms.invoiceAllowed && customer.owes + orderTotal > customer.terms.creditLimit

    /** What is left of the credit limit after this order; negative when it exceeds it. */
    fun leftAfter(customer: Customer, orderTotal: Money): Money = customer.terms.creditLimit - customer.owes - orderTotal

    /** "This order takes Birch & Co to €1,068.40 of its €1,000.00 credit limit." */
    fun message(customer: Customer, orderTotal: Money): String =
        "This order takes ${customer.name} to ${(customer.owes + orderTotal).format()} of its " +
            "${customer.terms.creditLimit.format()} credit limit."

    /** "Owes €940.00 now, this order adds €128.40." */
    fun detail(customer: Customer, orderTotal: Money): String =
        "Owes ${customer.owes.format()} now, this order adds ${orderTotal.format()}."
}
