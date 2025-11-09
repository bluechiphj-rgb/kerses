package com.kwonpop.companylife.common.command

import com.kwonpop.companylife.common.config.ConfigService
import com.kwonpop.companylife.common.service.AuditService
import com.kwonpop.companylife.common.service.MessageService
import com.kwonpop.companylife.common.service.ServiceRegistry
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CompanyLifeAdminCommand(services: ServiceRegistry) : CommandExecutor, TabCompleter {
    private val config: ConfigService = services.resolve()
    private val audit: AuditService = services.resolve()
    private val messages: MessageService = services.resolve()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("complife.admin")) {
            messages.send(sender, "command.no_permission")
            return true
        }
        if (args.isEmpty()) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        return when (args[0].lowercase()) {
            "reload" -> {
                config.reload()
                messages.send(sender, "prefix", "prefix" to "<green>구성이 다시 로드되었습니다")
                true
            }
            "flush" -> {
                audit.flush()
                messages.send(sender, "prefix", "prefix" to "<yellow>감사 로그를 저장했습니다")
                true
            }
            else -> {
                messages.send(sender, "command.invalid_usage")
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf("reload", "flush")
        }
        return mutableListOf()
    }
}
