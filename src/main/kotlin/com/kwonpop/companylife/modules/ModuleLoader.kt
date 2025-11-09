package com.kwonpop.companylife.modules

import com.kwonpop.companylife.common.service.ServiceRegistry
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

class ModuleLoader(private val plugin: Plugin, private val services: ServiceRegistry) {

    private val modules = mutableListOf<Module>()
    private val enabled = mutableListOf<Module>()
    private val logger: Logger = plugin.logger

    fun register(vararg module: Module) {
        modules += module
    }

    fun enableAll() {
        modules.sortedBy { it.id }.forEach { module ->
            if (module.requiredServices.any { !services.contains(it) }) {
                logger.warning("[Module] Skipping ${module.displayName} because requirements missing")
                return@forEach
            }
            try {
                module.onEnable(services)
                enabled += module
                logger.info("[Module] Enabled ${module.displayName}")
            } catch (ex: Exception) {
                logger.severe("[Module] Failed to enable ${module.id}: ${ex.message}")
                plugin.server.scheduler.runTask(plugin) {
                    throw ex
                }
            }
        }
    }

    fun disableAll() {
        enabled.asReversed().forEach { module ->
            runCatching { module.onDisable(services) }
                .onFailure { logger.severe("[Module] Failed to disable ${module.id}: ${it.message}") }
        }
        enabled.clear()
    }
}
