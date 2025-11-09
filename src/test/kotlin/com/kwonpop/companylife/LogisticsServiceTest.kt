package com.kwonpop.companylife

import com.kwonpop.companylife.modules.logistics.LogisticsService
import com.kwonpop.companylife.modules.logistics.RoutePlan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CompletableFuture

class LogisticsServiceTest {
    @Test
    fun `planRoute returns optimal path`() {
        val service = LogisticsService(FakeLogisticsRepo(), com.kwonpop.companylife.common.service.EventBus())
        val graph = mapOf(
            "A" to mapOf("B" to 2.0, "C" to 5.0),
            "B" to mapOf("C" to 1.0, "D" to 4.0),
            "C" to mapOf("D" to 1.0),
            "D" to emptyMap()
        )
        val plan = service.planRoute(graph, "A", "D")
        assertEquals(listOf("A", "B", "C", "D"), plan.nodes)
        assertEquals(4.0, plan.cost)
    }

    private class FakeLogisticsRepo : com.kwonpop.companylife.modules.logistics.LogisticsGateway {
        override fun create(job: com.kwonpop.companylife.common.persistence.LogisticsJobEntity): CompletableFuture<com.kwonpop.companylife.common.persistence.LogisticsJobEntity> {
            return CompletableFuture.completedFuture(job)
        }
    }
}
