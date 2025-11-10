package com.kwonpop.companylife.common.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.kwonpop.companylife.common.config.RootConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

fun interface AnalyticsConfigProvider {
    fun analytics(): RootConfig
}

class DiscordWebhookClient(private val provider: AnalyticsConfigProvider) {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val mapper = ObjectMapper()

    fun send(message: String): CompletableFuture<Boolean> {
        val url = provider.analytics().analytics.discord_webhook_url
        if (url.isBlank()) {
            return CompletableFuture.completedFuture(false)
        }
        val body = mapper.writeValueAsString(mapOf("content" to message))
        val request = HttpRequest.newBuilder(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build()
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply { response ->
            response.statusCode() in 200..299
        }
    }
}
