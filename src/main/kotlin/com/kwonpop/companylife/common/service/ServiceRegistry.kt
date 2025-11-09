package com.kwonpop.companylife.common.service

import java.util.concurrent.ConcurrentHashMap

class ServiceRegistry {
    private val services = ConcurrentHashMap<Class<*>, Any>()

    fun <T : Any> register(service: T) {
        services[service.javaClass] = service
    }

    inline fun <reified T : Any> resolve(): T {
        return resolve(T::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(type: Class<T>): T {
        return services.entries.firstOrNull { type.isAssignableFrom(it.key) }
            ?.value as? T ?: throw IllegalStateException("Service ${type.simpleName} not registered")
    }

    fun contains(type: Class<*>) = services.keys.any { type.isAssignableFrom(it) }
}
