package com.kwonpop.companylife.modules.r_and_d

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ResearchModule : Module {
    override val id: String = "r_and_d"
    override val displayName: String = "R&D"

    override fun onEnable(services: ServiceRegistry) {
        services.register(ResearchService())
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "연구개발 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.ENCHANTING_TABLE,
                        "<aqua>연구 진행",
                        listOf("<gray>프로젝트 3", "<gray>성공률 68%")
                    ),
                    ModuleDashboardGui.Card(
                        Material.BOOKSHELF,
                        "<gold>특허",
                        listOf("<gray>보유 7", "<gray>만료 예정 1")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
