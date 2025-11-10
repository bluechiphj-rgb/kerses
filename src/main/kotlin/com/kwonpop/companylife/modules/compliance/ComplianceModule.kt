package com.kwonpop.companylife.modules.compliance

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ComplianceModule : Module {
    override val id: String = "compliance"
    override val displayName: String = "Compliance"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "컴플라이언스",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<gold>감시",
                        listOf("<gray>AML 경고 0", "<gray>보고 12")
                    ),
                    ModuleDashboardGui.Card(
                        Material.SHIELD,
                        "<aqua>정책",
                        listOf("<gray>정책 위반 0", "<gray>교육 98%")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
