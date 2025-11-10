package com.kwonpop.companylife.modules.economy

import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import java.time.Instant
import java.util.concurrent.CompletableFuture

interface LedgerGateway {
    fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity>
    fun sumForPeriod(companyId: Long, account: String, periodStart: Instant, periodEnd: Instant): CompletableFuture<Double>
}

class LedgerService(private val repository: LedgerGateway) {
    fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> = repository.append(entry)

    fun revenueForPeriod(companyId: Long, start: Instant, end: Instant): CompletableFuture<Double> =
        repository.sumForPeriod(companyId, "SALES_REVENUE", start, end)
}
