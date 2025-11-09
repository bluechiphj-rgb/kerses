package com.kwonpop.companylife.modules.crm

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class CrmModule : Module {
    override val id: String = "crm"
    override val displayName: String = "CRM"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "CRM 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.PLAYER_HEAD,
                        "<green>고객",
                        listOf("<gray>세그먼트 5", "<gray>VIP 24")
                    ),
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<aqua>NPS",
                        listOf("<gray>NPS 46", "<gray>응답 210")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
