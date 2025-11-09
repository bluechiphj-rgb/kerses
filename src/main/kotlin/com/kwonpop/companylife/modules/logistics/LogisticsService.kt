package com.kwonpop.companylife.modules.logistics

import com.kwonpop.companylife.common.persistence.LogisticsJobEntity
import com.kwonpop.companylife.common.service.EventBus
import java.time.Duration
import java.time.Instant
import java.util.PriorityQueue
import java.util.concurrent.CompletableFuture
import kotlin.math.max

interface LogisticsGateway {
    fun create(job: LogisticsJobEntity): CompletableFuture<LogisticsJobEntity>
}

class LogisticsService(
    private val repository: LogisticsGateway,
    private val eventBus: EventBus
) {
    fun planRoute(graph: Map<String, Map<String, Double>>, start: String, end: String): RoutePlan {
        val distances = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val previous = mutableMapOf<String, String?>()
        val queue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        distances[start] = 0.0
        queue.add(start to 0.0)

        while (queue.isNotEmpty()) {
            val (node, dist) = queue.poll()
            if (node == end) break
            val neighbors = graph[node] ?: continue
            neighbors.forEach { (neighbor, cost) ->
                val alt = dist + cost
                if (alt < distances.getValue(neighbor)) {
                    distances[neighbor] = alt
                    previous[neighbor] = node
                    queue.add(neighbor to alt)
                }
            }
        }

        val path = mutableListOf<String>()
        var current: String? = end
        while (current != null) {
            path.add(0, current)
            current = previous[current]
        }
        val total = distances.getOrElse(end) { Double.POSITIVE_INFINITY }
        return RoutePlan(path, total)
    }

    fun dispatch(orderId: Long, vehicleId: String, route: RoutePlan, slaMinutes: Long): CompletableFuture<LogisticsJobEntity> {
        val eta = Instant.now().plus(Duration.ofMinutes(route.cost.toLong().coerceAtLeast(1)))
        val slaStatus = if (route.cost <= slaMinutes) "HIT" else "MISS"
        val job = LogisticsJobEntity(
            orderId = orderId,
            vehicleId = vehicleId,
            routeJson = route.nodes.joinToString(","),
            eta = eta,
            sla = slaStatus
        )
        val event = DeliveryDispatchedEvent(orderId, vehicleId, route, slaStatus)
        eventBus.publish(event)
        return repository.create(job).thenApply { saved ->
            eventBus.publish(DeliveryResultEvent(orderId, saved.sla))
            saved
        }
    }

    fun evaluateSla(orderId: Long, slaStatus: String, travelMinutes: Double, completedAt: Instant): CompletableFuture<LogisticsEvaluation> {
        val score = when (slaStatus) {
            "HIT" -> 100.0
            "MISS" -> max(0.0, 100 - travelMinutes)
            else -> 50.0
        }
        val evaluation = LogisticsEvaluation(orderId, completedAt, slaStatus, score)
        eventBus.publish(DeliverySlaEvaluatedEvent(evaluation))
        return CompletableFuture.completedFuture(evaluation)
    }
}

data class RoutePlan(val nodes: List<String>, val cost: Double)
data class DeliveryDispatchedEvent(val orderId: Long, val vehicleId: String, val routePlan: RoutePlan, val slaStatus: String)
data class DeliveryResultEvent(val orderId: Long, val slaResult: String)
data class LogisticsEvaluation(val orderId: Long, val evaluatedAt: Instant, val slaResult: String, val slaScore: Double)
data class DeliverySlaEvaluatedEvent(val evaluation: LogisticsEvaluation)
