package com.kwonpop.companylife

import com.kwonpop.companylife.api.CompanyLifeAPI
import com.kwonpop.companylife.common.command.CompanyCommand
import com.kwonpop.companylife.common.command.CompanyLifeAdminCommand
import com.kwonpop.companylife.common.config.ConfigService
import com.kwonpop.companylife.common.gui.GuiManager
import com.kwonpop.companylife.common.integration.DiscordWebhookClient
import com.kwonpop.companylife.common.integration.LuckPermsHelper
import com.kwonpop.companylife.common.integration.PlaceholderBridge
import com.kwonpop.companylife.common.integration.VaultBridge
import com.kwonpop.companylife.common.persistence.DatabaseManager
import com.kwonpop.companylife.common.scheduler.FoliaScheduler
import com.kwonpop.companylife.common.service.AuditService
import com.kwonpop.companylife.common.service.EventBus
import com.kwonpop.companylife.common.service.MessageService
import com.kwonpop.companylife.common.service.ServiceRegistry
import com.kwonpop.companylife.modules.ModuleLoader
import com.kwonpop.companylife.modules.analytics.AnalyticsModule
import com.kwonpop.companylife.modules.api.ApiModule
import com.kwonpop.companylife.modules.audit.AuditModule
import com.kwonpop.companylife.modules.company.CompanyModule
import com.kwonpop.companylife.modules.contract.ContractModule
import com.kwonpop.companylife.modules.core.CoreModule
import com.kwonpop.companylife.modules.crm.CrmModule
import com.kwonpop.companylife.modules.economy.EconomyModule
import com.kwonpop.companylife.modules.event.EventModule
import com.kwonpop.companylife.modules.finance.FinanceModule
import com.kwonpop.companylife.modules.franchise.FranchiseModule
import com.kwonpop.companylife.modules.gov.GovModule
import com.kwonpop.companylife.modules.hr.HrModule
import com.kwonpop.companylife.modules.integration.IntegrationModule
import com.kwonpop.companylife.modules.insurance.InsuranceModule
import com.kwonpop.companylife.modules.legal.LegalModule
import com.kwonpop.companylife.modules.logistics.LogisticsModule
import com.kwonpop.companylife.modules.manufacturing.ManufacturingModule
import com.kwonpop.companylife.modules.marketing.MarketingModule
import com.kwonpop.companylife.modules.payroll.PayrollModule
import com.kwonpop.companylife.modules.procurement.ProcurementModule
import com.kwonpop.companylife.modules.quest.QuestModule
import com.kwonpop.companylife.modules.r_and_d.ResearchModule
import com.kwonpop.companylife.modules.realestate.RealEstateModule
import com.kwonpop.companylife.modules.reputation.ReputationModule
import com.kwonpop.companylife.modules.risk.RiskModule
import com.kwonpop.companylife.modules.security.SecurityModule
import com.kwonpop.companylife.modules.stock.StockModule
import com.kwonpop.companylife.modules.store.StoreModule
import com.kwonpop.companylife.modules.store.StoreService
import com.kwonpop.companylife.modules.tax.TaxModule
import com.kwonpop.companylife.modules.tax.TaxService
import com.kwonpop.companylife.modules.transport.TransportModule
import com.kwonpop.companylife.modules.warehouse.WarehouseModule
import com.kwonpop.companylife.modules.warehouse.WarehouseService
import com.kwonpop.companylife.modules.compliance.ComplianceModule
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.java.JavaPlugin

class CompanyLifePlugin : JavaPlugin(), CompanyLifeAPI {

    private lateinit var audiences: BukkitAudiences
    private lateinit var services: ServiceRegistry
    private lateinit var moduleLoader: ModuleLoader

    override fun onLoad() {
        saveDefaultConfig()
        audiences = BukkitAudiences.create(this)
        services = ServiceRegistry()

        val scheduler = FoliaScheduler(this)
        val configService = ConfigService(this)
        val messageService = MessageService(configService, audiences)
        val eventBus = EventBus()
        val database = DatabaseManager(this, configService)
        val guiManager = GuiManager(this, scheduler, messageService)
        val auditService = AuditService(database.auditRepository, scheduler)
        val vaultBridge = VaultBridge(this.logger, server.servicesManager)
        val lpHelper = LuckPermsHelper(server.servicesManager)
        val placeholderBridge = PlaceholderBridge(this, scheduler)
        val webhookClient = DiscordWebhookClient { configService.root() }
        val storeService = StoreService(eventBus)
        val warehouseService = WarehouseService()
        val taxService = TaxService(database.ledgerRepository, eventBus)

        services.register(configService)
        services.register(messageService)
        services.register(eventBus)
        services.register(database)
        services.register(guiManager)
        services.register(scheduler)
        services.register(auditService)
        services.register(vaultBridge)
        services.register(lpHelper)
        services.register(placeholderBridge)
        services.register(webhookClient)
        services.register(storeService)
        services.register(warehouseService)
        services.register(taxService)

        database.migrate().join()

        moduleLoader = ModuleLoader(this, services)
        moduleLoader.register(
            CoreModule(),
            EconomyModule(),
            CompanyModule(),
            HrModule(),
            PayrollModule(),
            TaxModule(),
            StoreModule(),
            WarehouseModule(),
            LogisticsModule(),
            ContractModule(),
            LegalModule(),
            FinanceModule(),
            StockModule(),
            ResearchModule(),
            ManufacturingModule(),
            FranchiseModule(),
            RealEstateModule(),
            GovModule(),
            ProcurementModule(),
            TransportModule(),
            CrmModule(),
            MarketingModule(),
            AnalyticsModule(),
            QuestModule(),
            ReputationModule(),
            RiskModule(),
            InsuranceModule(),
            ComplianceModule(),
            EventModule(),
            SecurityModule(),
            AuditModule(),
            ApiModule(),
            IntegrationModule()
        )
    }

    override fun onEnable() {
        moduleLoader.enableAll()

        CompanyCommand(this, services).also { command ->
            getCommand("company")?.setExecutor(command)
            getCommand("company")?.setTabCompleter(command)
        }
        CompanyLifeAdminCommand(this, services).also { command ->
            getCommand("admin")?.setExecutor(command)
            getCommand("admin")?.setTabCompleter(command)
        }

        services.resolve<PlaceholderBridge>().hook()
    }

    override fun onDisable() {
        services.resolve<PlaceholderBridge>().unhook()
        moduleLoader.disableAll()
        if (::audiences.isInitialized) {
            audiences.close()
        }
    }

    override fun services(): ServiceRegistry = services
    override fun scheduler(): FoliaScheduler = services.resolve()
    override fun messages(): MessageService = services.resolve()
    override fun eventBus(): EventBus = services.resolve()
}
