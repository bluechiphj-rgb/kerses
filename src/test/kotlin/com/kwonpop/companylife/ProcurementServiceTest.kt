package com.kwonpop.companylife

import com.kwonpop.companylife.common.persistence.BidEntity
import com.kwonpop.companylife.modules.procurement.ProcurementService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ProcurementServiceTest {
    @Test
    fun `score favors better bids`() {
        val service = ProcurementService(FakeRepo(), com.kwonpop.companylife.common.service.EventBus())
        val lowScore = service.scoreBid(1, 1, price = 1500.0, leadTime = 6, quality = 4.0)
        val highScore = service.scoreBid(1, 2, price = 900.0, leadTime = 2, quality = 8.0)
        assertTrue(highScore > lowScore)
    }

    private class FakeRepo : com.kwonpop.companylife.modules.procurement.ProcurementGateway {
        override fun save(bid: BidEntity): CompletableFuture<BidEntity> = CompletableFuture.completedFuture(bid)
    }
}
