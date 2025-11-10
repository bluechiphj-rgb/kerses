package com.kwonpop.companylife.common.integration

import com.kwonpop.companylife.CompanyLifePlugin
import com.kwonpop.companylife.modules.company.CompanyService
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PlaceholderBridge(
    private val plugin: CompanyLifePlugin,
    private val scheduler: com.kwonpop.companylife.common.scheduler.FoliaScheduler
) {
    private var expansion: CompanyLifeExpansion? = null

    fun hook() {
        if (expansion != null) return
        expansion = CompanyLifeExpansion(plugin).also { it.register() }
    }

    fun unhook() {
        expansion?.unregister()
        expansion = null
    }

    private class CompanyLifeExpansion(private val plugin: CompanyLifePlugin) : PlaceholderExpansion() {
        override fun getIdentifier(): String = "complife"
        override fun getAuthor(): String = "CompanyLife"
        override fun getVersion(): String = plugin.description.version

        override fun onRequest(player: OfflinePlayer?, params: String): String? {
            return when (params.lowercase()) {
                "company_name" -> "Demo Corp"
                "balance_company" -> "102400"
                "reputation" -> "75"
                else -> null
            }
        }
    }
}
