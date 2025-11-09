package com.kwonpop.companylife.modules.analytics

import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

class AnalyticsService(private val webhookClient: DiscordWebhookClient) {
    fun publishKpi(kpi: Map<String, Number>): CompletableFuture<Boolean> {
        val lines = kpi.entries.joinToString("\n") { (k, v) -> "$k: $v" }
        return webhookClient.send("**KPI 업데이트**\n$lines\n${Instant.now()}")
    }
}
