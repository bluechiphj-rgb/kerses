package com.kwonpop.companylife.modules.event

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class EventModule : Module {
    override val id: String = "event"
    override val displayName: String = "Events"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "이벤트 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BELL,
                        "<gold>행사",
                        listOf("<gray>박람회 1", "<gray>세일 축제 진행")
                    ),
                    ModuleDashboardGui.Card(
                        Material.CAKE,
                        "<aqua>보상",
                        listOf("<gray>참여 320", "<gray>보상 280 지급")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
