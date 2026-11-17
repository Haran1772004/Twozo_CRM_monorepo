package com.tlc.crm.common.initializer;

import com.tlc.commons.executor.context.registry.ContextCodecRegistry;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.iam.cleaner.ActionLimitCleaner;
import com.tlc.crm.common.iam.internal.OrgInitiatorController;
import com.tlc.crm.common.iam.internal.UserMigrationService;
import com.tlc.crm.common.action.CrmPreflightHandler;
import com.tlc.crm.common.internal.listeners.UserRoleRowListener;
import com.tlc.crm.common.models.CommonFields;

import com.tlc.crm.common.iam.internal.permission.FollowerAccessRestrictedRolesProvider;
import com.tlc.crm.common.iam.internal.permission.UserTerritoryProvider;
import com.tlc.crm.common.internal.context.iam.security.SecurityContextCodec;
import com.tlc.crm.common.internal.listeners.RoleRowListener;
import com.tlc.crm.common.internal.export.DataExportConsumerController;
import com.tlc.crm.common.internal.listeners.DataExportRowListener;
import com.tlc.crm.common.internal.listeners.RemainingActionCountRowListener;
import com.tlc.crm.common.internal.listeners.UserPreferenceListener;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.crm.common.sql.resource.OMCRMPREFERENCE;
import com.tlc.crm.common.sql.resource.OMEXEXPORT;
import com.tlc.crm.security.context.CrmContextType;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.common.internal.service.impl.BulkRequestService;
import com.tlc.crm.common.internal.service.impl.EventRequestServiceImpl;
import com.tlc.crm.common.internal.service.impl.SingleRequestService;
import com.tlc.crm.common.iam.internal.permission.AdminUserProvider;
import com.tlc.crm.icon.internal.cache.CacheLoader;
import com.tlc.crm.security.permission.SecureDefaultValueProviderRegistry;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;
import com.tlc.crm.security.sql.resource.OIAMPACTIONLIMITDETAILS;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import com.tlc.web.undertow.cors.PreflightHandlerRegistry;

import java.util.Collection;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CommonServiceInitializer implements ServiceInitializer {

    private final DataExportConsumerController dataExportConsumerController;
    private final OrgInitiatorController orgInitiatorController;
    private final UserMigrationService userMigrationService;
    private final SingleRequestService singleRequestService;
    private final BulkRequestService bulkRequestService;
    private final EventRequestService eventRequestService;
    private final ContextCodecRegistry contextCodecRegistry;
    private final ContextHolderRegistry contextHolderRegistry;
    private final CacheLoader cacheLoader;
    private final RoleRowListener roleRowListener;
    private final UserRoleRowListener userRoleRowListener;
    private final AdminDataStore adminDataStore;
    private final ActionLimitCleaner actionLimitCleaner;
    private final RemainingActionCountRowListener remainingActionCountRowListener;
    private final DataExportRowListener dataExportRowListener;
    private final SecureDefaultValueProviderRegistry secureDefaultValueProviderRegistry;
    private final SecureDefaultValueProvider<Long> adminUserProvider;
    private final SecureDefaultValueProvider<Collection<Long>> userTerritoryProvider;
    private final  SecureDefaultValueProvider<Collection<Long>> followerAccessRestrictedRolesProvider;

    private CommonServiceInitializer() {
        dataExportConsumerController = new DataExportConsumerController();
        orgInitiatorController = new OrgInitiatorController();
        userMigrationService = new UserMigrationService();
        singleRequestService = new SingleRequestService();
        bulkRequestService = new BulkRequestService();
        eventRequestService = new EventRequestServiceImpl();
        contextCodecRegistry = ContextCodecRegistry.getInstance();
        contextHolderRegistry = ContextHolderRegistry.getInstance();
        cacheLoader = CacheLoader.getInstance();
        roleRowListener = RoleRowListener.getInstance();
        userRoleRowListener = UserRoleRowListener.getInstance();
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
        actionLimitCleaner = ActionLimitCleaner.getInstance();
        remainingActionCountRowListener = RemainingActionCountRowListener.getInstance();
        dataExportRowListener = DataExportRowListener.getInstance();
        secureDefaultValueProviderRegistry = SecureDefaultValueProviderRegistry.get();
        adminUserProvider = AdminUserProvider.getInstance();
        userTerritoryProvider = UserTerritoryProvider.getInstance();
        followerAccessRestrictedRolesProvider = FollowerAccessRestrictedRolesProvider.getInstance();
    }

    private static final class Instance {
        private static final CommonServiceInitializer COMMON_SERVICE_INITIALIZER = new CommonServiceInitializer();
    }

    public static CommonServiceInitializer getInstance() {
        return Instance.COMMON_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        singleRequestService.start(configurations);
        bulkRequestService.start(configurations);
        eventRequestService.start(configurations);
        contextCodecRegistry.register(CrmContextType.SECURITY, new SecurityContextCodec());
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        PreflightHandlerRegistry.getInstance().register(CommonFields.CRM, CrmPreflightHandler.getInstance());
        dataExportConsumerController.start(configurations);
        orgInitiatorController.start(configurations);
        userMigrationService.start(configurations);
        cacheLoader.load();
        secureDefaultValueProviderRegistry.register(adminUserProvider);
        secureDefaultValueProviderRegistry.register(userTerritoryProvider);
        secureDefaultValueProviderRegistry.register(followerAccessRestrictedRolesProvider);
        adminDataStore.registerRowListener(Table.get(OMCRMPREFERENCE.TABLE), UserPreferenceListener.getInstance());
        adminDataStore.registerUpdatedEntryListener(Table.get(OMCRMROLE.TABLE), roleRowListener);
        adminDataStore.registerUpdatedEntryListener(Table.get(MCRMUSERROLE.TABLE), userRoleRowListener);
        adminDataStore.registerDeletedEntryListener(Table.get(OIAMPACTIONLIMITDETAILS.TABLE), remainingActionCountRowListener);
        actionLimitCleaner.start(configurations);

        adminDataStore.registerRowListener(Table.get(OMEXEXPORT.TABLE), dataExportRowListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        dataExportConsumerController.stop();
        singleRequestService.stop();
        bulkRequestService.stop();
        eventRequestService.stop();
        orgInitiatorController.stop();
        userMigrationService.stop();
        contextCodecRegistry.unregister(CrmContextType.SECURITY);
        contextHolderRegistry.unregister(CrmContextType.SECURITY);
        cacheLoader.clear();
        contextCodecRegistry.unregister(CrmContextType.SECURITY);
        actionLimitCleaner.stop();
    }
}
