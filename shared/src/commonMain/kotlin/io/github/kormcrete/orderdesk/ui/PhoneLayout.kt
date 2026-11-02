package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.AvatarTile
import io.github.kormcrete.lagoon.compose.Lagoon
import io.github.kormcrete.lagoon.compose.LagoonButton
import io.github.kormcrete.lagoon.compose.LagoonButtonStyle
import io.github.kormcrete.lagoon.compose.LagoonCard
import io.github.kormcrete.lagoon.compose.LagoonChip
import io.github.kormcrete.lagoon.compose.LagoonDivider
import io.github.kormcrete.lagoon.compose.LagoonIconButton
import io.github.kormcrete.lagoon.compose.LagoonIcons
import io.github.kormcrete.lagoon.compose.LagoonSearchField
import io.github.kormcrete.lagoon.compose.LagoonStepper
import io.github.kormcrete.lagoon.compose.LagoonTag
import io.github.kormcrete.lagoon.compose.LagoonTone
import io.github.kormcrete.lagoon.compose.SectionLabel
import io.github.kormcrete.lagoon.compose.SegmentedControl
import io.github.kormcrete.lagoon.compose.StepProgress
import io.github.kormcrete.orderdesk.model.OrderStatus
import io.github.kormcrete.orderdesk.model.Pricing

/** The phone layout: one screen at a time, from [OrderDeskState.backStack]. */
@Composable
fun PhoneLayout(state: OrderDeskState) {
    PlatformBackHandler(enabled = state.backStack.size > 1) { state.pop() }
    when (val screen = state.screen) {
        Screen.Orders -> PhoneOrders(state)
        is Screen.OrderDetail -> PhoneOrderDetail(state, screen.number)
        Screen.CustomerPicker -> PhoneCustomerPicker(state)
        Screen.Products -> state.activeDraft?.let { PhoneProducts(state, it) } ?: BackToOrders(state)
        Screen.Cart -> state.activeDraft?.let { PhoneCart(state, it) } ?: BackToOrders(state)
        Screen.Checkout -> state.checkout?.let { PhoneCheckout(state, it) } ?: BackToOrders(state)
    }
}

/** A screen whose draft is gone (placed elsewhere) returns to the orders list. */
@Composable
private fun BackToOrders(state: OrderDeskState) {
    LaunchedEffect(Unit) { state.popToOrders() }
}

/** Back (or close) on the left, a centred title, an optional action on the right. */
@Composable
private fun TopBar(title: String, subtitle: String? = null, close: Boolean = false, onBack: () -> Unit, action: @Composable () -> Unit = { Spacer(Modifier.size(44.dp)) }) {
    Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LagoonIconButton(if (close) LagoonIcons.Close else LagoonIcons.Back, if (close) "Close" else "Back", onBack)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = Lagoon.type.bodyStrong, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = Lagoon.colors.inkMuted)
        }
        action()
    }
}

// ---------------------------------------------------------------- orders

@Composable
private fun PhoneOrders(state: OrderDeskState) {
    state.revision
    val c = Lagoon.colors
    val orders = state.visibleOrders()
    val byMonth = orders.groupBy { "${it.date.monthName()} ${it.date.year}" }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(20.dp, 20.dp, 20.dp, 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Orders", style = Lagoon.type.title)
                    Text("${state.store.orders.openCount} open · ${state.store.orders.openTotal.format()} to ship", style = Lagoon.type.small, color = c.inkMuted)
                }
                LagoonIconButton(LagoonIcons.More, "More options", {})
            }
            Column(Modifier.padding(20.dp, 14.dp, 20.dp, 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LagoonSearchField(state.query, { state.query = it }, "Search orders or customers", Modifier.fillMaxWidth())
                StatusFilter(state, Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 104.dp)) {
                byMonth.forEach { (month, monthOrders) ->
                    item(key = month) { SectionLabel(month.substringBefore(' '), Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)) }
                    item(key = "$month-card") {
                        LagoonCard {
                            monthOrders.forEachIndexed { i, order ->
                                OrderRow(order, false, { state.selected = order.number; state.push(Screen.OrderDetail(order.number)) })
                                if (i < monthOrders.lastIndex) LagoonDivider()
                            }
                        }
                    }
                }
                if (orders.isEmpty()) item { EmptyNote("No orders match.") }
            }
        }
        LagoonButton(
            "New order", { state.push(Screen.CustomerPicker) },
            Modifier.align(Alignment.BottomEnd).padding(20.dp, 0.dp, 20.dp, 28.dp),
            icon = LagoonIcons.Plus, height = 56.dp,
        )
    }
}

@Composable
fun StatusFilter(state: OrderDeskState, modifier: Modifier = Modifier) {
    val statuses = listOf<OrderStatus?>(null, OrderStatus.PLACED, OrderStatus.SHIPPED, OrderStatus.CANCELLED)
    SegmentedControl(
        statuses.map { "${it?.label ?: "All"} ${state.count(it)}" },
        statuses.indexOf(state.statusFilter),
        { state.statusFilter = statuses[it] },
        modifier,
    )
}

@Composable
fun EmptyNote(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(32.dp), style = Lagoon.type.body, color = Lagoon.colors.inkMuted, textAlign = TextAlign.Center)
}

@Composable
private fun PhoneOrderDetail(state: OrderDeskState, number: String) {
    state.revision
    val order = state.store.orders.find(number) ?: return
    Column(Modifier.fillMaxSize()) {
        TopBar(order.number, onBack = { state.pop() }, action = { LagoonIconButton(LagoonIcons.More, "More options", {}) })
        OrderSummary(order, Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 16.dp))
        Row(Modifier.padding(16.dp, 12.dp, 16.dp, 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (order.cancellable) LagoonButton("Cancel order", { state.confirmCancel = order }, Modifier.weight(1f), style = LagoonButtonStyle.Danger)
            LagoonButton("Receipt", { state.receipt = order }, Modifier.weight(1f), style = LagoonButtonStyle.Tonal, icon = LagoonIcons.Receipt)
        }
    }
}

// ---------------------------------------------------------------- new order

@Composable
private fun PhoneCustomerPicker(state: OrderDeskState) {
    var query by remember { mutableStateOf("") }
    val changing = state.activeDraft != null && state.backStack.contains(Screen.Products)
    Column(Modifier.fillMaxSize()) {
        TopBar(if (changing) "Change customer" else "New order", close = true, onBack = { state.pop() })
        LagoonSearchField(query, { query = it }, "Search customers or cities", Modifier.fillMaxWidth().padding(16.dp, 4.dp, 16.dp, 8.dp))
        val recent = state.store.orders.recentCustomers(3)
        val all = state.store.findCustomers(query)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 24.dp)) {
            if (query.isBlank()) {
                SectionLabel("Recent", Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp))
                CustomerList(recent) { pick(state, it, changing) }
            }
            SectionLabel(if (query.isBlank()) "All customers" else "Matches", Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
            CustomerList(all) { pick(state, it, changing) }
            if (all.isEmpty()) EmptyNote("No customer matches.")
        }
    }
}

private fun pick(state: OrderDeskState, customer: io.github.kormcrete.orderdesk.model.Customer, changing: Boolean) {
    val draft = state.activeDraft
    if (changing && draft != null) {
        state.changeCustomer(draft, customer)
        state.pop()
    } else {
        state.startDraft(customer)
        state.pop()
        state.push(Screen.Products)
    }
}

@Composable
fun CustomerList(customers: List<io.github.kormcrete.orderdesk.model.Customer>, onPick: (io.github.kormcrete.orderdesk.model.Customer) -> Unit) {
    if (customers.isEmpty()) return
    LagoonCard {
        customers.forEachIndexed { i, customer ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(customer) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarTile(customer.initials, key = customer.name)
                Column(Modifier.weight(1f)) {
                    Text(customer.name, style = Lagoon.type.bodyStrong)
                    Text(customer.suggestionDetail(), style = Lagoon.type.small, color = Lagoon.colors.inkMuted)
                }
                Icon(LagoonIcons.Forward, null, Modifier.size(18.dp), tint = Lagoon.colors.inkFaint)
            }
            if (i < customers.lastIndex) LagoonDivider()
        }
    }
}

@Composable
private fun PhoneProducts(state: OrderDeskState, draft: Draft) {
    state.revision
    val catalog = state.store.catalog
    val products = catalog.search(draft.category, draft.productQuery)
    var searching by remember { mutableStateOf(draft.productQuery.isNotEmpty()) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("New order", close = true, onBack = { state.popToOrders() }, action = {
                LagoonIconButton(LagoonIcons.Search, "Search products", { searching = !searching })
            })
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { CustomerCard(draft.customer, { state.push(Screen.CustomerPicker) }) }
                if (searching) item {
                    LagoonSearchField(draft.productQuery, { draft.productQuery = it }, "Search products", Modifier.fillMaxWidth())
                }
                item { CategoryChips(state, draft) }
                item {
                    LagoonCard {
                        products.forEachIndexed { i, p ->
                            ProductRow(
                                p, draft.cart.quantityOf(p),
                                onAdd = { state.add(draft, p) },
                                onIncrement = { state.increment(draft, p) },
                                onDecrement = { state.decrement(draft, p) },
                            )
                            if (i < products.lastIndex) LagoonDivider()
                        }
                        if (products.isEmpty()) EmptyNote("No products match.")
                    }
                }
            }
        }
        if (!draft.cart.isEmpty) CartBar(draft, { state.push(Screen.Cart) }, Modifier.align(Alignment.BottomCenter).padding(16.dp, 0.dp, 16.dp, 24.dp))
    }
}

@Composable
private fun CategoryChips(state: OrderDeskState, draft: Draft) {
    val root = state.store.catalog.root
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LagoonChip("All", draft.category === root, { draft.category = root })
        root.children.forEach { top ->
            LagoonChip(top.label, draft.category === top || (draft.category in top && draft.category !== root), { draft.category = top }, icon = top.icon())
        }
    }
    val top = draft.category.top
    if (top !== root && top.children.isNotEmpty()) {
        Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            top.children.forEach { sub ->
                val on = draft.category === sub
                Text(
                    sub.label,
                    Modifier.clip(Lagoon.shapes.small).background(if (on) Lagoon.colors.primaryTint else Lagoon.colors.surface)
                        .clickable { draft.category = if (on) top else sub }.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = Lagoon.type.small.copy(fontWeight = Lagoon.type.label.fontWeight),
                    color = if (on) Lagoon.colors.primaryStrong else Lagoon.colors.inkMuted,
                )
            }
        }
    }
}

/** The dark floating bar: cart icon with units, products, total, View cart. */
@Composable
private fun CartBar(draft: Draft, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Lagoon.colors
    val shape = Lagoon.shapes.dialog
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(16.dp, shape, ambientColor = c.shadow.copy(alpha = 0.3f), spotColor = c.shadow.copy(alpha = 0.3f))
            .clip(shape)
            .background(c.night)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(LagoonIcons.Cart, null, Modifier.size(24.dp), tint = c.onNight)
            Text(
                draft.cart.units.toString(),
                Modifier.align(Alignment.TopEnd).offset(10.dp, (-8).dp).clip(CircleShape).background(c.nightAccent).padding(horizontal = 5.dp, vertical = 1.dp),
                style = Lagoon.type.label.copy(fontSize = Lagoon.metrics.labelSize * 0.9f),
                color = c.night,
            )
        }
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text("${draft.cart.lines.size} products", style = Lagoon.type.bodyStrong, color = c.onNight)
            Text("incl. VAT", style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.onNightMuted)
        }
        Row(
            Modifier.height(44.dp).clip(Lagoon.shapes.control).background(c.primary).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(draft.cart.totals().total.format(), style = Lagoon.type.bodyStrong, color = c.onPrimary)
            Icon(LagoonIcons.Forward, "View cart", Modifier.size(16.dp), tint = c.onPrimary)
        }
    }
}

@Composable
private fun PhoneCart(state: OrderDeskState, draft: Draft) {
    state.revision
    val c = Lagoon.colors
    Column(Modifier.fillMaxSize()) {
        TopBar("Cart", subtitle = draft.customer.name, onBack = { state.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            draft.cart.lines.forEach { line ->
                val p = line.product
                LagoonCard(padding = PaddingValues(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarTile(p.code(), size = 48.dp, colors = p.tileColors())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.padding(end = 6.dp)) {
                                Text(p.name, Modifier.weight(1f), style = Lagoon.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Pricing.lineTotal(line).format(), style = Lagoon.type.bodyStrong)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                LagoonStepper(line.quantity, { state.decrement(draft, p) }, { state.increment(draft, p) }, label = p.name, canIncrement = line.quantity < p.stock)
                                Text("${p.price.format()} each", Modifier.weight(1f), style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
                                if (line.discounted) LagoonTag("−5%", tone = LagoonTone.Success)
                                LagoonIconButton(LagoonIcons.Trash, "Remove ${p.name}", { state.remove(draft, p) }, lifted = false, tint = c.inkMuted, background = c.surface)
                            }
                            if (line.overStock) Text(p.onlyInStockMessage(), style = Lagoon.type.small, color = c.danger)
                        }
                    }
                }
            }
            if (draft.cart.isEmpty) EmptyNote("The cart is empty.")
            LagoonCard(padding = PaddingValues(16.dp)) { TotalsBlock(draft.cart.totals(), shippingText = "at checkout") }
            Text("10 or more of one product take 5% off that line.", Modifier.padding(horizontal = 4.dp), style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
        }
        LagoonButton(
            "Checkout", { state.startCheckout(draft); state.push(Screen.Checkout) },
            Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 24.dp),
            trailingIcon = LagoonIcons.ArrowRight, height = 56.dp,
            enabled = !draft.cart.isEmpty && draft.cart.stockProblems().isEmpty(),
        )
    }
}

@Composable
private fun PhoneCheckout(state: OrderDeskState, checkout: Checkout) {
    state.revision
    Column(Modifier.fillMaxSize()) {
        TopBar("Checkout", subtitle = "${checkout.draft.customer.name} · ${checkout.totals.total.format()}", onBack = { state.backCheckout(); if (state.checkout == null) state.pop() })
        Column(Modifier.padding(20.dp, 8.dp, 20.dp, 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepProgress(4, checkout.step)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Step ${checkout.step} of 4 · ${Checkout.STEP_TITLES[checkout.step - 1]}", style = Lagoon.type.label, color = Lagoon.colors.primaryStrong)
                Text(stepTitle(checkout.step), style = Lagoon.type.headline)
            }
        }
        CheckoutStepBody(state, checkout, Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp))
        Row(Modifier.padding(16.dp, 12.dp, 16.dp, 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LagoonButton("Back", { state.backCheckout(); if (state.checkout == null) state.pop() }, Modifier.weight(1f), style = LagoonButtonStyle.Outline, height = 56.dp)
            LagoonButton(
                if (checkout.step == 4) "Place order · ${checkout.totals.total.format()}" else "Continue",
                { if (state.continueCheckout() != null) state.popToOrders() },
                Modifier.weight(2f), height = 56.dp,
            )
        }
    }
}
