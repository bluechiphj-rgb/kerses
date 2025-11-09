package com.example.companyroleplay.model;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class CompanyMember {
    private final UUID uuid;
    private String role;
    private double salary;

    public CompanyMember(UUID uuid, String role, double salary) {
        this.uuid = uuid;
        this.role = role;
        this.salary = salary;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public boolean isManager() {
        return "CEO".equalsIgnoreCase(role) || "Director".equalsIgnoreCase(role) || "Manager".equalsIgnoreCase(role);
    }

    public int getRolePriority() {
        if (role == null) {
            return 100;
        }
        switch (role.toLowerCase()) {
            case "ceo":
                return 0;
            case "director":
                return 1;
            case "manager":
                return 2;
            case "staff":
                return 3;
            case "intern":
                return 4;
            default:
                return 5;
        }
    }

    public String getNameIfKnown() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        return name == null ? uuid.toString() : name;
    }
}
