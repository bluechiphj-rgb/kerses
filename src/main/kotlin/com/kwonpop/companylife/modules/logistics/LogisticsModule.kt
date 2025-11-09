package com.kwonpop.companylife.modules.logistics

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class LogisticsModule : Module {
    override val id: String = "logistics"
    override val displayName: String = "Logistics"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        val eventBus: EventBus = services.resolve()
        services.register(LogisticsService(db.logisticsRepository, eventBus))
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "물류 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.MINECART,
                        "<gold>SLA",
                        listOf("<gray>SLA 달성 94%", "<gray>지연 2건")
                    ),
                    ModuleDashboardGui.Card(
                        Material.MAP,
                        "<aqua>경로",
                        listOf("<gray>평균 거리 12", "<gray>배차 대기 3")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
