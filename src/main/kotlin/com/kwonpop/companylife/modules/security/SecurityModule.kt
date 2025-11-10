package com.kwonpop.companylife.modules.security

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.integration.LuckPermsHelper
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class SecurityModule : Module {
    override val id: String = "security"
    override val displayName: String = "Security"

    override fun onEnable(services: ServiceRegistry) {
        services.resolve<LuckPermsHelper>()
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "보안 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.IRON_DOOR,
                        "<gold>권한",
                        listOf("<gray>롤 12", "<gray>이상 0")
                    ),
                    ModuleDashboardGui.Card(
                        Material.ENDER_EYE,
                        "<aqua>2단계",
                        listOf("<gray>서명 요청 4", "<gray>완료 3")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
