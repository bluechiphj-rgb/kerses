package com.kwonpop.companylife

import com.kwonpop.companylife.common.config.AutoBuildConfig
import com.kwonpop.companylife.common.config.BranchBlueprintConfig
import com.kwonpop.companylife.common.config.CompanyBlueprintConfig
import com.kwonpop.companylife.common.config.ProductBlueprintConfig
import com.kwonpop.companylife.common.config.RootConfig
import com.kwonpop.companylife.common.persistence.BranchEntity
import com.kwonpop.companylife.common.persistence.CompanyEntity
import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.company.BranchGateway
import com.kwonpop.companylife.modules.company.CompanyAutoBuilder
import com.kwonpop.companylife.modules.company.CompanyGateway
import com.kwonpop.companylife.modules.company.CompanyService
import com.kwonpop.companylife.modules.economy.LedgerGateway
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CompanyAutoBuilderTest {

    @Test
    fun `autobuild scaffolds company branches and inventory`() {
        val companyGateway = InMemoryCompanyGateway()
        val branchGateway = InMemoryBranchGateway()
        val ledgerService = LedgerService(NoopLedgerGateway())
        val eventBus = EventBus()
        val storeService = StoreService(eventBus)
        val warehouseService = WarehouseService()

        val companyService = CompanyService(
            companyGateway,
            branchGateway,
            ledgerService,
            storeService,
            warehouseService,
            eventBus
        )

        val blueprint = CompanyBlueprintConfig(
            key = "starter",
            displayName = "Starter Incorporated",
            type = "LLC",
            branches = listOf(
                BranchBlueprintConfig("hq", "world", 0.0, 64.0, 0.0, openStore = true)
            ),
            inventory = listOf(
                ProductBlueprintConfig(
                    branch = "hq",
                    sku = "starter_widget",
                    name = "Starter Widget",
                    cost = 320.0,
                    price = 520.0,
                    reorderPoint = 40,
                    initialWarehouseStock = 100,
                    initialStoreStock = 40,
                    holdingCost = 6.0,
                    orderCost = 240.0,
                    expectedDemand = 900,
                    leadTimeDays = 3
                )
            )
        )

        val config = RootConfig(
            autobuild = AutoBuildConfig(
                enabled = true,
                blueprints = listOf(blueprint)
            )
        )

        val builder = CompanyAutoBuilder({ config }, companyService)

        val summary = builder.autoBuild("starter", "Custom Starter").join()

        assertEquals("Custom Starter", summary.companyName)
        assertEquals(1, summary.branchCount)
        assertEquals(1, summary.productCount)
        val branchId = summary.branchIds["hq"] ?: fail("branch id missing")

        val audit = companyService.warehouseAudit(branchId, "starter_widget")
        assertEquals(60, audit.onHand, "warehouse should retain remainder after store allocation")

        val snapshot = companyService.moveWarehouseStockToStore(branchId, "starter_widget", 10).join()
        assertEquals(50, snapshot.onHand, "store inventory should include initial and replenished stock")
        val afterMoveAudit = companyService.warehouseAudit(branchId, "starter_widget")
        assertEquals(50, afterMoveAudit.onHand, "warehouse inventory should decrease after replenishment")

        val blueprints = builder.listBlueprintKeys()
        assertTrue(blueprints.contains("starter"))
    }

    private class InMemoryCompanyGateway : CompanyGateway {
        private val sequence = AtomicLong(1)
        override fun create(entity: CompanyEntity): CompletableFuture<CompanyEntity> {
            val created = entity.copy(id = sequence.getAndIncrement())
            return CompletableFuture.completedFuture(created)
        }
    }

    private class InMemoryBranchGateway : BranchGateway {
        private val sequence = AtomicLong(1)
        override fun create(entity: BranchEntity): CompletableFuture<BranchEntity> {
            val created = entity.copy(id = sequence.getAndIncrement())
            return CompletableFuture.completedFuture(created)
        }
    }

    private class NoopLedgerGateway : LedgerGateway {
        override fun append(entry: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> =
            CompletableFuture.completedFuture(entry.copy(id = 1L))

        override fun sumForPeriod(
            companyId: Long,
            account: String,
            periodStart: Instant,
            periodEnd: Instant
        ): CompletableFuture<Double> = CompletableFuture.completedFuture(0.0)
    }
}
