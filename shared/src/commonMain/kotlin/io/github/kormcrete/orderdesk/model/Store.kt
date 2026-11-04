package io.github.kormcrete.orderdesk.model

/** What the UI reads and changes. What can change (stock, carts, orders, what a customer owes) is Compose state. */
class Store(
    val catalog: Catalog,
    customers: List<Customer>,
    val orders: OrderBook,
    private val todayProvider: () -> Day,
) {
    val customers: List<Customer> = customers.toList()

    val today: Day get() = todayProvider()

    /** By name or city, any case, sorted by name. */
    fun findCustomers(query: String): List<Customer> = customers.filter { it.matches(query) }.sortedBy { it.name }

    fun place(customer: Customer, cart: Cart, address: Address, shipping: Shipping, note: String, payment: Payment): Order {
        return orders.place(customer, cart, address, shipping, note, payment, today)
    }

    /** Cancels all of them or, if one cannot be cancelled, none. */
    fun cancel(toCancel: Collection<Order>) {
        toCancel.firstOrNull { !it.cancellable }?.let { error("Only placed orders can be cancelled: ${it.number}") }
        for (order in toCancel) orders.cancel(order)
    }
}
