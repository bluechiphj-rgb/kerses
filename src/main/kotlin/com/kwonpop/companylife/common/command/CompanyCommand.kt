package com.kwonpop.companylife.common.command

import com.kwonpop.companylife.CompanyLifePlugin
import com.kwonpop.companylife.common.gui.ModuleDashboardGui
import com.kwonpop.companylife.common.service.MessageService
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.modules.company.CompanyService
import com.kwonpop.companylife.modules.company.CompanyAutoBuilder
import com.kwonpop.companylife.modules.core.ScenarioOrchestrator
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CompanyCommand(
    private val plugin: CompanyLifePlugin,
    services: ServiceRegistry
) : CommandExecutor, TabCompleter {

    private val messages: MessageService = services.resolve()
    private val companyService: CompanyService = services.resolve()
    private val autoBuilder: CompanyAutoBuilder = services.resolve()
    private val orchestrator: ScenarioOrchestrator = services.resolve()
    private val dashboards: ModuleDashboardGui.Registry = services.resolve()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        return when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "branch" -> handleBranch(sender, args)
            "gui" -> handleGui(sender, args)
            "simulate" -> handleSimulate(sender)
            "autobuild" -> handleAutoBuild(sender, args)
            else -> {
                messages.send(sender, "command.invalid_usage")
                true
            }
        }
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("complife.company.use")) {
            messages.send(sender, "command.no_permission")
            return true
        }
        if (args.size < 3) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        val name = args[1]
        val type = args[2]
        companyService.createCompany(name, type, "REG-${System.currentTimeMillis()}").thenAccept { company ->
            messages.send(sender, "company.created", "name" to company.name)
        }
        return true
    }

    private fun handleBranch(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 6) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        val companyId = args[1].toLong()
        val world = args[2]
        val x = args[3].toDouble()
        val y = args[4].toDouble()
        val z = args[5].toDouble()
        companyService.createBranch(companyId, world, x, y, z).thenAccept { branch ->
            messages.send(sender, "company.branch.created", "branch" to "${branch.world} (${branch.x},${branch.y},${branch.z})")
        }
        return true
    }

    private fun handleGui(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            messages.send(sender, "command.only_player")
            return true
        }
        val module = args.getOrNull(1)?.lowercase() ?: "core"
        val gui = dashboards.create(module)
        if (gui == null) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        plugin.services().resolve<com.kwonpop.companylife.common.gui.GuiManager>().open(sender, gui)
        return true
    }

    private fun handleSimulate(sender: CommandSender): Boolean {
        orchestrator.runSample().thenAccept { summary ->
            messages.send(sender, "prefix", "prefix" to summary)
        }
        return true
    }

    private fun handleAutoBuild(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("complife.company.autobuild")) {
            messages.send(sender, "command.no_permission")
            return true
        }
        val blueprint = args.getOrNull(1)
        if (blueprint.isNullOrBlank()) {
            messages.send(sender, "command.invalid_usage")
            return true
        }
        val nameOverride = args.getOrNull(2)
        autoBuilder.autoBuild(blueprint, nameOverride).thenAccept { summary ->
            messages.send(
                sender,
                "company.autobuild.success",
                "name" to summary.companyName,
                "branches" to summary.branchCount.toString(),
                "products" to summary.productCount.toString()
            )
        }.exceptionally { throwable ->
            val cause = throwable.cause ?: throwable
            val key = when {
                cause is IllegalStateException && cause.message == "disabled" -> "company.autobuild.disabled"
                cause is IllegalArgumentException && cause.message?.startsWith("unknown:") == true -> {
                    val requested = cause.message?.substringAfter(":") ?: blueprint
                    messages.send(sender, "company.autobuild.unknown", "key" to requested)
                    return@exceptionally null
                }
                else -> {
                    messages.send(sender, "company.autobuild.failed", "error" to (cause.message ?: cause.javaClass.simpleName))
                    return@exceptionally null
                }
            }
            messages.send(sender, key)
            null
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf("create", "branch", "gui", "simulate", "autobuild")
        }
        if (args.size == 2 && args[0].equals("gui", true)) {
            return dashboards.ids().toMutableList()
        }
        if (args.size == 2 && args[0].equals("autobuild", true)) {
            return autoBuilder.listBlueprintKeys().toMutableList()
        }
        return mutableListOf()
    }
}
