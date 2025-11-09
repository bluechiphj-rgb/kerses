package com.kwonpop.companylife.modules.tax

import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerGateway
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class TaxService(private val ledgerRepository: LedgerGateway, private val eventBus: EventBus) {
    fun calculatePayrollTax(companyId: Long, gross: Double): Double {
        val tax = gross * 0.12
        eventBus.publish(PayrollTaxCalculatedEvent(companyId, gross, tax))
        return tax
    }

    fun settleCorporateTax(companyId: Long, period: String): CompletableFuture<TaxSettlementResult> {
        val now = Instant.now()
        val start = now.minusSeconds(7 * 24 * 3600)
        return ledgerRepository.sumForPeriod(companyId, "SALES_REVENUE", start, now).thenApply { revenue ->
            val tax = revenue * 0.2
            val result = TaxSettlementResult(companyId, period, tax, LocalDate.now().plusDays(5))
            eventBus.publish(CorporateTaxSettledEvent(result))
            result
        }
    }
}

data class TaxSettlementResult(val companyId: Long, val period: String, val amount: Double, val dueDate: LocalDate)

data class PayrollTaxCalculatedEvent(val companyId: Long, val gross: Double, val tax: Double)
data class CorporateTaxSettledEvent(val result: TaxSettlementResult)
