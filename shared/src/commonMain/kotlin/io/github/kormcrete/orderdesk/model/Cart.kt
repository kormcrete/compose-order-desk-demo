package io.github.kormcrete.orderdesk.model

/** A product and how many of it, in a cart; the price is the product's current price. */
class CartLine internal constructor(val product: Product, quantity: Int) : PricedLine {
    override var quantity: Int = quantity
        internal set
    override val name get() = product.name
    override val price get() = product.price

    /** More than the shop has: the cart shows an error and checkout waits. */
    val overStock get() = quantity > product.stock

    /** 10 or more take 5% off this line. */
    val discounted get() = Pricing.lineDiscount(this).isPositive
}

/** The lines of a new order, in the order they were added. */
class Cart {
    private val _lines = mutableListOf<CartLine>()
    val lines: List<CartLine> get() = _lines

    /** Adds to the product's line if there is one. */
    fun add(product: Product, quantity: Int = 1) {
        require(quantity >= 1) { "Quantity must be at least 1, was $quantity" }
        require(product.inStock) { "${product.name} is out of stock" }
        val line = lineOf(product)
        if (line == null) _lines += CartLine(product, quantity) else line.quantity += quantity
    }

    fun setQuantity(product: Product, quantity: Int) {
        require(quantity >= 1) { "Quantity must be at least 1, was $quantity" }
        val line = requireNotNull(lineOf(product)) { "${product.name} is not in the cart" }
        line.quantity = quantity
    }

    /** One less; the line goes when it reaches zero. */
    fun decrement(product: Product) {
        val line = lineOf(product) ?: return
        if (line.quantity <= 1) remove(product) else line.quantity -= 1
    }

    fun remove(product: Product) {
        _lines.removeAll { it.product === product }
    }

    fun quantityOf(product: Product): Int = lineOf(product)?.quantity ?: 0

    val isEmpty get() = _lines.isEmpty()

    /** Units across all lines: "20 items". */
    val units get() = _lines.sumOf { it.quantity }

    /** "Only 4 Staplers in stock", one per line that asks for more than the shop has. */
    fun stockProblems(): List<String> = _lines.filter { it.overStock }.map { it.product.onlyInStockMessage() }

    /** Before checkout: no shipping yet, VAT on the goods. */
    fun totals(shipping: Shipping? = null): Totals = Pricing.of(_lines, shipping)

    private fun lineOf(product: Product) = _lines.firstOrNull { it.product === product }
}
