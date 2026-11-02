package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.AvatarTile
import io.github.kormcrete.lagoon.compose.Lagoon
import io.github.kormcrete.lagoon.compose.LagoonButton
import io.github.kormcrete.lagoon.compose.LagoonButtonStyle
import io.github.kormcrete.lagoon.compose.LagoonCard
import io.github.kormcrete.lagoon.compose.LagoonDivider
import io.github.kormcrete.lagoon.compose.LagoonIconButton
import io.github.kormcrete.lagoon.compose.LagoonIcons
import io.github.kormcrete.lagoon.compose.LagoonSearchField
import io.github.kormcrete.lagoon.compose.LagoonStepper
import io.github.kormcrete.lagoon.compose.LagoonTag
import io.github.kormcrete.lagoon.compose.LagoonTone
import io.github.kormcrete.lagoon.compose.SectionLabel
import io.github.kormcrete.lagoon.compose.StatusPill
import io.github.kormcrete.orderdesk.model.Category
import io.github.kormcrete.orderdesk.model.Order
import io.github.kormcrete.orderdesk.model.Pricing

/** The desktop layout: sidebar, then the orders workspace or a draft's workspace; checkout as a dialog. */
@Composable
fun DesktopLayout(state: OrderDeskState) {
    state.revision
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(state)
            val draft = state.activeDraft
            if (draft != null) DraftWorkspace(state, draft) else OrdersWorkspace(state)
        }
        if (state.pickingCustomer) CustomerPickerDialog(state)
        state.checkout?.let { CheckoutDialog(state, it) }
    }
}

// ---------------------------------------------------------------- sidebar

@Composable
private fun Sidebar(state: OrderDeskState) {
    val c = Lagoon.colors
    Column(
        Modifier.width(232.dp).fillMaxHeight().background(c.surface).padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.padding(start = 6.dp, top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(Lagoon.shapes.small).background(c.primary), contentAlignment = Alignment.Center) {
                Icon(LagoonIcons.Waves, null, Modifier.size(18.dp), tint = c.onPrimary)
            }
            Text("Order Desk", style = Lagoon.type.bodyStrong)
        }
        LagoonButton("New order", { state.pickingCustomer = true }, Modifier.fillMaxWidth().padding(bottom = 12.dp), icon = LagoonIcons.Plus)
        NavItem(state.activeDraft == null, { state.activeDraft = null }) {
            Icon(LagoonIcons.List, null, Modifier.size(18.dp), tint = LocalNavInk.current)
            Text("Orders", Modifier.weight(1f), style = Lagoon.type.bodyStrong, color = LocalNavInk.current)
            Text(state.store.orders.orders.size.toString(), style = Lagoon.type.label, color = LocalNavInk.current)
        }
        if (state.drafts.isNotEmpty()) {
            SectionLabel("Open drafts", Modifier.padding(start = 10.dp, top = 18.dp, bottom = 6.dp))
            state.drafts.forEach { draft ->
                NavItem(state.activeDraft === draft, { state.activeDraft = draft }) {
                    AvatarTile(draft.customer.initials, key = draft.customer.name, size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(draft.customer.name, style = Lagoon.type.bodyStrong, color = LocalNavInk.current, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${draft.cart.totals().total.format()} · ${draft.cart.units} items",
                            style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Ready · ${state.store.orders.openCount} open orders", Modifier.padding(horizontal = 10.dp), style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
    }
}

private val LocalNavInk = compositionLocalOf { Color.Unspecified }

@Composable
private fun NavItem(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val c = Lagoon.colors
    CompositionLocalProvider(LocalNavInk provides if (selected) c.primaryStrong else c.ink) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(Lagoon.shapes.small)
                .background(if (selected) c.primaryTint else Color.Transparent)
                .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// ---------------------------------------------------------------- orders

@Composable
private fun RowScope.OrdersWorkspace(state: OrderDeskState) {
    val c = Lagoon.colors
    val orders = state.visibleOrders()
    val selected = orders.firstOrNull { it.number == state.selected } ?: orders.firstOrNull()
    Column(Modifier.weight(1f).fillMaxHeight().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Orders", style = Lagoon.type.title)
                Text("${state.store.orders.openCount} open · ${state.store.orders.openTotal.format()} to ship", style = Lagoon.type.small, color = c.inkMuted)
            }
            LagoonSearchField(state.query, { state.query = it }, "Search orders or customers", Modifier.width(280.dp))
            StatusFilter(state)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OrdersTable(state, orders, selected, Modifier.weight(1f).fillMaxHeight())
            if (selected != null) {
                Column(Modifier.width(380.dp).fillMaxHeight()) {
                    OrderSummary(selected, Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp), dense = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LagoonButton("Save receipt", { state.receipt = selected }, Modifier.weight(1f), style = LagoonButtonStyle.Tonal, icon = LagoonIcons.Receipt)
                        if (selected.cancellable) LagoonButton("Cancel order", { state.confirmCancel = selected }, Modifier.weight(1f), style = LagoonButtonStyle.Danger)
                    }
                }
            }
        }
    }
}

private val ORDER_COLUMNS: List<Pair<String, Dp?>> = listOf(
    "Order" to 96.dp, "Date" to 72.dp, "Customer" to null, "Items" to 56.dp, "Total" to 92.dp, "Status" to 112.dp,
)

@Composable
private fun OrdersTable(state: OrderDeskState, orders: List<Order>, selected: Order?, modifier: Modifier) {
    val c = Lagoon.colors
    LagoonCard(modifier) {
        TableHeader(ORDER_COLUMNS, endAligned = setOf("Items", "Total"))
        LazyColumn(Modifier.weight(1f)) {
            items(orders, key = { it.number }) { order ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Lagoon.metrics.rowHeight)
                        .background(if (order === selected) c.selection else c.surface)
                        .clickable { state.selected = order.number }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cell(96.dp) { Text(order.number, style = Lagoon.type.bodyStrong) }
                    Cell(72.dp) { Text(order.date.formatShort(), style = Lagoon.type.body, color = c.inkMuted) }
                    Row(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarTile(order.customer.initials, key = order.customer.name, size = 28.dp)
                        Text(order.customer.name, style = Lagoon.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Cell(56.dp, end = true) { Text(order.units.toString(), style = Lagoon.type.body, color = c.inkMuted) }
                    Cell(92.dp, end = true) { Text(order.totals.total.format(), style = Lagoon.type.bodyStrong) }
                    Cell(112.dp) { StatusPill(order.status.label, order.status.tone()) }
                }
                LagoonDivider()
            }
            if (orders.isEmpty()) item { EmptyNote("No orders match.") }
        }
    }
}

@Composable
private fun TableHeader(columns: List<Pair<String, Dp?>>, endAligned: Set<String> = emptySet()) {
    Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        columns.forEach { (title, width) ->
            val m = if (width == null) Modifier.weight(1f) else Modifier.width(width)
            Text(
                title, m.padding(horizontal = 8.dp),
                style = Lagoon.type.label, color = Lagoon.colors.inkMuted,
                textAlign = if (title in endAligned) TextAlign.End else TextAlign.Start,
                maxLines = 1,
            )
        }
    }
    LagoonDivider()
}

@Composable
private fun Cell(width: Dp, end: Boolean = false, content: @Composable () -> Unit) {
    Box(Modifier.width(width).padding(horizontal = 8.dp), contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart) { content() }
}

// ---------------------------------------------------------------- new order

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowScope.DraftWorkspace(state: OrderDeskState, draft: Draft) {
    val c = Lagoon.colors
    Column(Modifier.weight(1f).fillMaxHeight().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LagoonCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(draft.customer.initials, key = draft.customer.name)
                Column {
                    Text("New order for", style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
                    Text(draft.customer.name, style = Lagoon.type.heading.copy(fontWeight = Lagoon.type.title.fontWeight))
                }
                TextAction("Change", { state.pickingCustomer = true })
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    draft.customer.termTags().forEach { LagoonTag(it, tone = if (it.startsWith("PO")) LagoonTone.Attention else null) }
                }
                LagoonIconButton(LagoonIcons.Trash, "Discard draft", { state.discard(draft) }, size = 32.dp, lifted = false, background = c.surfaceMuted, tint = c.inkMuted)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CategoryTree(state.store.catalog.root, draft.category, { draft.category = it }, Modifier.width(180.dp))
            ProductsTable(state, draft, Modifier.weight(1f).fillMaxHeight())
            CartPanel(state, draft, Modifier.width(320.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun CategoryTree(root: Category, selected: Category, onSelect: (Category) -> Unit, modifier: Modifier) {
    val c = Lagoon.colors
    @Composable
    fun item(category: Category, depth: Int) {
        val on = category === selected
        Row(
            Modifier.fillMaxWidth().height(32.dp).clip(Lagoon.shapes.small)
                .background(if (on) c.night else Color.Transparent)
                .clickable { onSelect(category) }.padding(start = (10 + depth * 14).dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (depth == 1) Icon(category.icon(), null, Modifier.size(15.dp), tint = if (on) c.onNight else c.inkMuted)
            Text(
                category.label,
                style = if (depth <= 1) Lagoon.type.bodyStrong else Lagoon.type.body,
                color = if (on) c.onNight else c.ink,
            )
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Categories", Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp), style = Lagoon.type.label, color = c.inkMuted)
        item(root, 0)
        root.children.forEach { top ->
            item(top, 1)
            top.children.forEach { item(it, 2) }
        }
    }
}

@Composable
private fun ProductsTable(state: OrderDeskState, draft: Draft, modifier: Modifier) {
    val c = Lagoon.colors
    val products = state.store.catalog.search(draft.category, draft.productQuery)
    LagoonCard(modifier) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LagoonSearchField(draft.productQuery, { draft.productQuery = it }, "Search products", Modifier.weight(1f))
            Text("${products.size} products", style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
        }
        LagoonDivider()
        TableHeader(listOf("Product" to null, "Price" to 76.dp, "Stock" to 84.dp, "" to 100.dp), endAligned = setOf("Price", "Stock"))
        LazyColumn(Modifier.weight(1f)) {
            items(products, key = { it.sku }) { p ->
                val qty = draft.cart.quantityOf(p)
                val faded = if (p.inStock) 1f else 0.5f
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Lagoon.metrics.rowHeight + 4.dp).background(if (qty > 0) c.selection else c.surface).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(p.name, style = Lagoon.type.body, color = c.ink.copy(alpha = faded), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.sku, style = Lagoon.type.label.copy(fontFamily = FontFamily.Monospace, fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted.copy(alpha = faded))
                    }
                    Cell(76.dp, end = true) { Text(p.price.format(), style = Lagoon.type.bodyStrong, color = c.ink.copy(alpha = faded)) }
                    Cell(84.dp, end = true) {
                        Text(
                            when { !p.inStock -> "Sold out"; p.lowStock -> "Only ${p.stock}"; else -> p.stock.toString() },
                            style = if (p.lowStock) Lagoon.type.label else Lagoon.type.small,
                            color = if (p.lowStock) c.attention else c.inkMuted.copy(alpha = faded),
                            maxLines = 1,
                        )
                    }
                    Cell(100.dp, end = true) {
                        AddOrStep(p, qty, { state.add(draft, p) }, { state.increment(draft, p) }, { state.decrement(draft, p) })
                    }
                }
                LagoonDivider()
            }
            if (products.isEmpty()) item { EmptyNote("No products match.") }
        }
    }
}

@Composable
private fun CartPanel(state: OrderDeskState, draft: Draft, modifier: Modifier) {
    val c = Lagoon.colors
    val cart = draft.cart
    LagoonCard(modifier) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cart", Modifier.weight(1f), style = Lagoon.type.heading.copy(fontWeight = Lagoon.type.title.fontWeight))
            Text("${cart.lines.size} products · ${cart.units} items", style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
        }
        LagoonDivider()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (cart.isEmpty) EmptyNote("Add products with +.")
            cart.lines.forEach { line ->
                val p = line.product
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(p.name, Modifier.weight(1f), style = Lagoon.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Pricing.lineTotal(line).format(), style = Lagoon.type.bodyStrong)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LagoonStepper(line.quantity, { state.decrement(draft, p) }, { state.increment(draft, p) }, label = p.name, canIncrement = line.quantity < p.stock)
                        Text("× ${p.price.format()}", Modifier.weight(1f), style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
                        if (line.discounted) LagoonTag("−5%", tone = LagoonTone.Success)
                        LagoonIconButton(LagoonIcons.Trash, "Remove ${p.name}", { state.remove(draft, p) }, size = 28.dp, lifted = false, tint = c.inkMuted)
                    }
                    if (line.overStock) Text(p.onlyInStockMessage(), style = Lagoon.type.small, color = c.danger)
                }
                LagoonDivider()
            }
        }
        Column(Modifier.background(c.ground.copy(alpha = 0.5f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalsBlock(cart.totals(), shippingText = "at checkout")
            LagoonButton(
                "Checkout…", { state.startCheckout(draft) }, Modifier.fillMaxWidth(),
                enabled = !cart.isEmpty && cart.stockProblems().isEmpty(), height = 42.dp,
            )
        }
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
private fun CustomerPickerDialog(state: OrderDeskState) {
    var query by remember { mutableStateOf("") }
    val draft = state.activeDraft
    Overlay({ state.pickingCustomer = false }) {
        LagoonCard(Modifier.width(440.dp).heightIn(max = 620.dp), padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft != null) "Change customer" else "New order", Modifier.weight(1f), style = Lagoon.type.headline)
                LagoonIconButton(LagoonIcons.Close, "Close", { state.pickingCustomer = false }, size = 32.dp, lifted = false, background = Lagoon.colors.surfaceMuted)
            }
            LagoonSearchField(query, { query = it }, "Search customers or cities", Modifier.fillMaxWidth().padding(vertical = 14.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val list = if (query.isBlank()) state.store.orders.recentCustomers(3) + state.store.findCustomers("").filterNot { it in state.store.orders.recentCustomers(3) }
                else state.store.findCustomers(query)
                CustomerList(list) { customer ->
                    if (draft != null) state.changeCustomer(draft, customer) else state.startDraft(customer)
                    state.pickingCustomer = false
                }
                if (list.isEmpty()) EmptyNote("No customer matches.")
            }
        }
    }
}

@Composable
private fun CheckoutDialog(state: OrderDeskState, checkout: Checkout) {
    val c = Lagoon.colors
    Overlay({ state.checkout = null }) {
        Row(
            Modifier.widthIn(max = 800.dp).heightIn(max = 640.dp)
                .shadow(24.dp, Lagoon.shapes.dialog, ambientColor = c.shadow.copy(alpha = 0.4f), spotColor = c.shadow.copy(alpha = 0.4f))
                .clip(Lagoon.shapes.dialog).background(c.ground),
        ) {
            Column(Modifier.width(220.dp).fillMaxHeight().padding(horizontal = 14.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Checkout", Modifier.padding(start = 10.dp), style = Lagoon.type.headline)
                Text(checkout.draft.customer.name, Modifier.padding(start = 10.dp, bottom = 16.dp), style = Lagoon.type.small, color = c.inkMuted)
                Checkout.STEP_TITLES.forEachIndexed { i, title -> StepItem(i + 1, title, checkout.step) }
                Spacer(Modifier.weight(1f))
                Column(Modifier.padding(horizontal = 10.dp)) { TotalsBlock(checkout.totals, shippingText = if (checkout.totals.shipping.isZero) "Free" else null) }
            }
            Column(Modifier.weight(1f).fillMaxHeight().background(c.surface)) {
                Row(Modifier.padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Step ${checkout.step} of 4", style = Lagoon.type.label, color = c.primaryStrong)
                        Text(stepTitle(checkout.step), style = Lagoon.type.headline)
                    }
                    LagoonIconButton(LagoonIcons.Close, "Close checkout", { state.checkout = null }, size = 32.dp, lifted = false, background = c.surfaceMuted)
                }
                CheckoutStepBody(state, checkout, Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp))
                LagoonDivider()
                Row(Modifier.padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    LagoonButton("Back", { state.backCheckout() }, style = LagoonButtonStyle.Outline)
                    LagoonButton(if (checkout.step == 4) "Place order · ${checkout.totals.total.format()}" else "Continue", { state.continueCheckout() })
                }
            }
        }
    }
}

@Composable
private fun StepItem(number: Int, title: String, current: Int) {
    val c = Lagoon.colors
    val done = number < current
    val on = number == current
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(Lagoon.shapes.small)
            .then(if (on) Modifier.shadow(2.dp, Lagoon.shapes.small).background(c.surface) else Modifier)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (done) c.primary else Color.Transparent)
                .then(if (!done) Modifier.background(c.surface) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(LagoonIcons.Check, null, Modifier.size(12.dp), tint = c.onPrimary)
            else Text(number.toString(), style = Lagoon.type.label, color = if (on) c.primaryStrong else c.inkMuted)
        }
        Text(title, style = if (on) Lagoon.type.bodyStrong else Lagoon.type.body, color = if (on) c.ink else c.inkMuted)
    }
}
