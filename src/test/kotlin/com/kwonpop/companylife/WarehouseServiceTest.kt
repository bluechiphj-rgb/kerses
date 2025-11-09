package com.kwonpop.companylife

import com.kwonpop.companylife.modules.warehouse.WarehouseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class WarehouseServiceTest {
    private val service = WarehouseService()

    @Test
    fun `receive calculates EOQ and safety stock`() {
        val receipt = service.receive(1, "iron", 100, cost = 200.0, holdingCost = 5.0, demand = 1000).get()
        assertEquals(100, receipt.onHand)
        assertEquals(283, receipt.recommendedOrder)
        assertTrue(receipt.safetyStock > 0)
    }

    @Test
    fun `allocation reduces inventory`() {
        service.receive(1, "copper", 80, cost = 160.0, holdingCost = 4.0, demand = 600, expireAt = Instant.now().plusSeconds(3600)).get()
        val allocation = service.allocateToStore(1, "copper", 50).get()
        assertEquals(50, allocation.fulfilled)
        assertTrue(allocation.onHand < 80)
    }
}
