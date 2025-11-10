package com.kwonpop.companylife.common.persistence

import com.kwonpop.companylife.common.service.AuditEntry
import com.kwonpop.companylife.modules.economy.LedgerGateway
import java.sql.ResultSet
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.sql.DataSource

abstract class AbstractRepository(protected val dataSource: DataSource, private val executor: Executor) {
    protected fun <T> supplyAsync(supplier: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(supplier, executor)

    protected fun connection() = dataSource.connection
}

class CompanyRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), com.kwonpop.companylife.modules.company.CompanyGateway {
    override fun create(entity: CompanyEntity): CompletableFuture<CompanyEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO companies(name, type, reg_no, reputation, created_at) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setString(1, entity.name)
                it.setString(2, entity.type)
                it.setString(3, entity.regNo)
                it.setDouble(4, entity.reputation)
                it.setString(5, entity.createdAt.toString())
                it.executeUpdate()
                val rs = it.generatedKeys
                rs.use { keys ->
                    entity.copy(id = if (keys.next()) keys.getLong(1) else null)
                }
            }
        }
    }

    fun findById(id: Long): CompletableFuture<CompanyEntity?> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM companies WHERE id = ?")
            stmt.use {
                it.setLong(1, id)
                val rs = it.executeQuery()
                rs.use { result -> if (result.next()) result.toCompany() else null }
            }
        }
    }
}

class BranchRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), com.kwonpop.companylife.modules.company.BranchGateway {
    override fun create(entity: BranchEntity): CompletableFuture<BranchEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO branches(company_id, world, x, y, z, rent, upkeep) VALUES (?,?,?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, entity.companyId)
                it.setString(2, entity.world)
                it.setDouble(3, entity.x)
                it.setDouble(4, entity.y)
                it.setDouble(5, entity.z)
                it.setDouble(6, entity.rent)
                it.setDouble(7, entity.upkeep)
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> entity.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }
}

class LedgerRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), LedgerGateway {
    override fun append(entity: LedgerEntryEntity): CompletableFuture<LedgerEntryEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO ledgers(company_id, entry_at, debit_acct, credit_acct, amount, memo) VALUES (?,?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, entity.companyId)
                it.setString(2, entity.entryAt.toString())
                it.setString(3, entity.debitAccount)
                it.setString(4, entity.creditAccount)
                it.setDouble(5, entity.amount)
                it.setString(6, entity.memo)
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> entity.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }

    override fun sumForPeriod(companyId: Long, account: String, periodStart: Instant, periodEnd: Instant): CompletableFuture<Double> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM ledgers WHERE company_id = ? AND debit_acct = ? AND entry_at BETWEEN ? AND ?"
            )
            stmt.use {
                it.setLong(1, companyId)
                it.setString(2, account)
                it.setString(3, periodStart.toString())
                it.setString(4, periodEnd.toString())
                val rs = it.executeQuery()
                rs.use { result -> if (result.next()) result.getDouble(1) else 0.0 }
            }
        }
    }
}

class PayrollRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), com.kwonpop.companylife.modules.payroll.PayrollGateway {
    override fun record(run: PayrollRunEntity): CompletableFuture<PayrollRunEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO payroll_runs(company_id, period, gross, net, processed_at) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, run.companyId)
                it.setString(2, run.period)
                it.setDouble(3, run.gross)
                it.setDouble(4, run.net)
                it.setString(5, run.processedAt.toString())
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> run.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }
}

class ContractRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor) {
    fun save(contract: ContractEntity): CompletableFuture<ContractEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO contracts(a_company, b_company, kind, terms_json, status, signed_a, signed_b, expires_at) VALUES (?,?,?,?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, contract.aCompany)
                it.setLong(2, contract.bCompany)
                it.setString(3, contract.kind)
                it.setString(4, contract.termsJson)
                it.setString(5, contract.status)
                it.setBoolean(6, contract.signedA)
                it.setBoolean(7, contract.signedB)
                it.setString(8, contract.expiresAt.toString())
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> contract.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }
}

class ProcurementRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), com.kwonpop.companylife.modules.procurement.ProcurementGateway {
    override fun save(bid: BidEntity): CompletableFuture<BidEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO bids(tender_id, company_id, price, lead_time, score) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, bid.tenderId)
                it.setLong(2, bid.companyId)
                it.setDouble(3, bid.price)
                it.setInt(4, bid.leadTime)
                it.setDouble(5, bid.score)
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> bid.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }
}

class LogisticsRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor), com.kwonpop.companylife.modules.logistics.LogisticsGateway {
    override fun create(job: LogisticsJobEntity): CompletableFuture<LogisticsJobEntity> = supplyAsync {
        connection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO deliveries(order_id, vehicle_id, route_json, eta, sla) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setLong(1, job.orderId)
                it.setString(2, job.vehicleId)
                it.setString(3, job.routeJson)
                it.setString(4, job.eta.toString())
                it.setString(5, job.sla)
                it.executeUpdate()
                val keys = it.generatedKeys
                keys.use { rs -> job.copy(id = if (rs.next()) rs.getLong(1) else null) }
            }
        }
    }
}

class AuditRepository(dataSource: DataSource, executor: Executor) : AbstractRepository(dataSource, executor) {
    fun insertBatch(entries: List<AuditEntry>): CompletableFuture<Unit> = supplyAsync {
        connection().use { conn ->
            conn.autoCommit = false
            val stmt = conn.prepareStatement(
                "INSERT INTO audits(actor_uuid, action, detail_json, created_at, sig) VALUES (?,?,?,?,?)"
            )
            stmt.use { prepared ->
                entries.forEach { entry ->
                    prepared.setString(1, entry.actor.toString())
                    prepared.setString(2, entry.action)
                    prepared.setString(3, entry.detailJson)
                    prepared.setString(4, entry.createdAt.toString())
                    prepared.setString(5, entry.signature)
                    prepared.addBatch()
                }
                prepared.executeBatch()
            }
            conn.commit()
        }
        Unit
    }
}

private fun ResultSet.toCompany() = CompanyEntity(
    id = getLong("id"),
    name = getString("name"),
    type = getString("type"),
    regNo = getString("reg_no"),
    reputation = getDouble("reputation"),
    createdAt = Instant.parse(getString("created_at"))
)
