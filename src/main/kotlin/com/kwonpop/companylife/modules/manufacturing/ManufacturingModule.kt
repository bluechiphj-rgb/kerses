package com.kwonpop.companylife.modules.manufacturing

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ManufacturingModule : Module {
    override val id: String = "manufacturing"
    override val displayName: String = "Manufacturing"

    override fun onEnable(services: ServiceRegistry) {
        services.register(ManufacturingService())
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "제조 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.FURNACE,
                        "<gold>공정",
                        listOf("<gray>큐 4건", "<gray>병목 없음")
                    ),
                    ModuleDashboardGui.Card(
                        Material.ANVIL,
                        "<red>설비",
                        listOf("<gray>가동률 82%", "<gray>점검 예정 1")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
