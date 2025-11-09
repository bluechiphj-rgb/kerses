package com.kwonpop.companylife.modules.warehouse

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class WarehouseService {
    private val inventory = ConcurrentHashMap<String, ItemStock>()

    fun receive(product: String, quantity: Int, cost: Double, holdingCost: Double, demand: Int): CompletableFuture<ItemStock> {
        val eoq = calculateEoq(demand.toDouble(), cost, holdingCost)
        val stock = inventory.compute(product) { _, existing ->
            val current = existing ?: ItemStock(product, 0, eoq)
            current.copy(quantity = current.quantity + quantity, recommendedOrder = eoq)
        } ?: ItemStock(product, quantity, eoq)
        return CompletableFuture.completedFuture(stock)
    }

    fun pick(product: String, quantity: Int): Boolean {
        val stock = inventory[product] ?: return false
        if (stock.quantity < quantity) return false
        inventory[product] = stock.copy(quantity = stock.quantity - quantity)
        return true
    }

    private fun calculateEoq(demand: Double, orderCost: Double, holdingCost: Double): Int {
        if (demand <= 0 || orderCost <= 0 || holdingCost <= 0) return 0
        val eoq = sqrt((2 * demand * orderCost) / holdingCost)
        return eoq.toInt().coerceAtLeast(1)
    }
}

data class ItemStock(val product: String, val quantity: Int, val recommendedOrder: Int)
