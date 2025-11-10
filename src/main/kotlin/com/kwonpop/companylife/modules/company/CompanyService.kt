package com.kwonpop.companylife.modules.company

import com.kwonpop.companylife.common.persistence.BranchEntity
import com.kwonpop.companylife.common.persistence.CompanyEntity
import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.store.SaleResult
import com.kwonpop.companylife.modules.store.StoreCatalogItem
import com.kwonpop.companylife.modules.store.StoreInventorySnapshot
import com.kwonpop.companylife.modules.store.StoreOpenResult
import com.kwonpop.companylife.modules.store.StorePerformance
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import java.util.concurrent.CompletableFuture

class CompanyService(
    private val companyRepository: CompanyGateway,
    private val branchRepository: BranchGateway,
    private val ledgerService: LedgerService,
    private val storeService: StoreService,
    private val warehouseService: WarehouseService,
    private val eventBus: EventBus
) {

    fun createCompany(name: String, type: String, regNo: String): CompletableFuture<CompanyEntity> {
        val entity = CompanyEntity(name = name, type = type, regNo = regNo, reputation = 0.5)
        return companyRepository.create(entity).thenApply { company ->
            eventBus.publish(CompanyCreatedEvent(company))
            company
        }
    }

    fun createBranch(companyId: Long, world: String, x: Double, y: Double, z: Double): CompletableFuture<BranchEntity> {
        val entity = BranchEntity(
            companyId = companyId,
            world = world,
            x = x,
            y = y,
            z = z,
            rent = 500.0,
            upkeep = 100.0
        )
        return branchRepository.create(entity).thenApply { branch ->
            eventBus.publish(BranchCreatedEvent(branch))
            branch
        }
    }

    fun openStore(companyId: Long, branchId: Long): CompletableFuture<StoreOpenResult> = storeService.openStore(companyId, branchId)

    fun registerProduct(
        branchId: Long,
        sku: String,
        name: String,
        cost: Double,
        price: Double,
        reorderPoint: Int
    ): StoreCatalogItem = storeService.defineProduct(branchId, sku, name, cost, price, reorderPoint)

    fun receiveIntoWarehouse(
        branchId: Long,
        product: String,
        quantity: Int,
        cost: Double,
        holdingCost: Double,
        demand: Int
    ) = warehouseService.receive(branchId, product, quantity, cost, holdingCost, demand)

    fun moveWarehouseStockToStore(branchId: Long, product: String, quantity: Int): CompletableFuture<StoreInventorySnapshot> {
        return warehouseService.allocateToStore(branchId, product, quantity).thenApply { allocation ->
            if (allocation.fulfilled == 0) {
                throw IllegalStateException("Warehouse cannot fulfil $quantity units of $product")
            }
            val anyLot = allocation.lots.firstOrNull()
            val unitCost = allocation.lots.sumOf { it.unitCost * it.quantity } / allocation.fulfilled
            storeService.receiveShipment(
                branchId,
                product,
                allocation.fulfilled,
                unitCost,
                anyLot?.expireAt
            )
        }
    }

    fun processSale(companyId: Long, branchId: Long, sku: String, quantity: Int): CompletableFuture<SalePostedEvent> {
        val sale = storeService.sell(branchId, sku, quantity)
        val revenueEntry = LedgerEntryEntity(
            companyId = companyId,
            entryAt = sale.soldAt,
            debitAccount = "ACCOUNTS_RECEIVABLE",
            creditAccount = "SALES_REVENUE",
            amount = sale.revenue,
            memo = "Sale of $sku (${sale.quantity})"
        )
        val cogsEntry = LedgerEntryEntity(
            companyId = companyId,
            entryAt = sale.soldAt,
            debitAccount = "COST_OF_GOODS_SOLD",
            creditAccount = "INVENTORY",
            amount = sale.cogs,
            memo = "COGS for $sku"
        )
        return ledgerService.append(revenueEntry).thenCompose {
            ledgerService.append(cogsEntry)
        }.thenApply {
            val posted = SalePostedEvent(companyId, branchId, sale)
            eventBus.publish(posted)
            posted
        }
    }

    fun evaluateStore(branchId: Long): StorePerformance = storeService.evaluatePerformance(branchId)

    fun warehouseAudit(branchId: Long, product: String) = warehouseService.audit(branchId, product)
}

data class CompanyCreatedEvent(val company: CompanyEntity)
data class BranchCreatedEvent(val branch: BranchEntity)
data class SalePostedEvent(val companyId: Long, val branchId: Long, val sale: SaleResult)

fun interface CompanyGateway {
    fun create(entity: CompanyEntity): CompletableFuture<CompanyEntity>
}

fun interface BranchGateway {
    fun create(entity: BranchEntity): CompletableFuture<BranchEntity>
}
