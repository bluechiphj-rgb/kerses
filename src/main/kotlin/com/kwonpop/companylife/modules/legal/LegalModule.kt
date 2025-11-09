package com.kwonpop.companylife.modules.legal

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class LegalModule : Module {
    override val id: String = "legal"
    override val displayName: String = "Legal"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "법무 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.FEATHER,
                        "<gold>IP 관리",
                        listOf("<gray>특허 5", "<gray>분쟁 1")
                    ),
                    ModuleDashboardGui.Card(
                        Material.GAVEL,
                        "<red>분쟁",
                        listOf("<gray>진행 중 2", "<gray>승소율 80%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
