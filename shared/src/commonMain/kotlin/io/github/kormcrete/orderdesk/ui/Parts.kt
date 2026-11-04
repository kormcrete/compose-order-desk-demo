package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.AmountRow
import io.github.kormcrete.lagoon.compose.AvatarTile
import io.github.kormcrete.lagoon.compose.IconTile
import io.github.kormcrete.lagoon.compose.Lagoon
import io.github.kormcrete.lagoon.compose.LagoonButton
import io.github.kormcrete.lagoon.compose.LagoonButtonStyle
import io.github.kormcrete.lagoon.compose.LagoonCard
import io.github.kormcrete.lagoon.compose.LagoonDivider
import io.github.kormcrete.lagoon.compose.LagoonIconButton
import io.github.kormcrete.lagoon.compose.LagoonIcons
import io.github.kormcrete.lagoon.compose.LagoonStepper
import io.github.kormcrete.lagoon.compose.LagoonTag
import io.github.kormcrete.lagoon.compose.LagoonTone
import io.github.kormcrete.lagoon.compose.SectionLabel
import io.github.kormcrete.lagoon.compose.StatusPill
import io.github.kormcrete.orderdesk.model.Category
import io.github.kormcrete.orderdesk.model.Customer
import io.github.kormcrete.orderdesk.model.Order
import io.github.kormcrete.orderdesk.model.OrderStatus
import io.github.kormcrete.orderdesk.model.Product
import io.github.kormcrete.orderdesk.model.Shipping
import io.github.kormcrete.orderdesk.model.Totals

// ---------------------------------------------------------------- mapping model to looks

fun OrderStatus.tone() = when (this) {
    OrderStatus.PLACED -> LagoonTone.Info
    OrderStatus.SHIPPED -> LagoonTone.Success
    OrderStatus.CANCELLED -> LagoonTone.Neutral
}

/** "A4" for A4 paper, "GP" for Gel pen: a product's tile. */
fun Product.code(): String {
    val words = name.split(' ', ',').filter { it.isNotBlank() }
    val first = words.first()
    return if (first.length <= 3 && first.any { it.isDigit() }) first.uppercase()
    else words.take(2).joinToString("") { it.first().uppercase() }
}

fun Category.icon(): ImageVector = when (top.id) {
    "office" -> LagoonIcons.Office
    "coffee" -> LagoonIcons.Coffee
    "cleaning" -> LagoonIcons.Drop
    else -> LagoonIcons.List
}

/** Tiles are coloured by top category, so a product list reads by colour too. */
@Composable
fun Product.tileColors() = Lagoon.colors.tile(category.top.id)

fun Product.stockText(): String = when {
    !inStock -> "Out of stock"
    lowStock -> "Only $stock left"
    else -> "$stock in stock"
}

// ---------------------------------------------------------------- orders

/** An order in a list: customer tile, customer, number · date · items, total and status. */
@Composable
fun OrderRow(order: Order, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Lagoon.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(if (selected) c.selection else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarTile(order.customer.initials, key = order.customer.name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(order.customer.name, style = Lagoon.type.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${order.number} · ${order.date.formatShort()} · ${order.units} items",
                style = Lagoon.type.small, color = c.inkMuted, maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(order.totals.total.format(), style = Lagoon.type.bodyStrong)
            StatusPill(order.status.label, order.status.tone())
        }
    }
}

/** Placed → Shipped as a thin progress line. */
@Composable
fun StatusTrack(status: OrderStatus, modifier: Modifier = Modifier) {
    val c = Lagoon.colors
    if (status == OrderStatus.CANCELLED) {
        LagoonTag("Cancelled · stock and credit given back", modifier, LagoonTone.Neutral)
        return
    }
    val shipped = status == OrderStatus.SHIPPED
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(c.primary))
        Text("Placed", style = Lagoon.type.label, color = c.ink)
        Box(
            Modifier.weight(1f).height(3.dp).clip(Lagoon.shapes.pill)
                .background(if (shipped) Brush.horizontalGradient(listOf(c.primary, c.success)) else Brush.horizontalGradient(0.5f to c.primary, 0.5f to c.line))
        )
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (shipped) c.success else c.line))
        Text("Shipped", style = Lagoon.type.label, color = if (shipped) c.ink else c.inkMuted)
    }
}

/** The order's content: hero card, delivery facts, items with totals. Phone detail and desktop panel share it. */
@Composable
fun OrderSummary(order: Order, modifier: Modifier = Modifier, dense: Boolean = false) {
    val c = Lagoon.colors
    val gap = if (dense) 10.dp else 12.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        LagoonCard(padding = PaddingValues(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(order.customer.initials, key = order.customer.name, size = 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(order.customer.name, style = Lagoon.type.headline.copy(fontSize = Lagoon.metrics.headlineSize * 0.8f))
                    Text("${order.number} · ${order.date.format()} · ${order.units} items", style = Lagoon.type.small, color = c.inkMuted)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
                Text(order.totals.total.format(), Modifier.weight(1f), style = Lagoon.type.amount)
                StatusPill(order.status.label, order.status.tone())
            }
            StatusTrack(order.status)
        }
        LagoonCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            FactRow(LagoonIcons.Pin, "Delivery", order.address.lines().take(4).joinToString("\n"))
            LagoonDivider()
            FactRow(if (order.shipping == Shipping.EXPRESS) LagoonIcons.Bolt else LagoonIcons.Truck, "Shipping", order.shipping.describe())
            LagoonDivider()
            FactRow(if (order.payment.isInvoice) LagoonIcons.Invoice else LagoonIcons.Card, "Payment", order.payment.describe())
            if (order.note.isNotEmpty()) {
                LagoonDivider()
                FactRow(LagoonIcons.Info, "Note", order.note)
            }
        }
        LagoonCard(padding = PaddingValues(16.dp)) {
            SectionLabel("Items")
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                order.lines.forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarTile("${line.quantity}×", colors = line.product.tileColors())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(line.name, style = Lagoon.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${line.price.format()} each", style = Lagoon.type.small, color = c.inkMuted)
                        }
                        Text(line.total.format(), style = Lagoon.type.bodyStrong)
                    }
                }
            }
            DashedSeparator(Modifier.padding(vertical = 12.dp))
            TotalsBlock(order.totals, shippingText = order.totals.shipping.let { if (it.isZero) "Free" else it.format() }, showTotal = false)
        }
    }
}

@Composable
fun FactRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = Lagoon.type.label, color = Lagoon.colors.inkMuted)
            Text(value, style = Lagoon.type.body)
        }
    }
}

/** Subtotal, discount, shipping, VAT and the total. [shippingText] overrides the shipping amount ("at checkout"). */
@Composable
fun TotalsBlock(totals: Totals, modifier: Modifier = Modifier, shippingText: String? = null, showTotal: Boolean = true) {
    val c = Lagoon.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AmountRow("Subtotal", totals.subtotal.format())
        if (totals.discount.isPositive) AmountRow("Volume discount", (-totals.discount).format(), amountColor = c.success)
        AmountRow("Shipping", shippingText ?: totals.shipping.format())
        AmountRow("VAT (20%)", totals.vat.format())
        if (showTotal) {
            DashedSeparator(Modifier.padding(vertical = 4.dp))
            AmountRow("Total", totals.total.format(), strong = true)
        }
    }
}

@Composable
fun DashedSeparator(modifier: Modifier = Modifier) {
    val color = Lagoon.colors.line
    Row(modifier.fillMaxWidth().height(1.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(80) { Box(Modifier.width(4.dp).height(1.dp).background(color)) }
    }
}

// ---------------------------------------------------------------- new order

/** The customer of a draft, with their terms as tags and a Change action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerCard(customer: Customer, onChange: () -> Unit, modifier: Modifier = Modifier, label: String = "Customer") {
    val c = Lagoon.colors
    LagoonCard(modifier, padding = PaddingValues(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarTile(customer.initials, key = customer.name)
            Column(Modifier.weight(1f)) {
                Text(label, style = Lagoon.type.label.copy(fontWeight = Lagoon.type.small.fontWeight), color = c.inkMuted)
                Text(customer.name, style = Lagoon.type.bodyStrong)
            }
            Text(
                "Change",
                Modifier.clip(Lagoon.shapes.small).clickable(onClick = onChange).padding(horizontal = 8.dp, vertical = 10.dp),
                style = Lagoon.type.bodyStrong,
                color = c.primary,
            )
        }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            customer.termTags().forEach { tag -> LagoonTag(tag, tone = if (tag.startsWith("PO")) LagoonTone.Attention else null) }
        }
    }
}

/** A product with its price and stock; a + to add it, or a stepper once it is in the cart. */
@Composable
fun ProductRow(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Lagoon.colors
    val faded = if (product.inStock) 1f else 0.5f
    Row(
        modifier.fillMaxWidth().background(if (quantity > 0) c.selection else c.surface).padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarTile(product.code(), Modifier.alpha(faded), size = 44.dp, colors = product.tileColors())
        Column(Modifier.weight(1f).alpha(faded), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(product.name, style = Lagoon.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.price.format(), style = Lagoon.type.small.copy(fontWeight = Lagoon.type.label.fontWeight))
                Text(
                    product.stockText(),
                    style = if (product.lowStock) Lagoon.type.small.copy(fontWeight = Lagoon.type.label.fontWeight) else Lagoon.type.small,
                    color = if (product.lowStock) c.attention else c.inkMuted,
                )
            }
        }
        AddOrStep(product, quantity, onAdd, onIncrement, onDecrement)
    }
}

@Composable
fun AddOrStep(product: Product, quantity: Int, onAdd: () -> Unit, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    val c = Lagoon.colors
    when {
        quantity > 0 -> LagoonStepper(
            quantity, onDecrement, onIncrement, label = product.name, filled = true, canIncrement = quantity < product.stock,
        )
        product.inStock -> {
            val size = if (Lagoon.metrics.touchTarget >= 44.dp) 40.dp else 30.dp
            Box(
                Modifier.size(size).clip(CircleShape).background(c.primaryTint).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LagoonIcons.Plus, "Add ${product.name}", Modifier.size(size * 0.45f), tint = c.primaryStrong)
            }
        }
        else -> Box(Modifier.size(40.dp))
    }
}

/** Content taller than the space it gets scrolls, with a scrollbar on computers: pass the modifier on to the content. */
@Composable
fun Scrolling(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val state = rememberScrollState()
    Box(modifier) {
        content(Modifier.verticalScroll(state))
        ScrollBar(state, Modifier.matchParentSize().wrapContentWidth(Alignment.End))
    }
}

/** A list that fills the rest of a column, with a scrollbar on computers. */
@Composable
fun ColumnScope.ScrollingList(content: LazyListScope.() -> Unit) {
    val state = rememberLazyListState()
    Box(Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), state, content = content)
        ScrollBar(state, Modifier.matchParentSize().wrapContentWidth(Alignment.End))
    }
}

// ---------------------------------------------------------------- overlays

/** A dimmed layer over the window with [content] centred: dialogs on desktop, confirmations on phones. */
@Composable
fun Overlay(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Lagoon.colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
            content()
        }
    }
}

/** "Cancel ORD-1637?" with what it does, and the two choices. */
@Composable
fun ConfirmCancel(order: Order, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        LagoonCard(Modifier.width(360.dp), padding = PaddingValues(22.dp)) {
            Text("Cancel ${order.number}?", style = Lagoon.type.headline.copy(fontSize = Lagoon.metrics.headlineSize * 0.8f))
            Text(
                "The stock goes back to the shelf" + if (order.payment.isInvoice) " and ${order.totals.total.format()} comes off what ${order.customer.name} owes." else ".",
                Modifier.padding(top = 8.dp, bottom = 20.dp),
                style = Lagoon.type.body,
                color = Lagoon.colors.inkMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LagoonButton("Keep", onDismiss, Modifier.weight(1f), style = LagoonButtonStyle.Outline)
                LagoonButton("Cancel order", onConfirm, Modifier.weight(1f), style = LagoonButtonStyle.Danger)
            }
        }
    }
}

/** The plain-text receipt, monospaced. */
@Composable
fun ReceiptView(text: String, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        LagoonCard(Modifier.width(560.dp), padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Receipt", Modifier.weight(1f), style = Lagoon.type.heading)
                LagoonIconButton(LagoonIcons.Close, "Close receipt", onDismiss, lifted = false, background = Lagoon.colors.surfaceMuted, size = 32.dp)
            }
            Box(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(Lagoon.shapes.control).background(Lagoon.colors.surfaceMuted)
                    .horizontalScroll(rememberScrollState()).padding(14.dp)
            ) {
                Text(text, style = Lagoon.type.small.copy(fontFamily = FontFamily.Monospace))
            }
        }
    }
}

/** A short message at the bottom: "ORD-1640 placed for Ada Shop Ltd". Tapping it closes it. */
@Composable
fun MessageBar(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Lagoon.colors
    Row(
        modifier
            .clip(Lagoon.shapes.control)
            .background(c.night)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LagoonIcons.Check, null, Modifier.size(18.dp), tint = c.nightAccent)
        Text(text, style = Lagoon.type.bodyStrong, color = c.onNight)
    }
}

/** Gap between stacked cards. */
val Gap: Dp @Composable get() = Lagoon.metrics.gap
