package com.kwonpop.companylife.modules.payroll

import com.kwonpop.companylife.common.persistence.PayrollRunEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.tax.PayrollTaxBreakdown
import com.kwonpop.companylife.modules.tax.TaxService
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
        require(employees.isNotEmpty()) { "Employees required for payroll" }
        val grossList = employees.map { it.grossPay() }
        val gross = grossList.sum()
        val breakdown = taxService.calculatePayrollTax(companyId, grossList)
        val net = gross - breakdown.total

        employees.forEachIndexed { index, employee ->
            val grossPay = grossList[index]
            val taxShare = if (gross == 0.0) 0.0 else breakdown.total * (grossPay / gross)
            payrollBank.deposit(employee.uuid, grossPay - taxShare)
        }

        val run = PayrollRunEntity(
            companyId = companyId,
            period = period,
            gross = gross,
            net = net,
            processedAt = Instant.now()
        )

        val ledgerFuture = ledgerService.append(run.toLedgerEntry(breakdown))

        return ledgerFuture.thenCompose {
            payrollRepository.record(run)
        }.thenApply { saved ->
            auditService.record(UUID.randomUUID(), "payroll_run", payrollAuditJson(companyId, period, breakdown))
            val event = PayrollProcessedEvent(companyId, period, gross, net, breakdown)
            eventBus.publish(event)
            saved
        }
    }

    private fun payrollAuditJson(companyId: Long, period: String, breakdown: PayrollTaxBreakdown): String =
        "{" +
            "\"company\":$companyId," +
            "\"period\":\"$period\"," +
            "\"tax_total\":${breakdown.total}" +
        "}"
}

data class PayrollEmployee(
    val uuid: UUID,
    val baseSalary: Double,
    val hoursWorked: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val overtimeMultiplier: Double = 1.5,
    val bonus: Double = 0.0,
    val deductions: Double = 0.0
) {
    fun grossPay(): Double {
        val hourly = hoursWorked * hourlyRate
        val overtime = overtimeHours * hourlyRate * overtimeMultiplier
        return baseSalary + hourly + overtime + bonus - deductions
    }
}

data class PayrollProcessedEvent(
    val companyId: Long,
    val period: String,
    val gross: Double,
    val net: Double,
    val breakdown: PayrollTaxBreakdown
)

fun interface AuditRecorder {
    fun record(actor: UUID, action: String, detailJson: String, signature: String? = null)
}

fun interface PayrollBank {
    fun deposit(uuid: UUID, amount: Double)
}

private fun PayrollRunEntity.toLedgerEntry(breakdown: PayrollTaxBreakdown) = com.kwonpop.companylife.common.persistence.LedgerEntryEntity(
    companyId = companyId,
    entryAt = processedAt,
    debitAccount = "PAYROLL_EXPENSE",
    creditAccount = "CASH",
    amount = net,
    memo = "Payroll run $period (tax=${breakdown.total})"
)
