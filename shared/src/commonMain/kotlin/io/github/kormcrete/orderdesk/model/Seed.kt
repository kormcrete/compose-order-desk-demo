package io.github.kormcrete.orderdesk.model

/** The demo data the app starts with. All customers, products and orders are invented. */
object Seed {
    const val NEXT_ORDER_NUMBER = 1640

    fun store(today: () -> Day = ::systemToday): Store {
        val all = Category("all", "All products", null)
        val office = Category("office", "Office", all)
        val paper = Category("office/paper", "Paper", office)
        val pens = Category("office/pens", "Pens", office)
        val desk = Category("office/desk", "Desk", office)
        val coffee = Category("coffee", "Coffee", all)
        val beans = Category("coffee/beans", "Beans", coffee)
        val machines = Category("coffee/machines", "Machines", coffee)
        val cleaning = Category("cleaning", "Cleaning", all)
        fun p(sku: String, name: String, category: Category, price: String, stock: Int) =
            Product(sku, name, category, Money.of(price), stock)
        val catalog = Catalog(
            all, listOf(
                p("PAP-A4-500", "A4 paper, 500 sheets", paper, "4.90", 120),
                p("PAP-A3-250", "A3 paper, 250 sheets", paper, "7.80", 40),
                p("PAP-NOTE-A5", "Notebook A5, ruled", paper, "2.60", 150),
                p("PAP-STICKY", "Sticky notes, 3 pads", paper, "3.10", 90),
                p("PEN-GEL-BLK", "Gel pen, black", pens, "1.20", 800),
                p("PEN-GEL-BLU", "Gel pen, blue", pens, "1.20", 650),
                p("PEN-MARK-SET", "Permanent markers, set of 4", pens, "5.40", 75),
                p("PEN-HILITE", "Highlighter, yellow", pens, "0.90", 300),
                p("DSK-STAPLER", "Stapler", desk, "12.50", 4),
                p("DSK-LAMP", "Desk lamp", desk, "39.00", 0),
                p("DSK-TRAY", "Letter tray", desk, "8.90", 22),
                p("DSK-CLIPS", "Paper clips, box of 100", desk, "1.50", 5),
                p("DSK-TAPE", "Tape dispenser", desk, "6.70", 18),
                p("COF-BEANS-1K", "Coffee beans, 1 kg", beans, "18.90", 60),
                p("COF-DECAF-500", "Decaf beans, 500 g", beans, "11.40", 25),
                p("COF-FILTERS", "Paper filters, 100", beans, "2.90", 110),
                p("COF-ESPRESSO", "Espresso machine", machines, "249.00", 3),
                p("COF-GRINDER", "Burr grinder", machines, "89.00", 7),
                p("COF-KETTLE", "Electric kettle", machines, "34.50", 12),
                p("CLN-WIPES", "Cleaning wipes", cleaning, "3.40", 200),
                p("CLN-SPRAY", "Glass cleaner, 500 ml", cleaning, "4.20", 48),
                p("CLN-BAGS", "Bin bags, roll of 50", cleaning, "6.30", 35),
            )
        )
        val ada = Customer(
            "Ada Shop Ltd", Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "+49 30 5550 1188"),
            PaymentTerms.invoice(Money.of("2000.00"), true), Money.of("450.00"),
        )
        val birch = Customer(
            "Birch & Co", Address("Jonas Birch", "Mariahilfer Straße 54", "1070", "Vienna", Country.AUSTRIA, ""),
            PaymentTerms.invoice(Money.of("1000.00"), false), Money.of("940.00"),
        )
        val lumen = Customer(
            "Café Lumen", Address("Noor de Vries", "Prinsengracht 263", "1016 GV", "Amsterdam", Country.NETHERLANDS, ""),
            PaymentTerms.cardOnly(), Money.ZERO,
        )
        val durand = Customer(
            "Durand Bureau", Address("Claire Durand", "14 Rue des Archives", "75004", "Paris", Country.FRANCE, ""),
            PaymentTerms.cardOnly(), Money.ZERO,
        )
        val elm = Customer(
            "Elm Street Studio", Address("Mia Hoffmann", "Elmstraße 7", "", "Leipzig", Country.GERMANY, ""),
            PaymentTerms.invoice(Money.of("5000.00"), false), Money.ZERO,
        )
        val fjord = Customer(
            "Fjord Supplies", Address("Lars Jensen", "Hafenstraße 21", "20359", "Hamburg", Country.GERMANY, ""),
            PaymentTerms.cardOnly(), Money.ZERO,
        )
        val book = OrderBook(catalog, NEXT_ORDER_NUMBER)
        fun order(
            number: Int, date: String, customer: Customer, status: OrderStatus, shipping: Shipping,
            payment: Payment, note: String, vararg lines: Pair<String, Int>,
        ) {
            val orderLines = lines.map { (sku, qty) ->
                val product = checkNotNull(catalog.find(sku)) { "No product $sku" }
                OrderLine(product, product.price, qty)
            }
            // Elm Street Studio has no postal code on file; its seed orders were delivered to 04109.
            val address = if (customer.address.postalCode.isEmpty()) customer.address.copy(postalCode = "04109") else customer.address
            book.addExisting(Order("ORD-$number", Day.parse(date), customer, orderLines, address, shipping, note, payment, status))
        }
        order(1628, "2026-09-01", elm, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.invoice(), "", "PAP-A4-500" to 20, "PEN-GEL-BLU" to 12)
        order(1629, "2026-09-05", fjord, OrderStatus.SHIPPED, Shipping.EXPRESS, Payment.card("1881"), "", "CLN-WIPES" to 6, "CLN-SPRAY" to 4, "CLN-BAGS" to 2)
        order(1630, "2026-09-10", durand, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("0005"), "", "COF-GRINDER" to 1, "COF-BEANS-1K" to 4)
        order(1631, "2026-09-15", birch, OrderStatus.SHIPPED, Shipping.EXPRESS, Payment.invoice(), "Deliver to the back entrance, please.", "COF-ESPRESSO" to 1, "COF-BEANS-1K" to 10, "COF-FILTERS" to 3)
        order(1632, "2026-09-18", lumen, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("4242"), "", "COF-BEANS-1K" to 12, "COF-DECAF-500" to 4)
        order(1633, "2026-09-22", ada, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.invoice("PO-2026-118"), "", "PAP-A4-500" to 10, "DSK-TRAY" to 2, "PEN-MARK-SET" to 3)
        order(1634, "2026-09-25", elm, OrderStatus.CANCELLED, Shipping.STANDARD, Payment.invoice(), "", "DSK-TAPE" to 2, "PAP-STICKY" to 5)
        order(1635, "2026-09-29", durand, OrderStatus.PLACED, Shipping.STANDARD, Payment.card("0005"), "", "PAP-NOTE-A5" to 30, "PEN-HILITE" to 20)
        order(1636, "2026-10-02", fjord, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("1881"), "", "CLN-BAGS" to 10, "CLN-WIPES" to 24)
        order(1637, "2026-10-05", birch, OrderStatus.PLACED, Shipping.EXPRESS, Payment.invoice(), "", "COF-DECAF-500" to 6, "COF-KETTLE" to 1)
        order(1638, "2026-10-07", lumen, OrderStatus.PLACED, Shipping.STANDARD, Payment.card("4242"), "Ring twice.", "COF-BEANS-1K" to 8)
        order(1639, "2026-10-09", ada, OrderStatus.PLACED, Shipping.STANDARD, Payment.invoice("PO-2026-131"), "", "PEN-GEL-BLK" to 50, "PAP-A4-500" to 3, "DSK-CLIPS" to 2)
        return Store(catalog, listOf(ada, birch, lumen, durand, elm, fjord), book, today)
    }
}
