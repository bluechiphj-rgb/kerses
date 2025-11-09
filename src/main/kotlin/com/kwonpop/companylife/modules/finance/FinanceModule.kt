package com.kwonpop.companylife.modules.finance

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class FinanceModule : Module {
    override val id: String = "finance"
    override val displayName: String = "Finance"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "재무 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.GOLD_BLOCK,
                        "<gold>대출",
                        listOf("<gray>잔액 240k", "<gray>연체 0")
                    ),
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<aqua>신용",
                        listOf("<gray>신용등급 A", "<gray>리스크 12%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
