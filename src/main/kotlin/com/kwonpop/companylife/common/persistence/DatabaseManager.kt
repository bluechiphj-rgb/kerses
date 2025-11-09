package com.kwonpop.companylife.common.persistence

import com.kwonpop.companylife.common.config.ConfigService
import com.kwonpop.companylife.common.config.DatabaseConfig
import org.bukkit.plugin.Plugin
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class DatabaseManager(
    private val plugin: Plugin,
    private val configService: ConfigService
) {
    private val executor: ExecutorService = Executors.newFixedThreadPool(8)
    val dataSource: DataSource = createDataSource(configService.root().database)

    val companyRepository = CompanyRepository(dataSource, executor)
    val branchRepository = BranchRepository(dataSource, executor)
    val ledgerRepository = LedgerRepository(dataSource, executor)
    val payrollRepository = PayrollRepository(dataSource, executor)
    val contractRepository = ContractRepository(dataSource, executor)
    val procurementRepository = ProcurementRepository(dataSource, executor)
    val logisticsRepository = LogisticsRepository(dataSource, executor)
    val auditRepository = AuditRepository(dataSource, executor)

    fun migrate(): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync({
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate()
            Unit
        }, executor)
    }

    private fun createDataSource(databaseConfig: DatabaseConfig): DataSource {
        val hikariConfig = HikariConfig().apply {
            maximumPoolSize = 12
            isAutoCommit = false
            poolName = "CompanyLife-Hikari"
            when (databaseConfig.type.lowercase()) {
                "mysql" -> {
                    jdbcUrl = "jdbc:mysql://${databaseConfig.mysql.host}:${databaseConfig.mysql.port}/${databaseConfig.mysql.database}"
                    username = databaseConfig.mysql.username
                    password = databaseConfig.mysql.password
                }
                else -> {
                    val file = plugin.dataFolder.toPath().resolve("data.db").toFile()
                    file.parentFile.mkdirs()
                    jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                }
            }
        }
        return HikariDataSource(hikariConfig)
    }
}
