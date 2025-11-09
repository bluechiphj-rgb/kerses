package com.kwonpop.companylife.common.service

import com.kwonpop.companylife.common.gui.ModuleDashboardGui

inline fun <reified T : Any> ServiceRegistry.resolveOrNull(): T? = try {
    resolve()
} catch (_: IllegalStateException) {
    null
}

fun ServiceRegistry.dashboardRegistry(): ModuleDashboardGui.Registry {
    return resolveOrNull() ?: ModuleDashboardGui.Registry().also { register(it) }
}
