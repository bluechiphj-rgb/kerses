package com.kwonpop.companylife.modules.company

import com.kwonpop.companylife.common.config.CompanyBlueprintConfig
import com.kwonpop.companylife.common.config.RootConfig
import com.kwonpop.companylife.common.persistence.BranchEntity
import com.kwonpop.companylife.common.persistence.CompanyEntity
import java.util.concurrent.CompletableFuture

class CompanyAutoBuilder(
    private val configProvider: () -> RootConfig,
    private val companyService: CompanyService
) {

    fun listBlueprintKeys(): List<String> = configProvider().autobuild.blueprints.map { it.key }

    fun autoBuild(key: String, nameOverride: String? = null): CompletableFuture<CompanyAutoBuildSummary> {
        val config = configProvider().autobuild
        if (!config.enabled) {
            return CompletableFuture.failedFuture(IllegalStateException("disabled"))
        }
        val blueprint = config.blueprints.firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: return CompletableFuture.failedFuture(IllegalArgumentException("unknown:$key"))

        return companyService.createCompany(
            nameOverride ?: blueprint.displayName,
            blueprint.type,
            blueprint.regNo ?: "REG-${System.currentTimeMillis()}"
        ).thenCompose { company ->
            buildBranches(company, blueprint).thenCompose { ctx ->
                populateInventory(blueprint, ctx).thenApply {
                    CompanyAutoBuildSummary(
                        companyId = company.id ?: error("company id missing"),
                        companyName = company.name,
                        branchCount = ctx.branches.size,
                        productCount = blueprint.inventory.size,
                        branchIds = ctx.branches.mapValues { it.value.id ?: error("branch id missing") }
                    )
                }
            }
        }
    }

    private fun buildBranches(
        company: CompanyEntity,
        blueprint: CompanyBlueprintConfig
    ): CompletableFuture<AutoBuildContext> {
        var chain = CompletableFuture.completedFuture(AutoBuildContext(company))
        for (branch in blueprint.branches) {
            chain = chain.thenCompose { ctx ->
                val branchFuture = companyService.createBranch(
                    company.id ?: error("company id missing"),
                    branch.world,
                    branch.x,
                    branch.y,
                    branch.z
                )
                branchFuture.thenCompose { created ->
                    val updated = ctx.withBranch(branch.name, created)
                    if (branch.openStore) {
                        companyService.openStore(company.id!!, created.id!!).thenApply { updated }
                    } else {
                        CompletableFuture.completedFuture(updated)
                    }
                }
            }
        }
        return chain
    }

    private fun populateInventory(
        blueprint: CompanyBlueprintConfig,
        context: AutoBuildContext
    ): CompletableFuture<AutoBuildContext> {
        var chain = CompletableFuture.completedFuture(context)
        for (item in blueprint.inventory) {
            chain = chain.thenCompose { ctx ->
                val branch = ctx.branches[item.branch]
                    ?: return@thenCompose CompletableFuture.failedFuture<AutoBuildContext>(
                        IllegalStateException("Unknown branch reference ${item.branch} in blueprint ${blueprint.key}")
                    )
                val branchId = branch.id ?: error("Branch id missing")
                companyService.registerProduct(
                    branchId,
                    item.sku,
                    item.name,
                    item.cost,
                    item.price,
                    item.reorderPoint
                )

                val receiveFuture = if (item.initialWarehouseStock > 0) {
                    companyService.receiveIntoWarehouse(
                        branchId,
                        item.sku,
                        item.initialWarehouseStock,
                        item.orderCost,
                        item.holdingCost,
                        item.expectedDemand
                    )
                } else {
                    CompletableFuture.completedFuture(null)
                }

                receiveFuture.thenCompose {
                    if (item.initialStoreStock > 0) {
                        companyService.moveWarehouseStockToStore(branchId, item.sku, item.initialStoreStock)
                            .thenApply { ctx }
                    } else {
                        CompletableFuture.completedFuture(ctx)
                    }
                }
            }
        }
        return chain
    }

    private data class AutoBuildContext(
        val company: CompanyEntity,
        val branches: MutableMap<String, BranchEntity> = mutableMapOf()
    ) {
        fun withBranch(key: String, branch: BranchEntity): AutoBuildContext {
            branches[key] = branch
            return this
        }
    }
}

data class CompanyAutoBuildSummary(
    val companyId: Long,
    val companyName: String,
    val branchCount: Int,
    val productCount: Int,
    val branchIds: Map<String, Long>
)
