package com.kwonpop.companylife.modules.contract

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class ContractModule : Module {
    override val id: String = "contract"
    override val displayName: String = "Contract"

    override fun onEnable(services: ServiceRegistry) {
        val db: DatabaseManager = services.resolve()
        val eventBus: EventBus = services.resolve()
        services.register(ContractService(db.contractRepository, eventBus))
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "계약 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.WRITABLE_BOOK,
                        "<gold>계약 진행",
                        listOf("<gray>체결 12", "<gray>대기 3")
                    ),
                    ModuleDashboardGui.Card(
                        Material.REDSTONE,
                        "<red>위약 위험",
                        listOf("<gray>경고 1", "<gray>만료 예정 2")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
