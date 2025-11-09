package com.example.companyroleplay.model;

import java.util.*;

public class Company {
    private final String name;
    private final UUID owner;
    private final Map<UUID, CompanyMember> members = new HashMap<>();

    public Company(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public Map<UUID, CompanyMember> getMembers() {
        return members;
    }

    public void addMember(CompanyMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isManager(UUID uuid) {
        if (owner.equals(uuid)) {
            return true;
        }

        CompanyMember member = members.get(uuid);
        return member != null && member.isManager();
    }

    public List<CompanyMember> getSortedMembers() {
        List<CompanyMember> list = new ArrayList<>(members.values());
        list.sort(Comparator.comparingInt(CompanyMember::getRolePriority)
                .thenComparing(CompanyMember::getRole, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CompanyMember::getNameIfKnown, String.CASE_INSENSITIVE_ORDER));
        return list;
    }
}
