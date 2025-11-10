package com.kwonpop.companylife.common.gui

import com.kwonpop.companylife.common.scheduler.FoliaScheduler
import com.kwonpop.companylife.common.service.MessageService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager(
    private val plugin: Plugin,
    private val scheduler: FoliaScheduler,
    private val messages: MessageService
) {
    private val activeGuis = ConcurrentHashMap<UUID, BaseInventoryGui>()

    fun open(player: Player, gui: BaseInventoryGui) {
        scheduler.runAtLocation(player.location) {
            val inventory = gui.createInventory()
            player.openInventory(inventory)
            activeGuis[player.uniqueId] = gui
        }
    }

    fun close(player: Player) {
        scheduler.runAtLocation(player.location) {
            player.closeInventory()
            activeGuis.remove(player.uniqueId)
        }
    }

    fun cleanup() {
        activeGuis.values.forEach { it.onClose() }
        activeGuis.clear()
    }
}

abstract class BaseInventoryGui(private val title: String, private val size: Int) {
    protected abstract fun draw(inventory: Inventory)

    fun createInventory(): Inventory {
        val inventory = Bukkit.createInventory(null, size, title)
        draw(inventory)
        return inventory
    }

    open fun onClose() {}
}
