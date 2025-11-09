package com.kwonpop.companylife.modules.core

import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.modules.company.CompanyService
import com.kwonpop.companylife.modules.payroll.PayrollEmployee
import com.kwonpop.companylife.modules.payroll.PayrollService
import com.kwonpop.companylife.modules.procurement.ProcurementService
import com.kwonpop.companylife.modules.logistics.LogisticsService
import com.kwonpop.companylife.modules.store.StorePerformance
import com.kwonpop.companylife.modules.tax.TaxService
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ScenarioOrchestrator(private val services: ServiceRegistry) {
    fun runSample(): CompletableFuture<String> {
        val companyService: CompanyService = services.resolve()
        val payrollService: PayrollService = services.resolve()
        val procurementService: ProcurementService = services.resolve()
        val logisticsService: LogisticsService = services.resolve()
        val taxService: TaxService = services.resolve()

        return companyService.createCompany("샘플코", "LLC", "REG-DEMO").thenCompose { company ->
            companyService.createBranch(company.id!!, "world", 0.0, 64.0, 0.0).thenCompose { branch ->
                companyService.openStore(company.id, branch.id!!).thenCompose {
                    companyService.registerProduct(branch.id, "sample_sku", "Sample Gadget", 48.0, 120.0, reorderPoint = 30)
                    companyService.receiveIntoWarehouse(branch.id, "sample_sku", 120, cost = 400.0, holdingCost = 8.0, demand = 900).thenCompose {
                        companyService.moveWarehouseStockToStore(branch.id, "sample_sku", 80)
                    }.thenCompose {
                        companyService.processSale(company.id, branch.id, "sample_sku", 20)
                    }.thenCompose {
                        taxService.settleCorporateTax(company.id, "Q1")
                    }.thenCompose { settlement ->
                        payrollService.runPayroll(
                            company.id,
                            listOf(
                                PayrollEmployee(UUID.randomUUID(), baseSalary = 500.0, hoursWorked = 40.0, hourlyRate = 12.5, overtimeHours = 5.0),
                                PayrollEmployee(UUID.randomUUID(), baseSalary = 650.0, bonus = 120.0)
                            ),
                            "W1"
                        ).thenCompose {
                            procurementService.submitBid(1, company.id, price = 950.0, leadTime = 3, quality = 9.0).thenCompose {
                                val graph = mapOf(
                                    "A" to mapOf("B" to 3.0, "C" to 5.0),
                                    "B" to mapOf("D" to 4.0),
                                    "C" to mapOf("D" to 2.0),
                                    "D" to emptyMap()
                                )
                                val route = logisticsService.planRoute(graph, "A", "D")
                                logisticsService.dispatch(10, "vehicle-1", route, slaMinutes = 12).thenCompose { job ->
                                    logisticsService.evaluateSla(job.orderId, job.sla, route.cost, Instant.now()).thenApply { evaluation ->
                                        val performance: StorePerformance = companyService.evaluateStore(branch.id)
                                        "Scenario complete: ${settlement.amount} tax due, ${performance.trailingRevenue} revenue, SLA ${evaluation.slaScore}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
