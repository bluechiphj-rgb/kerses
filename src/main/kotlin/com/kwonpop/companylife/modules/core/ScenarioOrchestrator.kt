package com.kwonpop.companylife.modules.core

import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.modules.company.CompanyService
import com.kwonpop.companylife.modules.payroll.PayrollEmployee
import com.kwonpop.companylife.modules.payroll.PayrollService
import com.kwonpop.companylife.modules.procurement.ProcurementService
import com.kwonpop.companylife.modules.logistics.LogisticsService
import com.kwonpop.companylife.modules.logistics.RoutePlan
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.tax.TaxService
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ScenarioOrchestrator(private val services: ServiceRegistry) {
    fun runSample(): CompletableFuture<String> {
        val companyService: CompanyService = services.resolve()
        val storeService: StoreService = services.resolve()
        val warehouseService: WarehouseService = services.resolve()
        val payrollService: PayrollService = services.resolve()
        val procurementService: ProcurementService = services.resolve()
        val logisticsService: LogisticsService = services.resolve()
        val taxService: TaxService = services.resolve()

        return companyService.createCompany("샘플코", "LLC", "REG-DEMO").thenCompose { company ->
            companyService.createBranch(company.id!!, "world", 0.0, 64.0, 0.0).thenCompose { branch ->
                companyService.openStore(company.id, branch.id!!).thenCompose {
                    warehouseService.receive("sample_sku", 100, cost = 50.0, holdingCost = 2.0, demand = 400).thenCompose {
                        storeService.addInventory(branch.id, "sample_sku", 50)
                        companyService.recordSale(company.id, "sample_sku", 5, 100.0)
                    }.thenCompose {
                        taxService.settleCorporateTax(company.id, "Q1").thenCompose { tax ->
                            payrollService.runPayroll(
                                company.id,
                                listOf(
                                    PayrollEmployee(UUID.randomUUID(), 500.0),
                                    PayrollEmployee(UUID.randomUUID(), 600.0)
                                ),
                                "W1"
                            ).thenCompose {
                                procurementService.submitBid(1, company.id, price = 1000.0, leadTime = 3, quality = 8.0).thenCompose {
                                    val graph = mapOf(
                                        "A" to mapOf("B" to 3.0, "C" to 5.0),
                                        "B" to mapOf("D" to 4.0),
                                        "C" to mapOf("D" to 2.0),
                                        "D" to emptyMap()
                                    )
                                    val route = logisticsService.planRoute(graph, "A", "D")
                                    logisticsService.dispatch(10, "vehicle-1", route, slaMinutes = 12).thenApply {
                                        "Scenario complete: revenue entry ${it.id}, route cost ${route.cost}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
