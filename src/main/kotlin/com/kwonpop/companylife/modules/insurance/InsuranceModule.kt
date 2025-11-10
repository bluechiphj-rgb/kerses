package com.kwonpop.companylife.modules.insurance

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class InsuranceModule : Module {
    override val id: String = "insurance"
    override val displayName: String = "Insurance"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "보험 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.TURTLE_HELMET,
                        "<gold>보장",
                        listOf("<gray>화재 3", "<gray>고장 2")
                    ),
                    ModuleDashboardGui.Card(
                        Material.POTION,
                        "<aqua>청구",
                        listOf("<gray>최근 1건", "<gray>지급률 92%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
