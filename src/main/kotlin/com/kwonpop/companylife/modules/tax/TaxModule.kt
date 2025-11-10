package com.kwonpop.companylife.modules.tax

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class TaxModule : Module {
    override val id: String = "tax"
    override val displayName: String = "Tax"

    override fun onEnable(services: ServiceRegistry) {
        if (!services.contains(TaxService::class.java)) {
            val db: DatabaseManager = services.resolve()
            val eventBus: EventBus = services.resolve()
            services.register(TaxService(db.ledgerRepository, eventBus))
        }

        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "세무 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<yellow>납부 일정",
                        listOf("<gray>법인세 D-5", "<gray>부가세 D-12")
                    ),
                    ModuleDashboardGui.Card(
                        Material.EMERALD,
                        "<green>세액", listOf("<gray>예상 24k", "<gray>납부 완료 18k")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
