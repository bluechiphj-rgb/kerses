package com.example.companyroleplay;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class CompanyRoleplayPlugin extends JavaPlugin {
    private CompanyManager companyManager;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        companyManager = new CompanyManager(this);
        companyManager.loadCompanies();

        setupEconomy();

        CommandExecutor companyCommand = new CompanyCommand(this, companyManager);
        PluginCommand command = getCommand("company");
        if (command != null) {
            command.setExecutor(companyCommand);
            command.setTabCompleter((CompanyCommand) companyCommand);
        } else {
            getLogger().log(Level.SEVERE, "Command 'company' not defined in plugin.yml");
        }

        getLogger().info("CompanyRoleplayPlugin enabled");
    }

    @Override
    public void onDisable() {
        companyManager.saveCompanies();
        getLogger().info("CompanyRoleplayPlugin disabled");
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found - salary payouts will be disabled");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Vault economy provider not found - salary payouts will be disabled");
            return;
        }

        economy = rsp.getProvider();
        getLogger().info("Hooked into Vault economy: " + economy.getName());
    }

    public Economy getEconomy() {
        return economy;
    }
}
