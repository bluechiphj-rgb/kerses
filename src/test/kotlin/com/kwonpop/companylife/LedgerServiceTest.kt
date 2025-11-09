package com.kwonpop.companylife

import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.modules.economy.LedgerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CompletableFuture

class LedgerServiceTest {
    private lateinit var repository: FakeLedgerRepository
    private lateinit var service: LedgerService

    @BeforeEach
    fun setup() {
        repository = FakeLedgerRepository()
        service = LedgerService(repository)
    }

    @Test
    fun `sumForPeriod calculates revenue`() {
        val now = Instant.now()
        repository.append(
            LedgerEntryEntity(null, 1, now.minusSeconds(3600), "SALES_REVENUE", "CASH", 100.0, "sale"))
        repository.append(
            LedgerEntryEntity(null, 1, now.minusSeconds(1800), "SALES_REVENUE", "CASH", 250.0, "sale"))
        repository.append(
            LedgerEntryEntity(null, 1, now.minusSeconds(7200), "OTHER", "CASH", 300.0, "other"))

        val total = service.revenueForPeriod(1, now.minusSeconds(4000), now).get()
        assertEquals(350.0, total)
    }

    private class FakeLedgerRepository : com.kwonpop.companylife.modules.economy.LedgerGateway {
        private val entries = mutableListOf<LedgerEntryEntity>()

        override fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> {
            val stored = entry.copy(id = (entries.size + 1).toLong())
            entries += stored
            return CompletableFuture.completedFuture(stored)
        }

        override fun sumForPeriod(companyId: Long, account: String, periodStart: Instant, periodEnd: Instant): CompletableFuture<Double> {
            val sum = entries.filter {
                it.companyId == companyId && it.debitAccount == account && !it.entryAt.isBefore(periodStart) && !it.entryAt.isAfter(periodEnd)
            }.sumOf { it.amount }
            return CompletableFuture.completedFuture(sum)
        }
    }
}
