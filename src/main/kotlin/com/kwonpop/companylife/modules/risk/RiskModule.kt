package com.kwonpop.companylife.modules.risk

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class RiskModule : Module {
    override val id: String = "risk"
    override val displayName: String = "Risk"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "리스크 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BLAZE_POWDER,
                        "<gold>위험",
                        listOf("<gray>평균 0.32", "<gray>감시 2")
                    ),
                    ModuleDashboardGui.Card(
                        Material.SHIELD,
                        "<aqua>완화",
                        listOf("<gray>보험 적용 3", "<gray>통제 5")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
