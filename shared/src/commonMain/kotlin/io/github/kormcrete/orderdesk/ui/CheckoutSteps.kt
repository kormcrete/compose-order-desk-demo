package io.github.kormcrete.orderdesk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kormcrete.lagoon.compose.IconTile
import io.github.kormcrete.lagoon.compose.Lagoon
import io.github.kormcrete.lagoon.compose.LagoonBanner
import io.github.kormcrete.lagoon.compose.LagoonCard
import io.github.kormcrete.lagoon.compose.LagoonChip
import io.github.kormcrete.lagoon.compose.LagoonDivider
import io.github.kormcrete.lagoon.compose.LagoonIcons
import io.github.kormcrete.lagoon.compose.LagoonTextField
import io.github.kormcrete.lagoon.compose.LagoonTone
import io.github.kormcrete.lagoon.compose.OptionCard
import io.github.kormcrete.orderdesk.model.AddressField
import io.github.kormcrete.orderdesk.model.CardField
import io.github.kormcrete.orderdesk.model.CardRules
import io.github.kormcrete.orderdesk.model.CreditCheck
import io.github.kormcrete.orderdesk.model.Country
import io.github.kormcrete.orderdesk.model.Pricing
import io.github.kormcrete.orderdesk.model.Shipping

/** The question each step asks, as its title. */
fun stepTitle(step: Int) = when (step) {
    1 -> "Where does it go?"
    2 -> "How should it ship?"
    3 -> "How is it paid?"
    else -> "Review and place"
}

/** The body of the current checkout step. */
@Composable
fun CheckoutStepBody(state: OrderDeskState, c: Checkout, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (c.step) {
            1 -> AddressStep(c)
            2 -> ShippingStep(c)
            3 -> PaymentStep(state, c)
            else -> ReviewStep(state, c)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddressStep(c: Checkout) {
    val errors = if (c.showErrors) c.addressErrors() else emptyMap()
    LagoonTextField(c.name, { c.name = it }, "Name", error = errors[AddressField.NAME])
    LagoonTextField(c.street, { c.street = it }, "Street", error = errors[AddressField.STREET])
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LagoonTextField(c.postalCode, { c.postalCode = it }, "Postal code", Modifier.weight(0.42f), error = errors[AddressField.POSTAL_CODE])
        LagoonTextField(c.city, { c.city = it }, "City", Modifier.weight(0.58f), error = errors[AddressField.CITY])
    }
    Text("Country", style = Lagoon.type.small.copy(fontWeight = Lagoon.type.label.fontWeight))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Country.entries.forEach { country ->
            LagoonChip(country.label, c.country == country, { c.selectCountry(country) })
        }
    }
    LagoonTextField(
        c.phone, { c.phone = it }, "Phone", hint = "(optional)", error = errors[AddressField.PHONE],
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
}

@Composable
private fun ShippingStep(c: Checkout) {
    Shipping.entries.forEach { s ->
        val allowed = s.allowedFor(c.country)
        OptionCard(
            selected = c.shipping == s,
            onClick = { c.shipping = s },
            title = s.label,
            subtitle = if (s == Shipping.EXPRESS) "Next business day · Germany and Austria only" else "3–5 business days · free from ${Shipping.FREE_FROM.format()}",
            icon = if (s == Shipping.EXPRESS) LagoonIcons.Bolt else LagoonIcons.Truck,
            tone = if (s == Shipping.EXPRESS) LagoonTone.Attention else LagoonTone.Info,
            amount = s.cost(c.draft.cart.totals().discounted).let { if (it.isZero) "Free" else it.format() },
            enabled = allowed,
        )
    }
    Pricing.moreForFreeShipping(c.draft.cart.totals())?.let { more ->
        LagoonBanner("Add ${more.format()} more for free Standard shipping", LagoonTone.Success, icon = LagoonIcons.Gift)
    }
    LagoonTextField(c.note, { c.note = it }, "Delivery note", hint = "(optional)", placeholder = "e.g. Ring at reception", singleLine = false, minLines = 3)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(LagoonIcons.Pin, size = 28.dp)
        Text("To ${c.name}, ${c.address.oneLine()}", style = Lagoon.type.small, color = Lagoon.colors.inkMuted)
    }
}

@Composable
private fun PaymentStep(state: OrderDeskState, c: Checkout) {
    val customer = c.draft.customer
    val cardErrors = if (c.showErrors) c.cardErrors(state.store.today) else emptyMap()
    if (customer.terms.invoiceAllowed) {
        OptionCard(
            selected = c.payByInvoice, onClick = { c.payByInvoice = true },
            title = "Invoice", subtitle = "30 days · ${customer.creditLeft.format()} credit left",
            icon = LagoonIcons.Invoice,
        )
    }
    OptionCard(
        selected = !c.payByInvoice, onClick = { c.payByInvoice = false },
        title = "Card", subtitle = "Debit or credit card",
        icon = LagoonIcons.Card, tone = LagoonTone.Success,
    )
    if (c.payByInvoice) {
        LagoonTextField(
            c.poNumber, { c.poNumber = it }, "PO number",
            hint = if (customer.terms.poRequired) "(required)" else "(optional)",
            placeholder = "e.g. PO-2026-140",
            error = if (c.showErrors) c.poError() else null,
        )
    } else {
        LagoonTextField(
            c.cardNumber, { c.cardNumber = CardRules.format(it) }, "Card number", placeholder = "1234 1234 1234 1234",
            error = cardErrors[CardField.NUMBER], keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LagoonTextField(
                c.cardExpiry, { c.cardExpiry = CardRules.formatExpiry(it) }, "Expiry", Modifier.weight(0.4f), placeholder = "MM/YY",
                error = cardErrors[CardField.EXPIRY], keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            LagoonTextField(c.cardName, { c.cardName = it }, "Name on card", Modifier.weight(0.6f), error = cardErrors[CardField.NAME])
        }
    }
}

@Composable
private fun ReviewStep(state: OrderDeskState, c: Checkout) {
    val customer = c.draft.customer
    val totals = c.totals
    LagoonCard(padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
        ReviewRow(LagoonIcons.Pin, "Deliver to", "${c.name} · ${c.address.oneLine()}") { c.step = 1 }
        LagoonDivider()
        ReviewRow(if (c.shipping == Shipping.EXPRESS) LagoonIcons.Bolt else LagoonIcons.Truck, "Shipping", c.shipping.describe()) { c.step = 2 }
        LagoonDivider()
        ReviewRow(
            if (c.payByInvoice) LagoonIcons.Invoice else LagoonIcons.Card,
            if (c.payByInvoice) "Invoice · 30 days" else "Card",
            when {
                !c.payByInvoice -> c.payment.describe()
                c.poNumber.isBlank() -> "No PO number"
                else -> "PO number ${c.poNumber.trim()}"
            },
        ) { c.step = 3 }
    }
    if (c.payByInvoice) {
        val warning = state.creditWarning(c)
        if (warning != null) {
            LagoonBanner("$warning ${CreditCheck.detail(customer, totals.total)}", LagoonTone.Attention, icon = LagoonIcons.Warning)
        } else {
            LagoonBanner(
                "Within credit limit · ${CreditCheck.leftAfter(customer, totals.total).format()} left after this order",
                LagoonTone.Success, icon = LagoonIcons.Shield,
            )
        }
    }
    LagoonCard(padding = PaddingValues(16.dp)) {
        Text("${c.draft.cart.units} items", Modifier.padding(bottom = 10.dp), style = Lagoon.type.bodyStrong)
        c.draft.cart.lines.forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("${line.quantity} × ${line.name}", Modifier.weight(1f), style = Lagoon.type.small)
                Text(Pricing.lineTotal(line).format(), style = Lagoon.type.small)
            }
        }
        DashedSeparator(Modifier.padding(vertical = 12.dp))
        TotalsBlock(totals, shippingText = if (totals.shipping.isZero) "Free" else null)
    }
}

@Composable
private fun ReviewRow(icon: ImageVector, label: String, value: String, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = Lagoon.type.label, color = Lagoon.colors.inkMuted)
            Text(value, style = Lagoon.type.body)
        }
        TextAction("Edit", onEdit)
    }
}

/** A link-like action: "Edit", "Change". */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clip(Lagoon.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        style = Lagoon.type.bodyStrong,
        color = Lagoon.colors.primary,
    )
}
