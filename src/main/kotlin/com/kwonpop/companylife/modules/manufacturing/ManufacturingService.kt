package com.kwonpop.companylife.modules.manufacturing

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class ManufacturingService {
    private val jobs = ConcurrentLinkedQueue<ManufacturingJob>()

    fun schedule(recipeId: String, quantity: Int, duration: Duration): ManufacturingJob {
        val job = ManufacturingJob(recipeId, quantity, Instant.now().plus(duration), "SCHEDULED")
        jobs.add(job)
        return job
    }

    fun completeNext(success: Boolean): ManufacturingJob? {
        val job = jobs.poll() ?: return null
        val status = if (success) "COMPLETED" else "FAILED"
        val updated = job.copy(status = status)
        jobs.add(updated)
        return updated
    }

    fun queue(): List<ManufacturingJob> = jobs.toList()
}

data class ManufacturingJob(val recipeId: String, val quantity: Int, val eta: Instant, val status: String)
