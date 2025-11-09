package com.kwonpop.companylife.api

import com.kwonpop.companylife.common.scheduler.FoliaScheduler
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.MessageService
import com.kwonpop.companylife.common.service.ServiceRegistry

interface CompanyLifeAPI {
    fun services(): ServiceRegistry
    fun scheduler(): FoliaScheduler
    fun messages(): MessageService
    fun eventBus(): EventBus
}
