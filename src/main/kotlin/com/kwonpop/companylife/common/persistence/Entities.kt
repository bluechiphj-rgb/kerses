package com.kwonpop.companylife.common.persistence

import java.time.Instant
import java.util.UUID

data class CompanyEntity(
    val id: Long? = null,
    val name: String,
    val type: String,
    val regNo: String,
    val reputation: Double,
    val createdAt: Instant = Instant.now()
)

data class BranchEntity(
    val id: Long? = null,
    val companyId: Long,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val rent: Double,
    val upkeep: Double
)

data class LedgerEntryEntity(
    val id: Long? = null,
    val companyId: Long,
    val entryAt: Instant,
    val debitAccount: String,
    val creditAccount: String,
    val amount: Double,
    val memo: String
)

data class PayrollRunEntity(
    val id: Long? = null,
    val companyId: Long,
    val period: String,
    val gross: Double,
    val net: Double,
    val processedAt: Instant = Instant.now()
)

data class ContractEntity(
    val id: Long? = null,
    val aCompany: Long,
    val bCompany: Long,
    val kind: String,
    val termsJson: String,
    val status: String,
    val signedA: Boolean,
    val signedB: Boolean,
    val expiresAt: Instant
)

data class BidEntity(
    val id: Long? = null,
    val tenderId: Long,
    val companyId: Long,
    val price: Double,
    val leadTime: Int,
    val score: Double
)

data class LogisticsJobEntity(
    val id: Long? = null,
    val orderId: Long,
    val vehicleId: String,
    val routeJson: String,
    val eta: Instant,
    val sla: String
)

data class AuditRecordEntity(
    val id: Long? = null,
    val actor: UUID,
    val action: String,
    val detailJson: String,
    val createdAt: Instant,
    val signature: String?
)
