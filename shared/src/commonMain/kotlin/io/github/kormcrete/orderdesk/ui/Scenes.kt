package io.github.kormcrete.orderdesk.ui

/**
 * Prepared states for screenshots and demos, picked by a launch option on each platform
 * (Android: intent extra `scene`; iOS: environment `ORDERDESK_SCENE`; desktop: `-Dorderdesk.scene`):
 * `orders`, `order`, `new-order`, `cart`, `checkout`, `review`.
 */
object Scenes {
    val names = listOf("orders", "order", "new-order", "cart", "checkout", "review")

    /** The cart of the mockups: 3 A4 paper, 10 gel pens, 5 notebooks, 2 coffee beans for Ada Shop Ltd. */
    fun demoDraft(state: OrderDeskState): Draft {
        val draft = state.startDraft(state.store.customers.first())
        mapOf("PAP-A4-500" to 3, "PEN-GEL-BLK" to 10, "PAP-NOTE-A5" to 5, "COF-BEANS-1K" to 2).forEach { (sku, qty) ->
            val product = state.store.catalog.find(sku) ?: return@forEach
            repeat(qty) { state.add(draft, product) }
        }
        return draft
    }

    /** Puts [state] into [scene]; an unknown or empty name leaves it on the orders list. */
    fun show(state: OrderDeskState, scene: String?) {
        when (scene?.trim()?.lowercase()) {
            "order" -> {
                state.selected = "ORD-1637"
                state.push(Screen.OrderDetail("ORD-1637"))
            }
            "new-order" -> {
                demoDraft(state)
                state.push(Screen.Products)
            }
            "cart" -> {
                demoDraft(state)
                state.push(Screen.Products)
                state.push(Screen.Cart)
            }
            "checkout", "review" -> {
                state.startCheckout(demoDraft(state))
                state.checkout?.let {
                    it.poNumber = "PO-2026-140"
                    it.step = if (scene == "review") 4 else 2
                }
                state.push(Screen.Products)
                state.push(Screen.Cart)
                state.push(Screen.Checkout)
            }
            else -> state.selected = "ORD-1637"
        }
    }
}
