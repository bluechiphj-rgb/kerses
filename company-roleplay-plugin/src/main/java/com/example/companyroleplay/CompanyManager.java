package com.example.companyroleplay;

import com.example.companyroleplay.model.Company;
import com.example.companyroleplay.model.CompanyMember;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CompanyManager {
    private final CompanyRoleplayPlugin plugin;
    private final Map<String, Company> companies = new HashMap<>();
    private final Map<UUID, String> invitations = new HashMap<>();
    private File companiesFile;
    private FileConfiguration companiesConfig;

    public CompanyManager(CompanyRoleplayPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadCompanies() {
        companiesFile = new File(plugin.getDataFolder(), "companies.yml");
        if (!companiesFile.exists()) {
            File parent = companiesFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                plugin.saveResource("companies.yml", false);
            } catch (IllegalArgumentException ignored) {
                try {
                    companiesFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create companies.yml: " + e.getMessage());
                }
            }
        }

        companiesConfig = YamlConfiguration.loadConfiguration(companiesFile);

        companies.clear();
        for (String key : companiesConfig.getKeys(false)) {
            String name = companiesConfig.getString(key + ".name", key);
            String ownerId = companiesConfig.getString(key + ".owner");
            if (ownerId == null) {
                plugin.getLogger().warning("Skipping company '" + key + "' because owner is missing in companies.yml");
                continue;
            }
            UUID owner = UUID.fromString(ownerId);
            Company company = new Company(name, owner);

            if (companiesConfig.contains(key + ".members")) {
                ConfigurationSection membersSection = companiesConfig.getConfigurationSection(key + ".members");
                if (membersSection != null) {
                    for (String memberKey : membersSection.getKeys(false)) {
                        UUID uuid = UUID.fromString(memberKey);
                        String role = companiesConfig.getString(key + ".members." + memberKey + ".role", "Employee");
                        double salary = companiesConfig.getDouble(key + ".members." + memberKey + ".salary", 0.0);
                        company.addMember(new CompanyMember(uuid, role, salary));
                    }
                }
            }

            companies.put(name.toLowerCase(Locale.ROOT), company);
        }
    }

    public void saveCompanies() {
        if (companiesFile == null) {
            companiesFile = new File(plugin.getDataFolder(), "companies.yml");
        }
        File parent = companiesFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        companiesConfig = new YamlConfiguration();

        for (Company company : companies.values()) {
            String key = company.getName().toLowerCase(Locale.ROOT);
            companiesConfig.set(key + ".name", company.getName());
            companiesConfig.set(key + ".owner", company.getOwner().toString());
            for (CompanyMember member : company.getMembers().values()) {
                String memberPath = key + ".members." + member.getUuid();
                companiesConfig.set(memberPath + ".role", member.getRole());
                companiesConfig.set(memberPath + ".salary", member.getSalary());
            }
        }

        try {
            companiesConfig.save(companiesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save companies.yml: " + e.getMessage());
        }
    }

    public Optional<Company> getCompany(String name) {
        return Optional.ofNullable(companies.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Company> getCompany(UUID playerId) {
        return companies.values().stream()
                .filter(company -> company.isMember(playerId))
                .findFirst();
    }

    public boolean createCompany(Player owner, String name) {
        if (companies.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }

        if (getCompany(owner.getUniqueId()).isPresent()) {
            return false;
        }

        Company company = new Company(name, owner.getUniqueId());
        company.addMember(new CompanyMember(owner.getUniqueId(), "CEO", plugin.getConfig().getDouble("settings.default_salary", 0.0)));
        companies.put(name.toLowerCase(Locale.ROOT), company);
        return true;
    }

    public boolean disbandCompany(Player owner) {
        Optional<Company> companyOpt = getCompany(owner.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (!company.getOwner().equals(owner.getUniqueId())) {
            return false;
        }

        companies.remove(company.getName().toLowerCase(Locale.ROOT));
        invitations.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(company.getName()));
        return true;
    }

    public boolean invitePlayer(Player inviter, OfflinePlayer target) {
        Optional<Company> companyOpt = getCompany(inviter.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (!company.isManager(inviter.getUniqueId())) {
            return false;
        }

        if (company.isMember(target.getUniqueId())) {
            return false;
        }

        invitations.put(target.getUniqueId(), company.getName());
        return true;
    }

    public boolean acceptInvite(Player player) {
        return acceptInvite(player, null);
    }

    public boolean acceptInvite(Player player, String expectedCompany) {
        String companyName = invitations.get(player.getUniqueId());
        if (companyName == null) {
            return false;
        }

        if (expectedCompany != null && !companyName.equalsIgnoreCase(expectedCompany)) {
            return false;
        }

        invitations.remove(player.getUniqueId());

        if (getCompany(player.getUniqueId()).isPresent()) {
            return false;
        }

        Company company = companies.get(companyName.toLowerCase(Locale.ROOT));
        if (company == null) {
            return false;
        }

        company.addMember(new CompanyMember(player.getUniqueId(), "Employee", plugin.getConfig().getDouble("settings.default_salary", 0.0)));
        return true;
    }

    public boolean leaveCompany(Player player) {
        Optional<Company> companyOpt = getCompany(player.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (company.getOwner().equals(player.getUniqueId())) {
            return false;
        }

        company.removeMember(player.getUniqueId());
        return true;
    }

    public boolean setRole(Player actor, OfflinePlayer target, String role) {
        Optional<Company> companyOpt = getCompany(actor.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (!company.isManager(actor.getUniqueId())) {
            return false;
        }

        CompanyMember member = company.getMembers().get(target.getUniqueId());
        if (member == null) {
            return false;
        }

        member.setRole(role);
        return true;
    }

    public boolean setSalary(Player actor, OfflinePlayer target, double salary) {
        Optional<Company> companyOpt = getCompany(actor.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (!company.isManager(actor.getUniqueId())) {
            return false;
        }

        CompanyMember member = company.getMembers().get(target.getUniqueId());
        if (member == null) {
            return false;
        }

        member.setSalary(salary);
        return true;
    }

    public boolean paySalaries(Player actor) {
        Optional<Company> companyOpt = getCompany(actor.getUniqueId());
        if (!companyOpt.isPresent()) {
            return false;
        }

        Company company = companyOpt.get();
        if (!company.isManager(actor.getUniqueId())) {
            return false;
        }

        if (plugin.getEconomy() == null) {
            return false;
        }

        for (CompanyMember member : company.getMembers().values()) {
            if (member.getSalary() <= 0) {
                continue;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getUuid());
            plugin.getEconomy().depositPlayer(offlinePlayer, member.getSalary());
            if (offlinePlayer.isOnline()) {
                Objects.requireNonNull(offlinePlayer.getPlayer()).sendMessage("§a" + company.getName() + " §7에서 급여 §a" + member.getSalary() + "§7원을 수령했습니다!");
            }
        }

        return true;
    }

    public Map<String, Company> getCompanies() {
        return Collections.unmodifiableMap(companies);
    }

    public List<String> getInvitedCompanyNames(UUID playerId) {
        return invitations.entrySet().stream()
                .filter(entry -> entry.getKey().equals(playerId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
