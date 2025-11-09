package com.kwonpop.companylife.common.integration

import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.ServicesManager
import java.util.UUID
import java.util.logging.Logger

class VaultBridge(private val logger: Logger, servicesManager: ServicesManager) {
    private val economy: Economy? = servicesManager.getRegistration(Economy::class.java)?.provider

    fun deposit(uuid: UUID, amount: Double) {
        economy?.depositPlayer(uuid.toString(), amount)
            ?: logger.fine("Vault economy not available; skipping deposit $amount for $uuid")
    }

    fun withdraw(uuid: UUID, amount: Double) {
        economy?.withdrawPlayer(uuid.toString(), amount)
            ?: logger.fine("Vault economy not available; skipping withdraw $amount for $uuid")
    }
}
