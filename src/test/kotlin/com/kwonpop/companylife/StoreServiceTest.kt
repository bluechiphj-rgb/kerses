package com.kwonpop.companylife

import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.store.StoreService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class StoreServiceTest {
    private lateinit var service: StoreService

    @BeforeEach
    fun setup() {
        service = StoreService(EventBus())
        service.openStore(1, 10)
        service.defineProduct(10, "sku-1", "Widget", cost = 40.0, price = 100.0, reorderPoint = 20)
        service.receiveShipment(10, "sku-1", 50, unitCost = 40.0, expireAt = Instant.now().plusSeconds(86400))
    }

    @Test
    fun `sell returns margin and triggers reorder`() {
        val result = service.sell(10, "sku-1", 45)
        assertEquals(4500.0, result.revenue, 0.001)
        assertTrue(result.margin > 0.0)
        assertTrue(result.needsReorder)
    }
}
