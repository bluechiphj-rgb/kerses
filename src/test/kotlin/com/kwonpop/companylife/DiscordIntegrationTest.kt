package com.kwonpop.companylife

import com.kwonpop.companylife.common.config.RootConfig
import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class DiscordIntegrationTest {
    @Test
    fun `webhook send returns false when url blank`() {
        val root = RootConfig()
        val client = DiscordWebhookClient { root }
        val result = client.send("hello").get()
        assertFalse(result)
    }
}
