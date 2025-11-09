package com.kwonpop.companylife.modules

import com.kwonpop.companylife.common.service.ServiceRegistry

interface Module {
    val id: String
    val displayName: String
    val requiredServices: Set<Class<*>> get() = emptySet()

    fun onEnable(services: ServiceRegistry)
    fun onDisable(services: ServiceRegistry)
}
