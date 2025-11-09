package com.kwonpop.companylife.modules.payroll

import com.kwonpop.companylife.common.persistence.PayrollRunEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.tax.TaxService
import com.kwonpop.companylife.common.integration.VaultBridge
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface PayrollGateway {
    fun record(run: PayrollRunEntity): CompletableFuture<PayrollRunEntity>
}

class PayrollService(
    private val payrollRepository: PayrollGateway,
    private val ledgerService: LedgerService,
    private val taxService: TaxService,
    private val auditService: AuditRecorder,
    private val payrollBank: PayrollBank,
    private val eventBus: EventBus
) {
    fun runPayroll(companyId: Long, employees: List<PayrollEmployee>, period: String): CompletableFuture<PayrollRunEntity> {
        val gross = employees.sumOf { it.grossPay }
        val tax = taxService.calculatePayrollTax(companyId, gross)
        val net = gross - tax

        employees.forEach { employee ->
            payrollBank.deposit(employee.uuid, employee.grossPay - tax / employees.size)
        }

        val run = PayrollRunEntity(
            companyId = companyId,
            period = period,
            gross = gross,
            net = net,
            processedAt = Instant.now()
        )

        val ledgerFuture = ledgerService.append(
            run.toLedgerEntry(tax = tax)
        )

        return ledgerFuture.thenCompose {
            payrollRepository.record(run)
        }.thenApply { saved ->
            auditService.record(UUID.randomUUID(), "payroll_run", "{\"company\":$companyId,\"period\":\"$period\"}")
            val event = PayrollProcessedEvent(companyId, period, gross, net)
            eventBus.publish(event)
            saved
        }
    }
}

data class PayrollEmployee(val uuid: UUID, val grossPay: Double)

data class PayrollProcessedEvent(val companyId: Long, val period: String, val gross: Double, val net: Double)

fun interface AuditRecorder {
    fun record(actor: UUID, action: String, detailJson: String, signature: String? = null)
}

fun interface PayrollBank {
    fun deposit(uuid: UUID, amount: Double)
}

private fun PayrollRunEntity.toLedgerEntry(tax: Double) = com.kwonpop.companylife.common.persistence.LedgerEntryEntity(
    companyId = companyId,
    entryAt = processedAt,
    debitAccount = "PAYROLL_EXPENSE",
    creditAccount = "CASH",
    amount = net,
    memo = "Payroll run $period (tax=$tax)"
)
