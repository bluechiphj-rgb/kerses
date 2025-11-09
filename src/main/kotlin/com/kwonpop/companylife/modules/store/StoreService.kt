package com.kwonpop.companylife.modules.store

import com.kwonpop.companylife.common.service.EventBus
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class StoreService(private val eventBus: EventBus) {
    private val stores = ConcurrentHashMap<Long, StoreState>()

    fun openStore(companyId: Long, branchId: Long): CompletableFuture<StoreOpenResult> {
        val state = stores.compute(branchId) { _, existing ->
            val base = existing ?: StoreState(companyId = companyId, branchId = branchId)
            base.copy(companyId = companyId)
        } ?: StoreState(companyId, branchId)
        val result = StoreOpenResult(
            companyId = state.companyId,
            branchId = state.branchId,
            catalogSize = state.catalog.size,
            totalStock = state.totalStock()
        )
        eventBus.publish(StoreOpenedEvent(result))
        return CompletableFuture.completedFuture(result)
    }

    fun defineProduct(
        branchId: Long,
        sku: String,
        name: String,
        cost: Double,
        price: Double,
        reorderPoint: Int,
        promotionMultiplier: Double = 1.0
    ): StoreCatalogItem {
        require(price > 0) { "Price must be positive" }
        require(cost >= 0) { "Cost cannot be negative" }
        val state = stores[branchId] ?: throw IllegalStateException("Store not opened for branch $branchId")
        val item = StoreCatalogItem(sku, name, cost, price, promotionMultiplier, reorderPoint)
        state.catalog[sku] = item
        return item
    }

    fun receiveShipment(
        branchId: Long,
        sku: String,
        quantity: Int,
        unitCost: Double,
        expireAt: Instant?
    ): StoreInventorySnapshot {
        require(quantity > 0) { "Quantity must be positive" }
        val state = stores[branchId] ?: throw IllegalStateException("Store not opened for branch $branchId")
        val batches = state.inventory.computeIfAbsent(sku) { mutableListOf() }
        batches += InventoryBatch(quantity, unitCost, expireAt, Instant.now())
        batches.sortBy { it.expireAt ?: Instant.MAX }
        val snapshot = state.snapshotOf(sku)
        if (snapshot.onHand <= state.catalog[sku]?.reorderPoint ?: 0) {
            eventBus.publish(LowInventoryEvent(state.companyId, branchId, sku, snapshot.onHand))
        }
        return snapshot
    }

    fun sell(branchId: Long, sku: String, quantity: Int): SaleResult {
        require(quantity > 0) { "Quantity must be positive" }
        val state = stores[branchId] ?: throw IllegalStateException("Store not opened for branch $branchId")
        val catalogItem = state.catalog[sku] ?: throw IllegalArgumentException("Unknown SKU $sku")
        val batches = state.inventory[sku] ?: throw IllegalStateException("No inventory for $sku")

        var remaining = quantity
        val consumed = mutableListOf<InventoryBatch>()
        val updated = mutableListOf<InventoryBatch>()
        for (batch in batches) {
            if (remaining <= 0) {
                updated += batch
                continue
            }
            val take = minOf(batch.quantity, remaining)
            if (take > 0) {
                consumed += batch.copy(quantity = take)
                val leftover = batch.quantity - take
                if (leftover > 0) {
                    updated += batch.copy(quantity = leftover)
                }
            } else {
                updated += batch
            }
            remaining -= take
        }
        if (remaining > 0) {
            throw IllegalStateException("Insufficient inventory for $sku")
        }
        state.inventory[sku] = updated

        val unitPrice = catalogItem.price * catalogItem.promotionMultiplier
        val revenue = unitPrice * quantity
        val cogs = consumed.sumOf { it.unitCost * it.quantity }
        val margin = if (revenue == 0.0) 0.0 else (revenue - cogs) / revenue
        val onHand = updated.sumOf { it.quantity }
        val reorder = onHand <= catalogItem.reorderPoint

        val sale = SaleResult(
            companyId = state.companyId,
            branchId = branchId,
            sku = sku,
            quantity = quantity,
            revenue = revenue,
            cogs = cogs,
            margin = margin,
            onHand = onHand,
            needsReorder = reorder,
            soldAt = Instant.now()
        )
        state.sales.add(sale)
        eventBus.publish(SaleCompletedEvent(sale))
        if (reorder) {
            eventBus.publish(LowInventoryEvent(state.companyId, branchId, sku, onHand))
        }
        return sale
    }

    fun evaluatePerformance(branchId: Long): StorePerformance {
        val state = stores[branchId] ?: throw IllegalStateException("Store not opened for branch $branchId")
        val trailingSales = state.sales.filter { it.soldAt.isAfter(Instant.now().minusSeconds(7 * 24 * 3600)) }
        val revenue = trailingSales.sumOf { it.revenue }
        val cogs = trailingSales.sumOf { it.cogs }
        val avgMargin = if (revenue == 0.0) 0.0 else (revenue - cogs) / revenue
        val lowStockSkus = state.catalog.keys.filter { sku ->
            val onHand = state.inventory[sku]?.sumOf { it.quantity } ?: 0
            onHand <= (state.catalog[sku]?.reorderPoint ?: 0)
        }
        return StorePerformance(revenue, avgMargin, lowStockSkus)
    }
}

data class StoreOpenResult(val companyId: Long, val branchId: Long, val catalogSize: Int, val totalStock: Int)

data class StoreCatalogItem(
    val sku: String,
    val name: String,
    val cost: Double,
    val price: Double,
    val promotionMultiplier: Double,
    val reorderPoint: Int
)

data class StorePerformance(val trailingRevenue: Double, val averageMargin: Double, val lowStockSkus: List<String>)

data class SaleResult(
    val companyId: Long,
    val branchId: Long,
    val sku: String,
    val quantity: Int,
    val revenue: Double,
    val cogs: Double,
    val margin: Double,
    val onHand: Int,
    val needsReorder: Boolean,
    val soldAt: Instant
)

data class StoreInventorySnapshot(val sku: String, val onHand: Int, val batches: List<InventoryBatch>)

data class InventoryBatch(val quantity: Int, val unitCost: Double, val expireAt: Instant?, val receivedAt: Instant)

data class StoreOpenedEvent(val result: StoreOpenResult)

data class SaleCompletedEvent(val result: SaleResult)

data class LowInventoryEvent(val companyId: Long, val branchId: Long, val sku: String, val onHand: Int)

private data class StoreState(
    val companyId: Long,
    val branchId: Long,
    val catalog: MutableMap<String, StoreCatalogItem> = ConcurrentHashMap(),
    val inventory: MutableMap<String, MutableList<InventoryBatch>> = ConcurrentHashMap(),
    val sales: MutableList<SaleResult> = mutableListOf()
) {
    fun totalStock(): Int = inventory.values.sumOf { list -> list.sumOf { it.quantity } }

    fun snapshotOf(sku: String): StoreInventorySnapshot {
        val batches = inventory[sku]?.toList() ?: emptyList()
        val onHand = batches.sumOf { it.quantity }
        return StoreInventorySnapshot(sku, onHand, batches)
    }
}
