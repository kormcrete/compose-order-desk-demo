package io.github.kormcrete.orderdesk.model

/**
 * What the UI reads and changes. Every change tells the listeners, synchronously; the UI changes the model on its
 * main thread only, so listeners run there too.
 */
class Store(
    val catalog: Catalog,
    customers: List<Customer>,
    val orders: OrderBook,
    private val todayProvider: () -> Day,
) {
    val customers: List<Customer> = customers.toList()
    private val listeners = mutableListOf<() -> Unit>()

    val today: Day get() = todayProvider()

    /** By name or city, any case, sorted by name. */
    fun findCustomers(query: String): List<Customer> = customers.filter { it.matches(query) }.sortedBy { it.name }

    fun place(customer: Customer, cart: Cart, address: Address, shipping: Shipping, note: String, payment: Payment): Order {
        val order = orders.place(customer, cart, address, shipping, note, payment, today)
        changed()
        return order
    }

    /** Cancels all of them or, if one cannot be cancelled, none. */
    fun cancel(toCancel: Collection<Order>) {
        toCancel.firstOrNull { !it.cancellable }?.let { error("Only placed orders can be cancelled: ${it.number}") }
        for (order in toCancel) orders.cancel(order)
        changed()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    private fun changed() {
        for (listener in listeners.toList()) listener()
    }
}
