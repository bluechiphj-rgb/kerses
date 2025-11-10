package com.kwonpop.companylife.modules.api

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ApiModule : Module {
    override val id: String = "api"
    override val displayName: String = "API"

    override fun onEnable(services: ServiceRegistry) {
        services.register(ApiService())
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "API 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.REPEATER,
                        "<gold>Webhook",
                        listOf("<gray>Discord 연동", "<gray>토큰 기반")
                    ),
                    ModuleDashboardGui.Card(
                        Material.BOOK,
                        "<aqua>REST",
                        listOf("<gray>/api/status", "<gray>JWT 준비 중")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
