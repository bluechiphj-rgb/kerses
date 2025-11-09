package com.kwonpop.companylife.modules.hr

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class HrModule : Module {
    override val id: String = "hr"
    override val displayName: String = "Human Resources"

    override fun onEnable(services: ServiceRegistry) {
        services.register(HrService())
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "HR 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.PLAYER_HEAD,
                        "<green>인원 현황",
                        listOf("<gray>정규직 24", "<gray>파트타임 8")
                    ),
                    ModuleDashboardGui.Card(
                        Material.CLOCK,
                        "<gold>교대/근태",
                        listOf("<gray>지각 0", "<gray>휴가 2")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
