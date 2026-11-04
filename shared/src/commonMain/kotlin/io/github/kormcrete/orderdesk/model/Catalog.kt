package io.github.kormcrete.orderdesk.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** A node of the product tree; "All products" is the root. */
class Category(val id: String, val label: String, val parent: Category?) {
    private val _children = mutableListOf<Category>()
    val children: List<Category> get() = _children

    init {
        parent?._children?.add(this)
    }

    /** True for this category and every category below it. */
    operator fun contains(other: Category): Boolean {
        var c: Category? = other
        while (c != null) {
            if (c === this) return true
            c = c.parent
        }
        return false
    }

    /** The top-level category this one sits under (itself for a top-level one). */
    val top: Category get() = if (parent == null || parent.parent == null) this else parent.top

    override fun toString() = label
}

/** A product in the catalog; only its stock changes. */
class Product(val sku: String, val name: String, val category: Category, val price: Money, stock: Int) {
    var stock: Int by mutableIntStateOf(stock)
        internal set

    val inStock get() = stock > 0

    /** Five or fewer left: lists show a warning. */
    val lowStock get() = stock in 1..5

    /**
     * "Only 4 Staplers in stock"; a name with a comma reads "Only 35 × Bin bags, roll of 50 in stock".
     * One left adds no plural "s". None left reads "No Staplers in stock".
     */
    fun onlyInStockMessage(): String {
        val comma = ',' in name
        val plural = if (comma || stock == 1 || name.endsWith("s")) name else name + "s"
        if (stock == 0) return "No $plural in stock"
        val what = if (comma) "$stock × $name" else "$stock $plural"
        return "Only $what in stock"
    }

    override fun toString() = name
}

/** The products and their category tree. Stock changes only through placing and cancelling orders. */
class Catalog(val root: Category, products: List<Product>) {
    val products: List<Product> = products.toList()

    fun find(sku: String): Product? = products.firstOrNull { it.sku == sku }

    /** The products under [category], whose name or SKU contains [query] in any case. */
    fun search(category: Category, query: String): List<Product> {
        val q = query.trim().lowercase()
        return products.filter {
            it.category in category && (q.isEmpty() || q in it.name.lowercase() || q in it.sku.lowercase())
        }
    }

    internal fun take(product: Product, quantity: Int) {
        check(quantity <= product.stock) { product.onlyInStockMessage() }
        product.stock -= quantity
    }

    internal fun giveBack(product: Product, quantity: Int) {
        product.stock += quantity
    }
}
