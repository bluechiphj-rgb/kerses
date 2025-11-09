package com.kwonpop.companylife.common.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class EventBus {
    private val listeners: MutableMap<Class<*>, CopyOnWriteArrayList<Consumer<Any>>> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> subscribe(event: Class<T>, listener: Consumer<T>) {
        listeners.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(listener as Consumer<Any>)
    }

    fun <T : Any> publish(event: T) {
        listeners[event.javaClass]?.forEach { it.accept(event) }
    }
}
