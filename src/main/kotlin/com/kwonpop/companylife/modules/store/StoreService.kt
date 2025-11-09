package com.kwonpop.companylife.modules.store

import com.kwonpop.companylife.common.service.EventBus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class StoreService(private val eventBus: EventBus) {
    private val stores = ConcurrentHashMap<Long, Store>()

    fun openStore(companyId: Long, branchId: Long): CompletableFuture<StoreOpenEvent> {
        val store = Store(companyId, branchId)
        stores[branchId] = store
        val event = StoreOpenEvent(companyId, branchId)
        eventBus.publish(event)
        return CompletableFuture.completedFuture(event)
    }

    fun addInventory(branchId: Long, sku: String, qty: Int) {
        stores[branchId]?.inventory?.merge(sku, qty) { old, inc -> old + inc }
    }

    fun sell(branchId: Long, sku: String, qty: Int): Boolean {
        val store = stores[branchId] ?: return false
        val current = store.inventory.getOrDefault(sku, 0)
        if (current < qty) return false
        store.inventory[sku] = current - qty
        return true
    }
}

data class Store(val companyId: Long, val branchId: Long, val inventory: MutableMap<String, Int> = ConcurrentHashMap())
data class StoreOpenEvent(val companyId: Long, val branchId: Long)
