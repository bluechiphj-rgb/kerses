package com.kwonpop.companylife.modules.audit

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.AuditService
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class AuditModule : Module {
    override val id: String = "audit"
    override val displayName: String = "Audit"

    override fun onEnable(services: ServiceRegistry) {
        services.resolve<AuditService>()
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "감사 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BOOK_AND_QUILL,
                        "<gold>로그",
                        listOf("<gray>오늘 128건", "<gray>경고 2")
                    ),
                    ModuleDashboardGui.Card(
                        Material.COMPARATOR,
                        "<aqua>롤백",
                        listOf("<gray>대기 1", "<gray>완료 5")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
