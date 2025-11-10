package com.kwonpop.companylife.modules.procurement

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ProcurementModule : Module {
    override val id: String = "procurement"
    override val displayName: String = "Procurement"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        val eventBus: EventBus = services.resolve()
        services.register(ProcurementService(db.procurementRepository, eventBus))
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "조달 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.WRITABLE_BOOK,
                        "<gold>입찰",
                        listOf("<gray>공고 4", "<gray>참여 12")
                    ),
                    ModuleDashboardGui.Card(
                        Material.COMPASS,
                        "<aqua>평가",
                        listOf("<gray>평균 점수 76", "<gray>낙찰 2")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
