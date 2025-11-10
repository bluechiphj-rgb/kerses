package com.kwonpop.companylife.common.service

import com.kwonpop.companylife.common.scheduler.FoliaScheduler
import com.kwonpop.companylife.common.persistence.AuditRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class AuditService(
    private val repository: AuditRepository,
    private val scheduler: FoliaScheduler
) {
    private val queue = ConcurrentLinkedQueue<AuditEntry>()

    fun record(actor: UUID, action: String, detailJson: String, signature: String? = null) {
        queue.add(AuditEntry(actor, action, detailJson, signature))
        if (queue.size > 50) {
            flush()
        }
    }

    fun flush() {
        val batch = mutableListOf<AuditEntry>()
        while (true) {
            val next = queue.poll() ?: break
            batch += next
        }
        if (batch.isEmpty()) return
        scheduler.asyncExecutor().execute {
            repository.insertBatch(batch)
        }
    }
}

data class AuditEntry(
    val actor: UUID,
    val action: String,
    val detailJson: String,
    val signature: String?,
    val createdAt: Instant = Instant.now()
)
