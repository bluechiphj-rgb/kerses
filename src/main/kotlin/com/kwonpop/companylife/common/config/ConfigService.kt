package com.kwonpop.companylife.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ConfigService(private val plugin: Plugin) {
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private val messageCache = ConcurrentHashMap<String, String>()
    private lateinit var config: RootConfig

    init {
        reload()
    }

    fun reload() {
        val file = File(plugin.dataFolder, "config.yml")
        if (!file.exists()) {
            plugin.saveResource("config.yml", false)
        }
        config = mapper.readValue(file, RootConfig::class.java)
        config.messages.forEach { (k, v) -> messageCache[k] = v }
    }

    fun root(): RootConfig = config
    fun messages(): Map<String, String> = messageCache
}

data class RootConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val economy: EconomyConfig = EconomyConfig(),
    val legal: LegalConfig = LegalConfig(),
    val payroll: PayrollConfig = PayrollConfig(),
    val manufacturing: ManufacturingConfig = ManufacturingConfig(),
    val analytics: AnalyticsConfig = AnalyticsConfig(),
    val autobuild: AutoBuildConfig = AutoBuildConfig(),
    val i18n: List<String> = listOf("ko_KR", "en_US"),
    val debug: Boolean = false,
    val messages: Map<String, String> = defaultMessages()
)

data class DatabaseConfig(
    val type: String = "sqlite",
    val file: String = "plugins/CompanyLife/data.db",
    val mysql: MysqlConfig = MysqlConfig()
)

data class MysqlConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "companylife",
    val username: String = "root",
    val password: String = "password"
)

data class PerformanceConfig(
    val cache_sizes: Map<String, Int> = mapOf("companies" to 2000, "products" to 5000),
    val async_db: Boolean = true,
    val rate_limits: Map<String, Int> = mapOf(
        "money_transfer_per_min" to 20,
        "contract_per_hour" to 10
    )
)

data class EconomyConfig(
    val vault: Boolean = true,
    val starting_company_balance: Long = 10_000
)

data class LegalConfig(
    val permits_required: Boolean = true,
    val anti_abuse_thresholds: Map<String, Long> = mapOf(
        "max_transfer" to 500_000,
        "daily_withdraw_limit" to 1_000_000
    )
)

data class PayrollConfig(
    val cycle: String = "WEEKLY",
    val overtime_multiplier: Double = 1.5
)

data class ManufacturingConfig(
    val enable: Boolean = true,
    val breakdown_chance: Double = 0.01
)

data class AnalyticsConfig(
    val enable_webhook: Boolean = true,
    val discord_webhook_url: String = ""
)

data class AutoBuildConfig(
    val enabled: Boolean = false,
    val blueprints: List<CompanyBlueprintConfig> = emptyList()
)

data class CompanyBlueprintConfig(
    val key: String,
    val displayName: String,
    val type: String = "LLC",
    val regNo: String? = null,
    val branches: List<BranchBlueprintConfig> = emptyList(),
    val inventory: List<ProductBlueprintConfig> = emptyList()
)

data class BranchBlueprintConfig(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val openStore: Boolean = true
)

data class ProductBlueprintConfig(
    val branch: String,
    val sku: String,
    val name: String,
    val cost: Double,
    val price: Double,
    val reorderPoint: Int,
    val initialWarehouseStock: Int = 0,
    val initialStoreStock: Int = 0,
    val holdingCost: Double = 5.0,
    val orderCost: Double = 100.0,
    val expectedDemand: Int = 120,
    val leadTimeDays: Int = 3
)

fun defaultMessages(): Map<String, String> = mapOf(
    "prefix" to "<gradient:#4ecdc4:#556270>[CompanyLife]</gradient> ",
    "company.created" to "{prefix}<green>새 회사가 설립되었습니다: <yellow>{name}</yellow></green>",
    "company.branch.created" to "{prefix}<green>새 지점이 등록되었습니다: <yellow>{branch}</yellow></green>",
    "company.autobuild.success" to "{prefix}<green>{name}</green> 자동 빌드 완료! <gray>지점 {branches}곳 / 제품 {products}개</gray>",
    "company.autobuild.failed" to "{prefix}<red>자동 빌드 실패:</red> {error}",
    "company.autobuild.disabled" to "{prefix}<red>자동 빌드가 비활성화되어 있습니다.</red>",
    "company.autobuild.unknown" to "{prefix}<red>자동 빌드 설계도 '{key}'를 찾을 수 없습니다.</red>",
    "command.only_player" to "{prefix}<red>플레이어만 사용 가능합니다.</red>",
    "command.no_permission" to "{prefix}<red>권한이 부족합니다.</red>",
    "command.invalid_usage" to "{prefix}<red>명령어 사용법이 올바르지 않습니다.</red>"
)
