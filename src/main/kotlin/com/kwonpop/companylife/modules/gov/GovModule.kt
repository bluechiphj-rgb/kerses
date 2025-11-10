package com.kwonpop.companylife.modules.gov

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class GovModule : Module {
    override val id: String = "gov"
    override val displayName: String = "Government"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "규제 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.SHIELD,
                        "<gold>허가",
                        listOf("<gray>유효 5", "<gray>만료 예정 1")
                    ),
                    ModuleDashboardGui.Card(
                        Material.BOOK,
                        "<aqua>보고",
                        listOf("<gray>제출 3", "<gray>지연 0")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
