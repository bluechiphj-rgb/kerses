package com.kwonpop.companylife.modules.payroll

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.AuditService
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.tax.TaxService
import org.bukkit.Material

class PayrollModule : Module {
    override val id: String = "payroll"
    override val displayName: String = "Payroll"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        val ledger: LedgerService = services.resolve()
        val tax: TaxService = services.resolve()
        val audit: AuditService = services.resolve()
        val vault: com.kwonpop.companylife.common.integration.VaultBridge = services.resolve()
        val eventBus = services.resolve<com.kwonpop.companylife.common.service.EventBus>()
        services.register(
            PayrollService(
                db.payrollRepository,
                ledger,
                tax,
                com.kwonpop.companylife.modules.payroll.AuditRecorder { actor, action, detail, signature ->
                    audit.record(actor, action, detail, signature)
                },
                com.kwonpop.companylife.modules.payroll.PayrollBank { uuid, amount ->
                    vault.deposit(uuid, amount)
                },
                eventBus
            )
        )

        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "급여 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<gold>이번 급여",
                        listOf("<gray>총액 45k", "<gray>세후 39k")
                    ),
                    ModuleDashboardGui.Card(
                        Material.EXPERIENCE_BOTTLE,
                        "<aqua>KPI",
                        listOf("<gray>성과 지급 3건", "<gray>보너스 2건")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
