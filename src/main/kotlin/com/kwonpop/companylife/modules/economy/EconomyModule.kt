package com.kwonpop.companylife.modules.economy

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class EconomyModule : Module {
    override val id: String = "economy"
    override val displayName: String = "Economy"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        services.register(LedgerService(db.ledgerRepository))
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "경제 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.GOLD_INGOT,
                        "<gold>현금 흐름",
                        listOf("<gray>매출: +120k", "<gray>비용: -45k")
                    ),
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<aqua>회계 상태",
                        listOf("<gray>전표 128건", "<gray>세금 신고 예정 2건")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
