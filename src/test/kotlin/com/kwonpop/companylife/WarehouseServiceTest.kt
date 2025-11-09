package com.kwonpop.companylife

import com.kwonpop.companylife.modules.warehouse.WarehouseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WarehouseServiceTest {
    private val service = WarehouseService()

    @Test
    fun `calculate EOQ updates recommended order`() {
        val stock = service.receive("iron", 100, cost = 200.0, holdingCost = 5.0, demand = 1000).get()
        assertEquals(100, stock.quantity)
        assertEquals(200, stock.recommendedOrder)
    }
}
