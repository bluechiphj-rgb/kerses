package com.kwonpop.companylife.common.gui

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class ModuleDashboardGui(private val descriptor: Descriptor) : BaseInventoryGui(descriptor.title, 54) {
    private val mm = MiniMessage.miniMessage()

    override fun draw(inventory: Inventory) {
        descriptor.cards.forEachIndexed { index, card ->
            val item = ItemStack(card.material)
            val meta = item.itemMeta
            meta.displayName(mm.deserialize(card.title))
            meta.lore(card.lines.map { mm.deserialize(it) })
            item.itemMeta = meta
            inventory.setItem(index, item)
        }
    }

    data class Descriptor(
        val id: String,
        val title: String,
        val cards: List<Card>
    )

    data class Card(
        val material: Material,
        val title: String,
        val lines: List<String>
    )

    class Registry(private val descriptors: MutableMap<String, Descriptor> = mutableMapOf()) {
        fun register(descriptor: Descriptor) {
            descriptors[descriptor.id] = descriptor
        }

        fun create(id: String): ModuleDashboardGui? = descriptors[id]?.let { ModuleDashboardGui(it) }

        fun ids(): Set<String> = descriptors.keys
    }
}
