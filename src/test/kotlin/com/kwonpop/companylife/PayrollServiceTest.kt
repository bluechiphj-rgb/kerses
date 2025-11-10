package com.kwonpop.companylife

import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.common.persistence.PayrollRunEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerGateway
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.payroll.PayrollEmployee
import com.kwonpop.companylife.modules.payroll.PayrollGateway
import com.kwonpop.companylife.modules.payroll.PayrollService
import com.kwonpop.companylife.modules.payroll.AuditRecorder
import com.kwonpop.companylife.modules.payroll.PayrollBank
import com.kwonpop.companylife.modules.tax.TaxService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PayrollServiceTest {
    private lateinit var payrollGateway: FakePayrollGateway
    private lateinit var ledgerGateway: FakeLedgerGateway
    private lateinit var service: PayrollService

    @BeforeEach
    fun setup() {
        payrollGateway = FakePayrollGateway()
        ledgerGateway = FakeLedgerGateway()
        val eventBus = EventBus()
        val taxService = TaxService(ledgerGateway, eventBus)
        val audit = AuditRecorder { _, _, _, _ -> }
        val bank = FakePayrollBank()
        service = PayrollService(
            payrollGateway,
            LedgerService(ledgerGateway),
            taxService,
            audit,
            bank,
            eventBus
        )
    }

    @Test
    fun `runPayroll handles overtime and bonuses`() {
        val employees = listOf(
            PayrollEmployee(UUID.randomUUID(), baseSalary = 400.0, hoursWorked = 30.0, hourlyRate = 10.0, overtimeHours = 6.0),
            PayrollEmployee(UUID.randomUUID(), baseSalary = 550.0, bonus = 150.0, deductions = 50.0)
        )
        val result = service.runPayroll(1, employees, "W1").get()
        assertEquals(1440.0, result.gross, 0.001)
        assertEquals(1180.0, result.net, 0.001)
        assertEquals(1, payrollGateway.runs.size)
    }

    private class FakePayrollGateway : PayrollGateway {
        val runs = mutableListOf<PayrollRunEntity>()
        override fun record(run: PayrollRunEntity): CompletableFuture<PayrollRunEntity> {
            val stored = run.copy(id = (runs.size + 1).toLong())
            runs += stored
            return CompletableFuture.completedFuture(stored)
        }
    }

    private class FakeLedgerGateway : LedgerGateway {
        val entries = mutableListOf<LedgerEntryEntity>()
        override fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> {
            entries += entry
            return CompletableFuture.completedFuture(entry)
        }

        override fun sumForPeriod(companyId: Long, account: String, periodStart: Instant, periodEnd: Instant): CompletableFuture<Double> {
            return CompletableFuture.completedFuture(0.0)
        }
    }

    private class FakePayrollBank : PayrollBank {
        val deposits = mutableMapOf<UUID, Double>()
        override fun deposit(uuid: UUID, amount: Double) {
            deposits[uuid] = deposits.getOrDefault(uuid, 0.0) + amount
        }
    }
}
