package com.kwonpop.companylife.modules.warehouse

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class WarehouseModule : Module {
    override val id: String = "warehouse"
    override val displayName: String = "Warehouse"

    override fun onEnable(services: ServiceRegistry) {
        if (!services.contains(WarehouseService::class.java)) {
            services.register(WarehouseService())
        }
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "창고 대시보드",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.BARREL,
                        "<gold>재고 수준",
                        listOf("<gray>안전재고 위반 1", "<gray>EOQ 평균 120")
                    ),
                    ModuleDashboardGui.Card(
                        Material.HOPPER,
                        "<aqua>입출고",
                        listOf("<gray>입고 12건", "<gray>출고 9건")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
