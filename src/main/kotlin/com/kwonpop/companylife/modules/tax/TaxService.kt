package com.kwonpop.companylife.modules.tax

import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerGateway
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import kotlin.math.max

class TaxService(private val ledgerRepository: LedgerGateway, private val eventBus: EventBus) {
    fun calculatePayrollTax(companyId: Long, grossPays: List<Double>): PayrollTaxBreakdown {
        val federal = grossPays.sumOf { progressiveRate(it) }
        val state = grossPays.sumOf { it * 0.035 }
        val insurance = grossPays.sumOf { max(0.0, it) * 0.015 }
        val breakdown = PayrollTaxBreakdown(federal, state, insurance)
        eventBus.publish(PayrollTaxCalculatedEvent(companyId, grossPays.sum(), breakdown))
        return breakdown
    }

    fun settleCorporateTax(companyId: Long, period: String): CompletableFuture<TaxSettlementResult> {
        val now = Instant.now()
        val start = now.minusSeconds(7 * 24 * 3600)
        return ledgerRepository.sumForPeriod(companyId, "SALES_REVENUE", start, now).thenApply { revenue ->
            val tax = revenue * 0.2
            val result = TaxSettlementResult(
                companyId = companyId,
                period = period,
                amount = tax,
                dueDate = LocalDate.now().plusDays(5),
                revenue = revenue,
                effectiveRate = if (revenue == 0.0) 0.0 else tax / revenue
            )
            eventBus.publish(CorporateTaxSettledEvent(result))
            result
        }
    }

    private fun progressiveRate(gross: Double): Double {
        if (gross <= 0) return 0.0
        val base = minOf(500.0, gross) * 0.1
        val remainder = max(0.0, gross - 500.0)
        val tierTwo = minOf(1000.0, remainder) * 0.2
        val tierThree = max(0.0, remainder - 1000.0) * 0.28
        return base + tierTwo + tierThree
    }
}

data class PayrollTaxBreakdown(val federal: Double, val state: Double, val insurance: Double) {
    val total: Double = federal + state + insurance
}

data class TaxSettlementResult(
    val companyId: Long,
    val period: String,
    val amount: Double,
    val dueDate: LocalDate,
    val revenue: Double,
    val effectiveRate: Double
)

data class PayrollTaxCalculatedEvent(val companyId: Long, val gross: Double, val breakdown: PayrollTaxBreakdown)
data class CorporateTaxSettledEvent(val result: TaxSettlementResult)
