package com.kwonpop.companylife.modules.stock

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class StockModule : Module {
    override val id: String = "stock"
    override val displayName: String = "Stock Market"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "주식 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.NAME_TAG,
                        "<green>주가",
                        listOf("<gray>IPO 120", "<gray>변동 +3%")
                    ),
                    ModuleDashboardGui.Card(
                        Material.BEACON,
                        "<aqua>배당",
                        listOf("<gray>배당률 2.5%", "<gray>지급 예정 1")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
