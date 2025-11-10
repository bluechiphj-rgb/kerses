package com.kwonpop.companylife.modules.analytics

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class AnalyticsModule : Module {
    override val id: String = "analytics"
    override val displayName: String = "Analytics"

    override fun onEnable(services: ServiceRegistry) {
        val webhook: DiscordWebhookClient = services.resolve()
        services.register(AnalyticsService(webhook))
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "분석 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.COMPASS,
                        "<gold>KPI",
                        listOf("<gray>매출 128k", "<gray>재고회전 4.3")
                    ),
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<aqua>Export",
                        listOf("<gray>CSV/JSON 지원", "<gray>Webhook On")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
