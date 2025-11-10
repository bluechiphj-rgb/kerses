package com.kwonpop.companylife.common.scheduler

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class FoliaScheduler(private val plugin: Plugin) {

    fun runGlobal(task: Runnable): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        plugin.server.globalRegionScheduler.run(plugin) { _ ->
            try {
                task.run()
                future.complete(null)
            } catch (ex: Exception) {
                future.completeExceptionally(ex)
            }
        }
        return future
    }

    fun runAtLocation(location: Location, task: Runnable): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        plugin.server.regionScheduler.run(plugin, location) { _ ->
            try {
                task.run()
                future.complete(null)
            } catch (ex: Exception) {
                future.completeExceptionally(ex)
            }
        }
        return future
    }

    fun runEntity(entity: Entity, task: Runnable): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        plugin.server.entityScheduler.run(plugin, entity) { _ ->
            try {
                task.run()
                future.complete(null)
            } catch (ex: Exception) {
                future.completeExceptionally(ex)
            }
        }
        return future
    }

    fun laterGlobal(delay: Duration, task: Runnable): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        plugin.server.globalRegionScheduler.runDelayed(plugin, { _ ->
            try {
                task.run()
                future.complete(null)
            } catch (ex: Exception) {
                future.completeExceptionally(ex)
            }
        }, delay.toMillis() / 50)
        return future
    }

    fun asyncExecutor(): Executor = Executor { runnable ->
        plugin.server.asyncScheduler.runNow(plugin, { _ -> runnable.run() })
    }
}
