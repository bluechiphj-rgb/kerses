package com.kwonpop.companylife.modules.company

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import com.kwonpop.companylife.modules.economy.LedgerService
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import org.bukkit.Material

class CompanyModule : Module {
    override val id: String = "company"
    override val displayName: String = "Company"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        val ledger: LedgerService = services.resolve()
        val store: StoreService = services.resolve()
        val warehouse: WarehouseService = services.resolve()
        val eventBus: EventBus = services.resolve()
        services.register(CompanyService(db.companyRepository, db.branchRepository, ledger, store, warehouse, eventBus))

        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "법인 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.EMERALD,
                        "<green>평판",
                        listOf("<gray>서비스 82", "<gray>윤리 90")
                    ),
                    ModuleDashboardGui.Card(
                        Material.CLOCK,
                        "<gold>허가",
                        listOf("<gray>납부 기한: 5d", "<gray>검토 중: 1")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
