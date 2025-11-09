package com.kwonpop.companylife.modules.company

import com.kwonpop.companylife.common.persistence.BranchEntity
import com.kwonpop.companylife.common.persistence.CompanyEntity
import com.kwonpop.companylife.common.persistence.LedgerEntryEntity
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.store.StoreService
import java.time.Instant
import java.util.concurrent.CompletableFuture

class CompanyService(
    private val companyRepository: CompanyGateway,
    private val branchRepository: BranchGateway,
    private val ledgerService: LedgerService,
    private val storeService: StoreService,
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

    fun recordSale(companyId: Long, sku: String, qty: Int, unitPrice: Double): CompletableFuture<LedgerEntryEntity> {
        val revenue = qty * unitPrice
        val entry = LedgerEntryEntity(
            companyId = companyId,
            entryAt = Instant.now(),
            debitAccount = "ACCOUNTS_RECEIVABLE",
            creditAccount = "SALES_REVENUE",
            amount = revenue,
            memo = "Sale of $sku"
        )
        return ledgerService.append(entry).thenApply { ledger ->
            eventBus.publish(SaleRecordedEvent(companyId, sku, qty, revenue))
            ledger
        }
    }

    fun openStore(companyId: Long, branchId: Long): CompletableFuture<StoreOpenResult> {
        return storeService.openStore(companyId, branchId)
    }
}

data class StoreOpenResult(val companyId: Long, val branchId: Long, val catalogSize: Int)

data class CompanyCreatedEvent(val company: CompanyEntity)
data class BranchCreatedEvent(val branch: BranchEntity)
data class SaleRecordedEvent(val companyId: Long, val sku: String, val qty: Int, val revenue: Double)

fun interface CompanyGateway {
    fun create(entity: CompanyEntity): CompletableFuture<CompanyEntity>
}

fun interface BranchGateway {
    fun create(entity: BranchEntity): CompletableFuture<BranchEntity>
}
