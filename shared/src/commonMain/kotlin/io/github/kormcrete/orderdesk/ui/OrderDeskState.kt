package io.github.kormcrete.orderdesk.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kormcrete.orderdesk.model.Address
import io.github.kormcrete.orderdesk.model.AddressField
import io.github.kormcrete.orderdesk.model.AddressRules
import io.github.kormcrete.orderdesk.model.Cart
import io.github.kormcrete.orderdesk.model.CardField
import io.github.kormcrete.orderdesk.model.CardRules
import io.github.kormcrete.orderdesk.model.Category
import io.github.kormcrete.orderdesk.model.Country
import io.github.kormcrete.orderdesk.model.CreditCheck
import io.github.kormcrete.orderdesk.model.Customer
import io.github.kormcrete.orderdesk.model.Order
import io.github.kormcrete.orderdesk.model.OrderStatus
import io.github.kormcrete.orderdesk.model.Payment
import io.github.kormcrete.orderdesk.model.Product
import io.github.kormcrete.orderdesk.model.Shipping
import io.github.kormcrete.orderdesk.model.Store
import io.github.kormcrete.orderdesk.model.Totals

/** Phone screens, as a back stack. Desktop shows the same content side by side instead. */
sealed interface Screen {
    data object Orders : Screen
    data class OrderDetail(val number: String) : Screen
    data object CustomerPicker : Screen
    data object Products : Screen
    data object Cart : Screen
    data object Checkout : Screen
}

/** A new order being put together: the desktop's "Open drafts", the phone's New order flow. */
class Draft(customer: Customer, root: Category) {
    var customer by mutableStateOf(customer)
    val cart = Cart()
    var category by mutableStateOf(root)
    var productQuery by mutableStateOf("")
}

/** The checkout of a draft: four steps, each checked before the next. */
class Checkout(val draft: Draft) {
    var step by mutableIntStateOf(1)
    private val start = draft.customer.address

    var name by mutableStateOf(start.name)
    var street by mutableStateOf(start.street)
    var postalCode by mutableStateOf(start.postalCode)
    var city by mutableStateOf(start.city)
    var country by mutableStateOf(start.country)
    var phone by mutableStateOf(start.phone)

    var shipping by mutableStateOf(Shipping.STANDARD)
    var note by mutableStateOf("")

    var payByInvoice by mutableStateOf(draft.customer.terms.invoiceAllowed)
    var poNumber by mutableStateOf("")
    var cardNumber by mutableStateOf("")
    var cardExpiry by mutableStateOf("")
    var cardName by mutableStateOf("")

    /** Errors show once Continue was pressed on a step, not while typing the first time. */
    var showErrors by mutableStateOf(false)

    val address get() = Address(name, street, postalCode, city, country, phone)
    fun addressErrors(): Map<AddressField, String> = AddressRules.validate(address)
    fun cardErrors(today: io.github.kormcrete.orderdesk.model.Day): Map<CardField, String> =
        if (payByInvoice) emptyMap() else CardRules.validate(cardNumber, cardExpiry, cardName, today)
    fun poError(): String? =
        if (payByInvoice && draft.customer.terms.poRequired && poNumber.isBlank()) "PO number is required" else null

    val totals: Totals get() = draft.cart.totals(shipping)
    val payment: Payment get() = if (payByInvoice) Payment.invoice(poNumber) else Payment.card(cardNumber)

    /** Express goes to Germany and Austria only; a change of country falls back to Standard. */
    fun selectCountry(c: Country) {
        country = c
        if (!shipping.allowedFor(c)) shipping = Shipping.STANDARD
    }

    companion object {
        val STEP_TITLES = listOf("Address", "Shipping", "Payment", "Review")
    }
}

/** Everything the screens read and change. Model changes bump [revision], so composables reading it refresh. */
class OrderDeskState(val store: Store) {
    var revision by mutableIntStateOf(0)
        private set

    init {
        store.addListener { revision++ }
    }

    /** Tells the UI that the model changed outside the store (a cart). */
    fun touch() {
        revision++
    }

    // ---- orders
    var query by mutableStateOf("")
    var statusFilter by mutableStateOf<OrderStatus?>(null)
    var selected by mutableStateOf<String?>(null)

    fun visibleOrders(): List<Order> {
        val q = query.trim().lowercase()
        return store.orders.newestFirst().filter {
            (statusFilter == null || it.status == statusFilter) &&
                (q.isEmpty() || q in it.number.lowercase() || q in it.customer.name.lowercase())
        }
    }

    fun count(status: OrderStatus?) = store.orders.orders.count { status == null || it.status == status }

    // ---- drafts
    val drafts = mutableStateListOf<Draft>()
    var activeDraft by mutableStateOf<Draft?>(null)

    fun startDraft(customer: Customer): Draft {
        val draft = drafts.firstOrNull { it.customer === customer } ?: Draft(customer, store.catalog.root).also { drafts += it }
        activeDraft = draft
        return draft
    }

    fun changeCustomer(draft: Draft, customer: Customer) {
        draft.customer = customer
        touch()
    }

    fun discard(draft: Draft) {
        drafts -= draft
        if (activeDraft === draft) activeDraft = null
    }

    fun add(draft: Draft, product: Product) { draft.cart.add(product); touch() }
    fun increment(draft: Draft, product: Product) { draft.cart.add(product); touch() }
    fun decrement(draft: Draft, product: Product) { draft.cart.decrement(product); touch() }
    fun remove(draft: Draft, product: Product) { draft.cart.remove(product); touch() }

    // ---- checkout
    var checkout by mutableStateOf<Checkout?>(null)

    fun startCheckout(draft: Draft) {
        checkout = Checkout(draft)
    }

    /** Validates the current step and moves on; on the last step places the order. Returns the order when placed. */
    fun continueCheckout(): Order? {
        val c = checkout ?: return null
        val ok = when (c.step) {
            1 -> c.addressErrors().isEmpty()
            3 -> c.cardErrors(store.today).isEmpty() && c.poError() == null
            else -> true
        }
        if (!ok) {
            c.showErrors = true
            return null
        }
        c.showErrors = false
        if (c.step < 4) {
            c.step++
            return null
        }
        return try {
            val order = store.place(c.draft.customer, c.draft.cart, c.address, c.shipping, c.note, c.payment)
            discard(c.draft)
            checkout = null
            selected = order.number
            message = "${order.number} placed for ${order.customer.name}"
            order
        } catch (e: IllegalStateException) {
            message = e.message
            null
        }
    }

    fun backCheckout() {
        val c = checkout ?: return
        c.showErrors = false
        if (c.step > 1) c.step-- else checkout = null
    }

    fun creditWarning(c: Checkout): String? =
        if (c.payByInvoice && CreditCheck.exceeds(c.draft.customer, c.totals.total)) CreditCheck.message(c.draft.customer, c.totals.total) else null

    // ---- order actions
    var confirmCancel by mutableStateOf<Order?>(null)
    var receipt by mutableStateOf<Order?>(null)
    var message by mutableStateOf<String?>(null)

    fun cancel(order: Order) {
        store.cancel(listOf(order))
        confirmCancel = null
        message = "${order.number} cancelled"
    }

    // ---- phone navigation
    val backStack = mutableStateListOf<Screen>(Screen.Orders)
    val screen: Screen get() = backStack.last()

    fun push(screen: Screen) {
        backStack += screen
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        if (screen == Screen.Checkout) checkout = null
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** Back to the orders list, e.g. after placing an order. */
    fun popToOrders() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    // ---- desktop dialogs
    var pickingCustomer by mutableStateOf(false)
}
