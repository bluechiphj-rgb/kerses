package com.kwonpop.companylife.modules.contract

import com.kwonpop.companylife.common.persistence.ContractEntity
import com.kwonpop.companylife.common.persistence.ContractRepository
import com.kwonpop.companylife.common.service.EventBus
import java.time.Instant
import java.util.concurrent.CompletableFuture

class ContractService(private val repository: ContractRepository, private val eventBus: EventBus) {
    fun createContract(aCompany: Long, bCompany: Long, kind: String, terms: String): CompletableFuture<ContractEntity> {
        val entity = ContractEntity(
            aCompany = aCompany,
            bCompany = bCompany,
            kind = kind,
            termsJson = terms,
            status = "PENDING",
            signedA = true,
            signedB = false,
            expiresAt = Instant.now().plusSeconds(30L * 24 * 3600)
        )
        return repository.save(entity).thenApply { contract ->
            eventBus.publish(ContractCreatedEvent(contract))
            contract
        }
    }
}

data class ContractCreatedEvent(val contract: ContractEntity)
