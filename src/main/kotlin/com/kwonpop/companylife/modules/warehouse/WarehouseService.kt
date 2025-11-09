package com.kwonpop.companylife.modules.warehouse

import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class WarehouseService {
    private val inventory = ConcurrentHashMap<WarehouseKey, WarehouseSnapshot>()

    fun receive(
        branchId: Long,
        product: String,
        quantity: Int,
        cost: Double,
        holdingCost: Double,
        demand: Int,
        expireAt: Instant? = null,
        leadTimeDays: Int = 3,
        serviceLevel: Double = 0.95
    ): CompletableFuture<WarehouseReceipt> {
        require(quantity > 0) { "Quantity must be positive" }
        require(cost > 0) { "Order cost must be positive" }
        require(holdingCost > 0) { "Holding cost must be positive" }
        require(demand >= 0) { "Demand cannot be negative" }

        val key = WarehouseKey(branchId, product)
        val now = Instant.now()
        val snapshot = inventory.compute(key) { _, existing ->
            val eoq = calculateEoq(demand.toDouble(), cost, holdingCost)
            val safetyStock = calculateSafetyStock(demand, serviceLevel)
            val reorderPoint = calculateReorderPoint(demand, leadTimeDays, safetyStock)
            val lots = existing?.lots ?: mutableListOf()
            lots += InventoryLot(quantity, cost / max(quantity, 1), now, expireAt)
            lots.sortBy { it.expireAt ?: Instant.MAX }
            WarehouseSnapshot(branchId, product, lots, eoq, safetyStock, reorderPoint)
        } ?: error("Inventory snapshot should not be null")

        return CompletableFuture.completedFuture(
            WarehouseReceipt(
                branchId = branchId,
                product = product,
                receivedQuantity = quantity,
                onHand = snapshot.onHand(),
                recommendedOrder = snapshot.recommendedOrder,
                safetyStock = snapshot.safetyStock,
                reorderPoint = snapshot.reorderPoint,
                lots = snapshot.lots.map { it.toView() }
            )
        )
    }

    fun allocateToStore(branchId: Long, product: String, quantity: Int): CompletableFuture<WarehouseAllocation> {
        require(quantity > 0) { "Quantity must be positive" }
        val key = WarehouseKey(branchId, product)
        var pulledLots = listOf<LotAllocation>()
        var remaining = quantity

        val snapshot = inventory.computeIfPresent(key) { _, existing ->
            val updatedLots = mutableListOf<InventoryLot>()
            val allocations = mutableListOf<LotAllocation>()

            for (lot in existing.lots) {
                if (remaining <= 0) {
                    updatedLots += lot
                    continue
                }
                val take = minOf(remaining, lot.quantity)
                if (take > 0) {
                    allocations += LotAllocation(take, lot.unitCost, lot.expireAt)
                    val leftover = lot.quantity - take
                    if (leftover > 0) {
                        updatedLots += lot.copy(quantity = leftover)
                    }
                } else {
                    updatedLots += lot
                }
                remaining -= take
            }

            pulledLots = allocations
            existing.copy(lots = updatedLots)
        }

        val actualSnapshot = snapshot ?: return CompletableFuture.completedFuture(
            WarehouseAllocation(
                branchId = branchId,
                product = product,
                requested = quantity,
                fulfilled = 0,
                lots = emptyList(),
                onHand = 0,
                needsReorder = true
            )
        )

        val fulfilled = pulledLots.sumOf { it.quantity }
        val needsReorder = actualSnapshot.onHand() <= actualSnapshot.reorderPoint

        return CompletableFuture.completedFuture(
            WarehouseAllocation(
                branchId = branchId,
                product = product,
                requested = quantity,
                fulfilled = fulfilled,
                lots = pulledLots,
                onHand = actualSnapshot.onHand(),
                needsReorder = needsReorder
            )
        )
    }

    fun audit(branchId: Long, product: String, now: Instant = Instant.now()): WarehouseAuditReport {
        val snapshot = inventory[WarehouseKey(branchId, product)] ?: return WarehouseAuditReport(product, branchId, 0, 0, emptyList())
        val expiringSoon = snapshot.lots.filter { lot ->
            lot.expireAt?.isBefore(now.plusSeconds(3 * 24 * 3600)) == true
        }.map { it.toView() }
        val expired = snapshot.lots.filter { lot -> lot.expireAt?.isBefore(now) == true }.sumOf { it.quantity }
        return WarehouseAuditReport(
            product = product,
            branchId = branchId,
            onHand = snapshot.onHand(),
            expiredUnits = expired,
            expiringLots = expiringSoon
        )
    }

    private fun calculateEoq(demand: Double, orderCost: Double, holdingCost: Double): Int {
        if (demand <= 0 || orderCost <= 0 || holdingCost <= 0) return 0
        val eoq = sqrt((2 * demand * orderCost) / holdingCost)
        return eoq.roundToInt().coerceAtLeast(1)
    }

    private fun calculateSafetyStock(demand: Int, serviceLevel: Double): Int {
        if (demand <= 0) return 0
        val z = when {
            serviceLevel >= 0.99 -> 2.33
            serviceLevel >= 0.97 -> 1.88
            serviceLevel >= 0.95 -> 1.65
            serviceLevel >= 0.9 -> 1.28
            else -> 0.84
        }
        return ceil(z * sqrt(max(demand, 1).toDouble())).toInt()
    }

    private fun calculateReorderPoint(demand: Int, leadTimeDays: Int, safetyStock: Int): Int {
        if (demand <= 0) return safetyStock
        val dailyDemand = demand / 30.0
        return ceil(dailyDemand * leadTimeDays + safetyStock).toInt()
    }
}

data class WarehouseReceipt(
    val branchId: Long,
    val product: String,
    val receivedQuantity: Int,
    val onHand: Int,
    val recommendedOrder: Int,
    val safetyStock: Int,
    val reorderPoint: Int,
    val lots: List<InventoryLotView>
)

data class WarehouseAllocation(
    val branchId: Long,
    val product: String,
    val requested: Int,
    val fulfilled: Int,
    val lots: List<LotAllocation>,
    val onHand: Int,
    val needsReorder: Boolean
)

data class WarehouseAuditReport(
    val product: String,
    val branchId: Long,
    val onHand: Int,
    val expiredUnits: Int,
    val expiringLots: List<InventoryLotView>
)

data class LotAllocation(val quantity: Int, val unitCost: Double, val expireAt: Instant?)

data class InventoryLotView(val quantity: Int, val unitCost: Double, val receivedAt: Instant, val expireAt: Instant?)

private data class WarehouseKey(val branchId: Long, val product: String)

private data class WarehouseSnapshot(
    val branchId: Long,
    val product: String,
    val lots: MutableList<InventoryLot>,
    val recommendedOrder: Int,
    val safetyStock: Int,
    val reorderPoint: Int
) {
    fun onHand(): Int = lots.sumOf { it.quantity }
}

private data class InventoryLot(
    val quantity: Int,
    val unitCost: Double,
    val receivedAt: Instant,
    val expireAt: Instant?
) {
    fun toView() = InventoryLotView(quantity, unitCost, receivedAt, expireAt)
}
