package com.kwonpop.companylife.common.service

import com.kwonpop.companylife.common.config.ConfigService
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender

class MessageService(
    private val configService: ConfigService,
    private val audiences: BukkitAudiences
) {
    private val miniMessage = MiniMessage.miniMessage()

    fun format(key: String, vararg placeholders: Pair<String, String>): Component {
        val raw = configService.messages().getOrDefault(key, "<gray>$key</gray>")
        var processed = raw
        placeholders.forEach { (placeholder, value) ->
            processed = processed.replace("{$placeholder}", value)
        }
        return miniMessage.deserialize(processed)
    }

    fun send(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        audiences.sender(sender).sendMessage(format(key, *placeholders))
    }
}
