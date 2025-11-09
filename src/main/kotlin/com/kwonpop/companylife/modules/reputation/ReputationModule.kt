package com.kwonpop.companylife.modules.reputation

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ReputationModule : Module {
    override val id: String = "reputation"
    override val displayName: String = "Reputation"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "평판 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.EMERALD,
                        "<green>지표",
                        listOf("<gray>서비스 88", "<gray>윤리 92")
                    ),
                    ModuleDashboardGui.Card(
                        Material.LODESTONE,
                        "<aqua>보너스",
                        listOf("<gray>입찰 가산 +5", "<gray>거래 제한 없음")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
