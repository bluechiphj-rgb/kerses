package com.kwonpop.companylife

import com.kwonpop.companylife.common.persistence.BranchEntity
import com.kwonpop.companylife.common.persistence.CompanyEntity
import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.modules.company.BranchGateway
import com.kwonpop.companylife.modules.company.CompanyGateway
import com.kwonpop.companylife.modules.company.CompanyService
import com.kwonpop.companylife.modules.core.ScenarioOrchestrator
import com.kwonpop.companylife.modules.economy.LedgerGateway
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.logistics.LogisticsGateway
import com.kwonpop.companylife.modules.logistics.LogisticsService
import com.kwonpop.companylife.modules.payroll.AuditRecorder
import com.kwonpop.companylife.modules.payroll.PayrollBank
import com.kwonpop.companylife.modules.payroll.PayrollGateway
import com.kwonpop.companylife.modules.payroll.PayrollService
import com.kwonpop.companylife.modules.procurement.ProcurementGateway
import com.kwonpop.companylife.modules.procurement.ProcurementService
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.tax.TaxService
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CompletableFuture

class ScenarioIntegrationTest {
    private lateinit var registry: ServiceRegistry
    private lateinit var ledger: FakeLedgerGateway

    @BeforeEach
    fun setup() {
        registry = ServiceRegistry()
        val eventBus = EventBus()
        ledger = FakeLedgerGateway()

        val store = StoreService(eventBus)
        val warehouse = WarehouseService()
        val ledgerService = LedgerService(ledger)
        val taxService = TaxService(ledger, eventBus)
        val procurement = ProcurementService(FakeProcurementGateway(), eventBus)
        val logistics = LogisticsService(FakeLogisticsGateway(), eventBus)
        val payroll = PayrollService(
            FakePayrollGateway(),
            ledgerService,
            taxService,
            AuditRecorder { _, _, _, _ -> },
            PayrollBank { _, _ -> },
            eventBus
        )
        val company = CompanyService(
            FakeCompanyGateway(),
            FakeBranchGateway(),
            ledgerService,
            store,
            warehouse,
            eventBus
        )

        registry.register(eventBus)
        registry.register(store)
        registry.register(warehouse)
        registry.register(ledgerService)
        registry.register(taxService)
        registry.register(procurement)
        registry.register(logistics)
        registry.register(payroll)
        registry.register(company)
    }

    @Test
    fun `sample scenario completes`() {
        val orchestrator = ScenarioOrchestrator(registry)
        val result = orchestrator.runSample().get()
        assertTrue(result.contains("Scenario complete"))
        assertTrue(ledger.entries.isNotEmpty())
    }

    private class FakeCompanyGateway : CompanyGateway {
        override fun create(entity: CompanyEntity): CompletableFuture<CompanyEntity> {
            return CompletableFuture.completedFuture(entity.copy(id = 1L))
        }
    }

    private class FakeBranchGateway : BranchGateway {
        override fun create(entity: BranchEntity): CompletableFuture<BranchEntity> {
            return CompletableFuture.completedFuture(entity.copy(id = 1L))
        }
    }

    private class FakeLedgerGateway : LedgerGateway {
        val entries = mutableListOf<LedgerEntryEntity>()
        override fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> {
            entries += entry
            return CompletableFuture.completedFuture(entry)
        }

        override fun sumForPeriod(companyId: Long, account: String, periodStart: Instant, periodEnd: Instant): CompletableFuture<Double> {
            return CompletableFuture.completedFuture(1000.0)
        }
    }

    private class FakePayrollGateway : PayrollGateway {
        override fun record(run: com.kwonpop.companylife.common.persistence.PayrollRunEntity): CompletableFuture<com.kwonpop.companylife.common.persistence.PayrollRunEntity> {
            return CompletableFuture.completedFuture(run.copy(id = 1L))
        }
    }

    private class FakeProcurementGateway : ProcurementGateway {
        override fun save(bid: com.kwonpop.companylife.common.persistence.BidEntity): CompletableFuture<com.kwonpop.companylife.common.persistence.BidEntity> {
            return CompletableFuture.completedFuture(bid.copy(id = 1L))
        }
    }

    private class FakeLogisticsGateway : LogisticsGateway {
        override fun create(job: com.kwonpop.companylife.common.persistence.LogisticsJobEntity): CompletableFuture<com.kwonpop.companylife.common.persistence.LogisticsJobEntity> {
            return CompletableFuture.completedFuture(job.copy(id = 1L))
        }
    }
}
