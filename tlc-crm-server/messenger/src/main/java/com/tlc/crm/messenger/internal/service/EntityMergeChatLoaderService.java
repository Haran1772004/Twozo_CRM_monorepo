package com.tlc.crm.messenger.internal.service;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.features.api.merge.service.EntityMergeCallbackRegistry;
import com.tlc.crm.core.features.api.merge.service.EntityMergeCallbackService;
import com.tlc.crm.core.features.api.merge.service.EntityMergeCallbackType;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;

/**
 * <p>
 * Loads messenger specific entity merge mapping rows into the target {@link DataContainer}.
 * Auto-registered via {@link AutoServiceRegistrar} into {@link EntityMergeCallbackRegistry}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = EntityMergeCallbackRegistry.class)
public final class EntityMergeChatLoaderService implements EntityMergeCallbackService {

    private EntityMergeChatLoaderService() {

    }

    private static final class Instance {
        private static final EntityMergeChatLoaderService INSTANCE = new EntityMergeChatLoaderService();
    }

    /**
     * <p>
     * Gets the instance of {@link EntityMergeChatLoaderService}
     * </p>
     *
     * @return The {@link EntityMergeChatLoaderService}
     */
    public static EntityMergeChatLoaderService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityMergeCallbackType getType() {
        return EntityMergeCallbackType.CHAT_MERGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                      final Source source, final DataContainer targetContainer) {
        switch (source) {
            case DEAL -> loadMappingRowsIntoContainer(orgId, primaryId, secondaryIds, OMSOCHAT.DEAL_ID, targetContainer);
            case CONTACT -> loadMappingRowsIntoContainer(orgId, primaryId, secondaryIds, OMSOCHAT.CONTACT_ID, targetContainer);
            default -> {

            }
        }
    }

    /**
     * <p>
     * Loads chat table mapping rows into container by replacing secondary entity id with primary entity id.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param primaryId       Represents the primary entity id
     * @param secondaryIds    Represents the secondary entity ids
     * @param columnName      Represents the mapping column name in {@link OMSOCHAT}
     * @param targetContainer Holds the updated information
     */
    private void loadMappingRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                                              final String columnName, final DataContainer targetContainer) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Table chatTable = Table.get(OMSOCHAT.TABLE);
        final Column entityColumn = chatTable.getColumn(columnName);

        orgDataStore.get(chatTable, new WhereClause(Criteria.in(entityColumn, secondaryIds))).getRows(chatTable).forEach(row -> {
            row.set(entityColumn, primaryId);
            targetContainer.updateRow(row);
        });
    }
}