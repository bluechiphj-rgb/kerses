package com.kwonpop.companylife.modules.core

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class CoreModule : Module {
    override val id: String = "core"
    override val displayName: String = "Core Systems"

    override fun onEnable(services: ServiceRegistry) {
        val registry = services.dashboardRegistry()
        registry.register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "Core Dashboard",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.NETHER_STAR,
                        "<gold>시스템 상태",
                        listOf("<gray>서비스: OK", "<gray>DB: Ready")
                    ),
                    ModuleDashboardGui.Card(
                        Material.BOOK,
                        "<aqua>법인 요약",
                        listOf("<gray>등록 법인: 1", "<gray>지점: 1")
                    )
                )
            )
        )
        services.register(ScenarioOrchestrator(services))
    }

    override fun onDisable(services: ServiceRegistry) {}
}
