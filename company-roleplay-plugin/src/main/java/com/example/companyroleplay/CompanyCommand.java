package com.example.companyroleplay;

import com.example.companyroleplay.model.Company;
import com.example.companyroleplay.model.CompanyMember;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class CompanyCommand implements CommandExecutor, TabCompleter {
    private final CompanyRoleplayPlugin plugin;
    private final CompanyManager companyManager;
    private final String prefix;

    public CompanyCommand(CompanyRoleplayPlugin plugin, CompanyManager companyManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.prefix = plugin.getConfig().getString("messages.prefix", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§c사용법: /company create <회사명>");
                    return true;
                }
                String name = args[1];
                if (companyManager.createCompany(player, name)) {
                    player.sendMessage(prefix + "§a회사 '" + name + "'을(를) 설립했습니다!");
                } else {
                    player.sendMessage(prefix + "§c이미 존재하는 회사이거나 이미 다른 회사에 소속되어 있습니다.");
                }
                return true;
            case "disband":
                if (companyManager.disbandCompany(player)) {
                    player.sendMessage(prefix + "§c회사를 해산했습니다.");
                } else {
                    player.sendMessage(prefix + "§c당신은 회사를 해산할 수 없습니다.");
                }
                return true;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§c사용법: /company invite <플레이어>");
                    return true;
                }
                OfflinePlayer targetInvite = Bukkit.getOfflinePlayer(args[1]);
                if (!targetInvite.hasPlayedBefore() && !targetInvite.isOnline()) {
                    player.sendMessage(prefix + "§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                String inviteName = targetInvite.getName() != null ? targetInvite.getName() : args[1];
                if (companyManager.invitePlayer(player, targetInvite)) {
                    player.sendMessage(prefix + "§a" + inviteName + "님에게 회사 초대를 보냈습니다.");
                    if (targetInvite.isOnline()) {
                        targetInvite.getPlayer().sendMessage(prefix + "§a" + player.getName() + "§7님이 §a" + companyManager.getCompany(player.getUniqueId()).map(Company::getName).orElse("?") + "§7 회사에 초대했습니다! /company join 으로 수락하세요.");
                    }
                } else {
                    player.sendMessage(prefix + "§c회사를 초대할 권한이 없거나 이미 초대한 상태입니다.");
                }
                return true;
            case "join":
                String requestedCompany = args.length >= 2 ? args[1] : null;
                if (companyManager.acceptInvite(player, requestedCompany)) {
                    player.sendMessage(prefix + "§a회사를 수락했습니다!");
                } else {
                    player.sendMessage(prefix + "§c수락할 수 있는 초대가 없거나 다른 회사를 입력했습니다.");
                }
                return true;
            case "leave":
                if (companyManager.leaveCompany(player)) {
                    player.sendMessage(prefix + "§c회사를 탈퇴했습니다.");
                } else {
                    player.sendMessage(prefix + "§c회사를 탈퇴할 수 없습니다. (대표는 탈퇴 불가)");
                }
                return true;
            case "setrole":
                if (args.length < 3) {
                    player.sendMessage(prefix + "§c사용법: /company setrole <플레이어> <직급>");
                    return true;
                }
                OfflinePlayer roleTarget = Bukkit.getOfflinePlayer(args[1]);
                String role = args[2];
                String roleName = roleTarget.getName() != null ? roleTarget.getName() : args[1];
                if (companyManager.setRole(player, roleTarget, role)) {
                    player.sendMessage(prefix + "§a" + roleName + "님의 직급을 " + role + "(으)로 설정했습니다.");
                } else {
                    player.sendMessage(prefix + "§c직급을 설정할 수 없습니다.");
                }
                return true;
            case "setsalary":
                if (args.length < 3) {
                    player.sendMessage(prefix + "§c사용법: /company setsalary <플레이어> <금액>");
                    return true;
                }
                OfflinePlayer salaryTarget = Bukkit.getOfflinePlayer(args[1]);
                double salary;
                try {
                    salary = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + "§c금액은 숫자여야 합니다.");
                    return true;
                }
                String salaryName = salaryTarget.getName() != null ? salaryTarget.getName() : args[1];
                if (companyManager.setSalary(player, salaryTarget, salary)) {
                    player.sendMessage(prefix + "§a" + salaryName + "님의 급여를 " + salary + "(으)로 설정했습니다.");
                } else {
                    player.sendMessage(prefix + "§c급여를 설정할 수 없습니다.");
                }
                return true;
            case "payday":
                if (companyManager.paySalaries(player)) {
                    player.sendMessage(prefix + "§a직원들에게 급여를 지급했습니다.");
                } else {
                    player.sendMessage(prefix + "§c급여 지급에 실패했습니다. Vault가 설치되었는지 확인하세요.");
                }
                return true;
            case "info":
                Optional<Company> companyOpt = args.length >= 2
                        ? companyManager.getCompany(args[1])
                        : companyManager.getCompany(player.getUniqueId());
                if (!companyOpt.isPresent()) {
                    player.sendMessage(prefix + "§c회사를 찾을 수 없습니다.");
                    return true;
                }
                sendCompanyInfo(player, companyOpt.get());
                return true;
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendCompanyInfo(Player player, Company company) {
        String ownerName = Optional.ofNullable(Bukkit.getOfflinePlayer(company.getOwner()).getName()).orElse("알 수 없음");
        player.sendMessage(prefix + "§6===== §e" + company.getName() + " 회사 정보 §6=====");
        player.sendMessage(prefix + "§7대표: §a" + ownerName);
        for (CompanyMember member : company.getSortedMembers()) {
            String name = member.getNameIfKnown();
            player.sendMessage(prefix + "§7 - §a" + name + " §7| 직급: §e" + member.getRole() + " §7| 급여: §b" + member.getSalary());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(prefix + "§6/company create <이름> §7- 회사 설립");
        player.sendMessage(prefix + "§6/company disband §7- 회사 해산");
        player.sendMessage(prefix + "§6/company invite <플레이어> §7- 직원 초대");
        player.sendMessage(prefix + "§6/company join [회사명] §7- 초대 수락");
        player.sendMessage(prefix + "§6/company leave §7- 회사 탈퇴");
        player.sendMessage(prefix + "§6/company setrole <플레이어> <직급> §7- 직급 설정");
        player.sendMessage(prefix + "§6/company setsalary <플레이어> <급여> §7- 급여 설정");
        player.sendMessage(prefix + "§6/company payday §7- 급여 지급 (Vault 필요)");
        player.sendMessage(prefix + "§6/company info [회사명] §7- 회사 정보 확인");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            return Arrays.asList("create", "disband", "invite", "join", "leave", "setrole", "setsalary", "payday", "info", "help")
                    .stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite":
            case "setrole":
            case "setsalary":
                if (args.length == 2) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                }
                if (args[0].equalsIgnoreCase("setrole") && args.length == 3) {
                    return Arrays.asList("CEO", "Director", "Manager", "Staff", "Intern")
                            .stream()
                            .filter(role -> role.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                }
                break;
            case "info":
                if (args.length == 2) {
                    String search = args[1].toLowerCase(Locale.ROOT);
                    return companyManager.getCompanies().values().stream()
                            .map(Company::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(search))
                            .collect(Collectors.toList());
                }
                break;
        }

        return Collections.emptyList();
    }
}
