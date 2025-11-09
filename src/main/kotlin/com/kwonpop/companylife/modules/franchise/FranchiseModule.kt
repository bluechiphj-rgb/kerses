package com.kwonpop.companylife.modules.franchise

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class FranchiseModule : Module {
    override val id: String = "franchise"
    override val displayName: String = "Franchise"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "가맹 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BOOK,
                        "<gold>계약",
                        listOf("<gray>가맹점 6", "<gray>갱신 1")
                    ),
                    ModuleDashboardGui.Card(
                        Material.HONEYCOMB,
                        "<aqua>로열티",
                        listOf("<gray>월간 18k", "<gray>미납 0")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
