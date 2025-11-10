package com.kwonpop.companylife.modules.marketing

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class MarketingModule : Module {
    override val id: String = "marketing"
    override val displayName: String = "Marketing"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "마케팅 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.FIREWORK_ROCKET,
                        "<gold>캠페인",
                        listOf("<gray>진행 4", "<gray>예산 15k")
                    ),
                    ModuleDashboardGui.Card(
                        Material.MAP,
                        "<aqua>전환",
                        listOf("<gray>CTR 4.2%", "<gray>ROI 160%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
