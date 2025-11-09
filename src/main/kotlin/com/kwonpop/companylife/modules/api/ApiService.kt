package com.kwonpop.companylife.modules.api

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ApiService {
    private val tokens = ConcurrentHashMap<UUID, String>()

    fun issueToken(user: UUID): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        tokens[user] = token
        return token
    }

    fun validate(user: UUID, token: String): Boolean = tokens[user] == token
}
