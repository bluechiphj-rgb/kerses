package com.kwonpop.companylife.common.integration

import net.luckperms.api.LuckPerms
import net.luckperms.api.cacheddata.CachedPermissionData
import org.bukkit.plugin.ServicesManager
import java.util.UUID

class LuckPermsHelper(servicesManager: ServicesManager) {
    private val luckPerms: LuckPerms? = servicesManager.getRegistration(LuckPerms::class.java)?.provider

    fun hasPermission(uuid: UUID, node: String): Boolean {
        val user = luckPerms?.userManager?.getUser(uuid) ?: return false
        val data: CachedPermissionData = user.cachedData.permissionData
        return data.checkPermission(node).asBoolean()
    }
}
