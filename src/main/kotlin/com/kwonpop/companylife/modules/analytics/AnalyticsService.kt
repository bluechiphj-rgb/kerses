package com.kwonpop.companylife.modules.analytics

import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

class AnalyticsService(private val webhookClient: DiscordWebhookClient) {
    fun publishKpi(kpi: Map<String, Number>): CompletableFuture<Boolean> {
        val lines = kpi.entries.joinToString("\n") { (k, v) -> "$k: $v" }
        return webhookClient.send("**KPI 업데이트**\n$lines\n${Instant.now()}")
    }

    fun buildSnapshot(
        revenue: Double,
        profit: Double,
        payrollCost: Double,
        inventoryValue: Double,
        slaHitRate: Double,
        reputation: Double,
        sendWebhook: Boolean = false
    ): CompletableFuture<KpiSnapshot> {
        val margin = if (revenue == 0.0) 0.0 else profit / revenue
        val snapshot = KpiSnapshot(revenue, profit, payrollCost, inventoryValue, slaHitRate, reputation, margin)
        return if (sendWebhook) {
            val payload = mapOf(
                "Revenue" to revenue,
                "Profit" to profit,
                "Payroll" to payrollCost,
                "InventoryValue" to inventoryValue,
                "SLA" to slaHitRate,
                "Reputation" to reputation,
                "Margin" to margin
            )
            publishKpi(payload).thenApply { snapshot }
        } else {
            CompletableFuture.completedFuture(snapshot)
        }
    }
}

data class KpiSnapshot(
    val revenue: Double,
    val profit: Double,
    val payrollCost: Double,
    val inventoryValue: Double,
    val slaHitRate: Double,
    val reputation: Double,
    val margin: Double
)
