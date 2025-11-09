package com.kwonpop.companylife.modules.procurement

import com.kwonpop.companylife.common.persistence.BidEntity
import com.kwonpop.companylife.common.service.EventBus
import java.util.concurrent.CompletableFuture

interface ProcurementGateway {
    fun save(bid: BidEntity): CompletableFuture<BidEntity>
}

class ProcurementService(
    private val repository: ProcurementGateway,
    private val eventBus: EventBus
) {
    fun scoreBid(tenderId: Long, companyId: Long, price: Double, leadTime: Int, quality: Double): Double {
        val priceScore = 60 - price / 1000
        val timeScore = 30 - leadTime
        val qualityScore = 10 + quality
        val score = (priceScore * 0.5) + (timeScore * 0.3) + (qualityScore * 0.2)
        eventBus.publish(BidScoredEvent(tenderId, companyId, score))
        return score
    }

    fun submitBid(tenderId: Long, companyId: Long, price: Double, leadTime: Int, quality: Double): CompletableFuture<BidEntity> {
        val score = scoreBid(tenderId, companyId, price, leadTime, quality)
        val entity = BidEntity(tenderId = tenderId, companyId = companyId, price = price, leadTime = leadTime, score = score)
        return repository.save(entity).thenApply { saved ->
            eventBus.publish(BidSubmittedEvent(saved))
            saved
        }
    }
}

data class BidScoredEvent(val tenderId: Long, val companyId: Long, val score: Double)
data class BidSubmittedEvent(val bid: BidEntity)
