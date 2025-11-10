package com.kwonpop.companylife.common.cache

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class EntityCache<K, V>(private val ttl: Duration) {
    private val map = ConcurrentHashMap<K, CacheEntry<V>>()

    fun get(key: K, supplier: () -> V): V {
        val existing = map[key]
        if (existing != null && existing.expiresAt.isAfter(Instant.now())) {
            return existing.value
        }
        val value = supplier()
        map[key] = CacheEntry(value, Instant.now().plus(ttl))
        return value
    }

    fun invalidate(key: K) {
        map.remove(key)
    }

    fun clear() {
        map.clear()
    }

    private data class CacheEntry<V>(val value: V, val expiresAt: Instant)
}
