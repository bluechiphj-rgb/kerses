package com.kwonpop.companylife.modules.store

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class StoreModule : Module {
    override val id: String = "store"
    override val displayName: String = "Store"

    override fun onEnable(services: ServiceRegistry) {
        val eventBus: EventBus = services.resolve()
        if (!services.contains(StoreService::class.java)) {
            services.register(StoreService(eventBus))
        }
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "상점 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.CHEST,
                        "<gold>SKU",
                        listOf("<gray>재고 SKU: 85", "<gray>품절 3")
                    ),
                    ModuleDashboardGui.Card(
                        Material.DIAMOND,
                        "<aqua>마진",
                        listOf("<gray>평균 32%", "<gray>프로모션 4건")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
