package io.github.kormcrete.orderdesk.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TODAY = Day(2026, 10, 12)
private fun store() = Seed.store { TODAY }

class MoneyTest {
    @Test
    fun formatsWithGroupingAndMinus() {
        assertEquals("€1,234.50", Money.of("1234.5").format())
        assertEquals("€0.05", Money.ofCents(5).format())
        assertEquals("−€0.60", (-Money.of("0.60")).format())
        assertEquals("€1,000,000.00", Money.of("1000000").format())
    }

    @Test
    fun percentRoundsHalfUp() {
        assertEquals(Money.ofCents(3), Money.ofCents(50).percent(5)) // 2.5 cents
        assertEquals(Money.ofCents(2), Money.ofCents(49).percent(5)) // 2.45 cents
    }

    @Test
    fun aFractionOfACentIsAnError() {
        assertFailsWith<IllegalArgumentException> { Money.of("0.001") }
    }
}

private fun Money.Companion.ofCents(cents: Long) = Money(cents)

class PricingTest {
    @Test
    fun ord1637MatchesTheSwingApp() {
        val order = store().orders.find("ORD-1637")!!
        assertEquals("€102.90", order.totals.subtotal.format())
        assertEquals("€14.90", order.totals.shipping.format())
        assertEquals("€23.56", order.totals.vat.format())
        assertEquals("€141.36", order.totals.total.format())
    }

    @Test
    fun tenOrMoreTakeFivePercentOffThatLine() {
        val s = store()
        val cart = Cart()
        val pen = s.catalog.find("PEN-GEL-BLK")!!
        cart.add(pen, 10)
        assertEquals("€0.60", cart.totals().discount.format())
        cart.setQuantity(pen, 9)
        assertTrue(cart.totals().discount.isZero)
    }

    @Test
    fun standardIsFreeFromOneHundredAfterDiscount() {
        val s = store()
        val cart = Cart()
        cart.add(s.catalog.find("COF-GRINDER")!!, 1) // €89.00
        assertEquals("€4.90", cart.totals(Shipping.STANDARD).shipping.format())
        assertEquals("€11.00", Pricing.moreForFreeShipping(cart.totals())!!.format())
        cart.add(s.catalog.find("COF-FILTERS")!!, 4) // +€11.60
        assertTrue(cart.totals(Shipping.STANDARD).shipping.isZero)
        assertNull(Pricing.moreForFreeShipping(cart.totals()))
        assertEquals("€14.90", cart.totals(Shipping.EXPRESS).shipping.format())
    }

    @Test
    fun theMockupCart() {
        val s = store()
        val cart = Cart()
        cart.add(s.catalog.find("PAP-A4-500")!!, 3)
        cart.add(s.catalog.find("PEN-GEL-BLK")!!, 10)
        cart.add(s.catalog.find("PAP-NOTE-A5")!!, 5)
        cart.add(s.catalog.find("COF-BEANS-1K")!!, 2)
        assertEquals(20, cart.units)
        assertEquals("€92.28", cart.totals().total.format())
        assertEquals("€98.16", cart.totals(Shipping.STANDARD).total.format())
        assertEquals("€23.10", Pricing.moreForFreeShipping(cart.totals())!!.format())
    }
}

class CartTest {
    @Test
    fun addingTwiceAddsToTheLine() {
        val s = store()
        val cart = Cart()
        val tray = s.catalog.find("DSK-TRAY")!!
        cart.add(tray)
        cart.add(tray, 2)
        assertEquals(1, cart.lines.size)
        assertEquals(3, cart.quantityOf(tray))
    }

    @Test
    fun decrementRemovesTheLastOne() {
        val s = store()
        val cart = Cart()
        val tray = s.catalog.find("DSK-TRAY")!!
        cart.add(tray)
        cart.decrement(tray)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun outOfStockCannotBeAdded() {
        val s = store()
        assertFailsWith<IllegalArgumentException> { Cart().add(s.catalog.find("DSK-LAMP")!!) }
    }

    @Test
    fun moreThanInStockIsAProblem() {
        val s = store()
        val cart = Cart()
        cart.add(s.catalog.find("DSK-STAPLER")!!, 5)
        assertEquals(listOf("Only 4 Staplers in stock"), cart.stockProblems())
    }

    @Test
    fun stockMessages() {
        val s = store()
        assertEquals("No Desk lamps in stock", s.catalog.find("DSK-LAMP")!!.onlyInStockMessage())
        assertEquals("Only 35 × Bin bags, roll of 50 in stock", s.catalog.find("CLN-BAGS")!!.onlyInStockMessage())
    }
}

class OrderBookTest {
    private fun cartOf(s: Store, vararg lines: Pair<String, Int>) = Cart().also { cart ->
        lines.forEach { (sku, qty) -> cart.add(s.catalog.find(sku)!!, qty) }
    }

    @Test
    fun placingTakesStockAndAddsToWhatTheCustomerOwes() {
        val s = store()
        val ada = s.customers.first { it.name == "Ada Shop Ltd" }
        val beans = s.catalog.find("COF-BEANS-1K")!!
        val order = s.place(ada, cartOf(s, "COF-BEANS-1K" to 2), ada.address, Shipping.STANDARD, "", Payment.invoice("PO-1"))
        assertEquals("ORD-1640", order.number)
        assertEquals(TODAY, order.date)
        assertEquals(58, beans.stock)
        assertEquals(Money.of("450.00") + order.totals.total, ada.owes)
        assertEquals("ORD-1641", s.orders.nextNumber)
    }

    @Test
    fun cancellingGivesItAllBack() {
        val s = store()
        val birch = s.customers.first { it.name == "Birch & Co" }
        val order = s.orders.find("ORD-1637")!!
        val kettle = s.catalog.find("COF-KETTLE")!!
        s.cancel(listOf(order))
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(13, kettle.stock)
        assertEquals(Money.of("798.64"), birch.owes)
        assertFailsWith<IllegalStateException> { s.cancel(listOf(order)) }
    }

    @Test
    fun expressOnlyToGermanyAndAustria() {
        val s = store()
        val lumen = s.customers.first { it.name == "Café Lumen" }
        assertFailsWith<IllegalStateException> {
            s.place(lumen, cartOf(s, "PAP-A4-500" to 1), lumen.address, Shipping.EXPRESS, "", Payment.card("4242424242424242"))
        }
    }

    @Test
    fun cardOnlyCustomersCannotPayOnInvoice() {
        val s = store()
        val lumen = s.customers.first { it.name == "Café Lumen" }
        assertFailsWith<IllegalStateException> {
            s.place(lumen, cartOf(s, "PAP-A4-500" to 1), lumen.address, Shipping.STANDARD, "", Payment.invoice())
        }
    }

    @Test
    fun openOrdersAndRecentCustomers() {
        val s = store()
        assertEquals(4, s.orders.openCount)
        assertEquals("€533.64", s.orders.openTotal.format())
        assertEquals(listOf("Ada Shop Ltd", "Café Lumen", "Birch & Co"), s.orders.recentCustomers(3).map { it.name })
        assertEquals("ORD-1639", s.orders.newestFirst().first().number)
    }

    @Test
    fun creditCheck() {
        val s = store()
        val birch = s.customers.first { it.name == "Birch & Co" }
        assertTrue(CreditCheck.exceeds(birch, Money.of("60.01")))
        assertFalse(CreditCheck.exceeds(birch, Money.of("60.00")))
        assertEquals("This order takes Birch & Co to €1,068.40 of its €1,000.00 credit limit.", CreditCheck.message(birch, Money.of("128.40")))
        val ada = s.customers.first { it.name == "Ada Shop Ltd" }
        assertEquals("€1,451.84", CreditCheck.leftAfter(ada, Money.of("98.16")).format())
    }
}

class RulesTest {
    @Test
    fun addressRules() {
        val ok = Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "+49 30 5550 1188")
        assertTrue(AddressRules.validate(ok).isEmpty())
        val bad = ok.copy(postalCode = "1011", phone = "call me")
        val errors = AddressRules.validate(bad)
        assertEquals("Germany uses a 5-digit postal code", errors[AddressField.POSTAL_CODE])
        assertEquals("Only +, digits and spaces, 6–20 characters", errors[AddressField.PHONE])
        assertTrue(Country.NETHERLANDS.acceptsPostalCode(" 1016 gv "))
    }

    @Test
    fun cardRules() {
        assertEquals("4242 4242 4242 4242", CardRules.format("4242-4242 4242 42425"))
        assertEquals("12/28", CardRules.formatExpiry("1228"))
        assertTrue(CardRules.validate("4242 4242 4242 4242", "10/26", "Ada", TODAY).isEmpty())
        val errors = CardRules.validate("4242", "09/26", " ", TODAY)
        assertEquals("Enter the 16 digits of the card", errors[CardField.NUMBER])
        assertEquals("This card has expired", errors[CardField.EXPIRY])
        assertEquals("Name on card is required", errors[CardField.NAME])
        assertEquals("Month must be 01–12", CardRules.validate("4242424242424242", "13/27", "A", TODAY)[CardField.EXPIRY])
        assertEquals("Card ending 4242", Payment.card("4242 4242 4242 4242").describe())
    }

    @Test
    fun receipt() {
        val text = ReceiptText.of(store().orders.find("ORD-1637")!!)
        assertTrue(text.startsWith("Order Desk — receipt\nORD-1637 · 5 Oct 2026 · Placed\n"))
        assertTrue("€141.36" in text.lines().last { it.startsWith("Total") })
    }
}

class SeedTest {
    @Test
    fun seedMatchesTheSwingApp() {
        val s = store()
        assertEquals(22, s.catalog.products.size)
        assertEquals(12, s.orders.orders.size)
        assertEquals(6, s.customers.size)
        assertEquals("04109", s.orders.find("ORD-1628")!!.address.postalCode)
        assertEquals("AS", s.customers.first { it.name == "Ada Shop Ltd" }.initials)
        assertEquals("BC", s.customers.first { it.name == "Birch & Co" }.initials)
        assertEquals(listOf("Invoice · 30 days", "€1,550.00 credit left", "PO number required"), s.customers.first().termTags())
    }

    @Test
    fun days() {
        assertEquals("9 Oct 2026", Day.parse("2026-10-09").format())
        assertTrue(Day(2026, 10, 9) > Day(2026, 9, 30))
    }
}
