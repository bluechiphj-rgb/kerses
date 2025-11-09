package com.kwonpop.companylife.modules.quest

import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.common.service.dashboardRegistry
import com.kwonpop.companylife.modules.Module
import org.bukkit.Material

class QuestModule : Module {
    override val id: String = "quest"
    override val displayName: String = "Season & Quest"

    override fun onEnable(services: ServiceRegistry) {
        services.dashboardRegistry().register(
            ModuleDashboardGui.Descriptor(
                id = id,
                title = "이벤트/퀘스트",
                cards = listOf(
                    ModuleDashboardGui.Card(
                        Material.TOTEM_OF_UNDYING,
                        "<gold>시즌",
                        listOf("<gray>분기 시즌 2", "<gray>종료까지 14d")
                    ),
                    ModuleDashboardGui.Card(
                        Material.PAPER,
                        "<aqua>도전과제",
                        listOf("<gray>완료 42%", "<gray>리더보드 갱신")
                    )
                )
            )
        )
    }

    override fun onDisable(services: ServiceRegistry) {}
}
