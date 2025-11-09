package com.kwonpop.companylife.modules.r_and_d

import java.util.concurrent.ConcurrentHashMap

class ResearchService {
    private val projects = ConcurrentHashMap<String, ResearchProject>()

    fun start(topic: String, points: Int, successRate: Double): ResearchProject {
        val project = ResearchProject(topic, points, successRate, status = "RUNNING")
        projects[topic] = project
        return project
    }

    fun complete(topic: String, success: Boolean): ResearchProject? {
        val project = projects[topic] ?: return null
        val status = if (success) "SUCCESS" else "FAILED"
        val updated = project.copy(status = status)
        projects[topic] = updated
        return updated
    }

    fun list(): List<ResearchProject> = projects.values.toList()
}

data class ResearchProject(val topic: String, val points: Int, val successRate: Double, val status: String)
