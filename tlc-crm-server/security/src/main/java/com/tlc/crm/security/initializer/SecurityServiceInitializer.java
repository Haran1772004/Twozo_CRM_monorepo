package com.tlc.crm.security.initializer;

import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.security.context.CrmContextType;
import com.tlc.crm.security.internal.context.SecurityContextHolder;
import com.tlc.crm.security.internal.permission.manager.DataPermissionManager;
import com.tlc.crm.security.internal.permission.model.CurrentUserOrgProvider;
import com.tlc.crm.security.internal.permission.model.CurrentUserProvider;
import com.tlc.crm.security.internal.permission.model.CurrentUserRoleProvider;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;
import com.tlc.crm.security.internal.permission.service.cache.PermissionMetaDataCache;
import com.tlc.crm.security.internal.permission.service.cache.RolePermissionCache;
import com.tlc.crm.security.internal.sql.listener.RoleRowListener;
import com.tlc.crm.security.permission.SecureDefaultValueProviderRegistry;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

/**
 * <p>
 * An initializer that initializes the security services which needs to be started
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class SecurityServiceInitializer implements ServiceInitializer {

    private final ContextHolderRegistry contextHolderRegistry;
    private final WritableDataStore writableDataStore;
    private final RoleRowListener roleRowListener;
    private final SecureDefaultValueProviderRegistry defaultValueProviderRegistry;
    private final SecureDefaultValueProvider<Long> currentUserProvider;
    private final SecureDefaultValueProvider<Long> currentUserOrgProvider;
    private final SecureDefaultValueProvider<Long> currentUserRoleProvider;

    private SecurityServiceInitializer() {
        contextHolderRegistry = ContextHolderRegistry.getInstance();
        writableDataStore = DataStoreProvider.getWritableDataStore();
        roleRowListener = RoleRowListener.getInstance();
        defaultValueProviderRegistry = SecureDefaultValueProviderRegistry.get();
        currentUserProvider = CurrentUserProvider.getInstance();
        currentUserOrgProvider = CurrentUserOrgProvider.getInstance();
        currentUserRoleProvider = CurrentUserRoleProvider.getInstance();
    }

    private static final class Instance {
        private static final SecurityServiceInitializer SECURITY_SERVICE_INITIALIZER = new SecurityServiceInitializer();
    }

    public static SecurityServiceInitializer getInstance() {
        return Instance.SECURITY_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        contextHolderRegistry.register(CrmContextType.SECURITY, SecurityContextHolder.getInstance());
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        DataPermissionManager.get().loadDataPermissions();
        final Table roleTable = Table.get(OMCRMROLE.TABLE);

        writableDataStore.registerDeletedEntryListener(roleTable, roleRowListener);
        writableDataStore.registerUpdatedEntryListener(roleTable, roleRowListener);
        PermissionMetaDataCache.getInstance().load();
        RolePermissionCache.getInstance().load();
        defaultValueProviderRegistry.register(currentUserProvider);
        defaultValueProviderRegistry.register(currentUserOrgProvider);
        defaultValueProviderRegistry.register(currentUserRoleProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        contextHolderRegistry.unregister(CrmContextType.SECURITY);
        DataPermissionHandlerRegistry.getInstance().clear();
        PermissionMetaDataCache.getInstance().clear();
    }
}
