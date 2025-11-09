package com.kwonpop.companylife.modules.transport

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class TransportModule : Module {
    override val id: String = "transport"
    override val displayName: String = "Transport"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "운송 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.MINECART,
                        "<gold>차량",
                        listOf("<gray>보유 8", "<gray>운행 5")
                    ),
                    ModuleDashboardGui.Card(
                        Material.CLOCK,
                        "<aqua>ETA",
                        listOf("<gray>평균 12m", "<gray>SLA 92%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
