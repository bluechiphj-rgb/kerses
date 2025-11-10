package com.kwonpop.companylife.modules.integration

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class IntegrationModule : Module {
    override val id: String = "integration"
    override val displayName: String = "Integrations"

    override fun onEnable(services: ServiceRegistry) {
        services.resolve<DiscordWebhookClient>()
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "연동 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.FILLED_MAP,
                        "<gold>지도",
                        listOf("<gray>Dynmap Hook", "<gray>BlueMap Hook")
                    ),
                    ModuleDashboardGui.Card(
                        Material.NOTE_BLOCK,
                        "<aqua>Discord",
                        listOf("<gray>Webhook 상태", "<gray>PAPI 연동")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
