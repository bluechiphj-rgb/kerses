package com.kwonpop.companylife.modules.realestate

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class RealEstateModule : Module {
    override val id: String = "realestate"
    override val displayName: String = "Real Estate"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "부동산 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BRICKS,
                        "<gold>지점",
                        listOf("<gray>보유 4", "<gray>임대 2")
                    ),
                    ModuleDashboardGui.Card(
                        Material.EMERALD_BLOCK,
                        "<aqua>ROI",
                        listOf("<gray>평균 ROI 18%", "<gray>공실 1")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
