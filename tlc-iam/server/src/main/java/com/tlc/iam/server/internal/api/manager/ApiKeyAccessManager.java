package com.tlc.iam.server.internal.api.manager;

import com.tlc.iam.common.models.ApiKeyStore;
import com.tlc.iam.server.sql.resource.MIAMOAPIKEYSTORE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import java.time.Instant;

/**
 * Manages the process of API keys used for authenticating
 * and authorizing external requests.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ApiKeyAccessManager extends AbstractIamManager<ApiKeyStore> {

    private ApiKeyAccessManager() {
    }

    private static class Instance {
        private static final ApiKeyAccessManager INSTANCE = new ApiKeyAccessManager();
    }

    public static ApiKeyAccessManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The table of api key store.
     */
    @Override
    protected Table getTable() {
        return Table.get(MIAMOAPIKEYSTORE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the is.
     * @return
     */
    @Override
    public ApiKeyStore get(final Long id) {
        final Row row = readableDataStore().get(getTable(), id);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final ApiKeyStore apiKeyStore = new ApiKeyStore(row.getOrgId(), row.get(MIAMOAPIKEYSTORE.USER_ID));

            apiKeyStore.setApiKey(row.get(MIAMOAPIKEYSTORE.API_KEY));
            apiKeyStore.setExpirationTime(row.get(MIAMOAPIKEYSTORE.EXPIRATION_TIME));
            apiKeyStore.setCreatedTime(row.get(MIAMOAPIKEYSTORE.CREATED_TIME));
            apiKeyStore.setUpdatedTime(row.get(MIAMOAPIKEYSTORE.UPDATED_TIME));

            return apiKeyStore;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param apiKeyStore Refers the {@code ApiKeyStore}.
     */
    @Override
    public void save(final ApiKeyStore apiKeyStore) {
        final Table table = getTable();
        final Long orgId = apiKeyStore.orgId();
        final Long userId = apiKeyStore.getUserId();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer dataContainer = DataContainer.create();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(MIAMOAPIKEYSTORE.USER_ID), userId));
        final Row row = orgDataStore.get(table, whereClause).getOrInitRow(table, whereClause);
        final long currentTime = Instant.now().toEpochMilli();

        if (row.isNewRow()) {
            apiKeyStore.setId(row.getPKValue());
            row.setOrgId(orgId);
            row.set(MIAMOAPIKEYSTORE.UPDATED_TIME, currentTime);
        }

        row.set(MIAMOAPIKEYSTORE.USER_ID, userId);
        row.set(MIAMOAPIKEYSTORE.API_KEY, apiKeyStore.getApiKey());
        row.set(MIAMOAPIKEYSTORE.CREATED_TIME, currentTime);
        row.set(MIAMOAPIKEYSTORE.EXPIRATION_TIME, apiKeyStore.getExpirationTime());

        dataContainer.updateRow(row);
        orgDataStore.commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param models Refers the models.
     * @return
     */
    @Override
    public Map<String, Object> update(final Collection<ApiKeyStore> models) {
        return null;
    }

    /**
     * Gets the {@code ApiKeyStore} by its id.
     *
     * @param orgId Refers the organization id.
     * @param id    Refers the id.
     * @return The {@code ApiKeyStore}.
     */
    public ApiKeyStore get(final Long orgId, final Long id) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(MIAMOAPIKEYSTORE.USER_ID), id))).getRow(table);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final ApiKeyStore apiKeyStore = new ApiKeyStore(row.getOrgId(), row.get(MIAMOAPIKEYSTORE.USER_ID));

            apiKeyStore.setApiKey(row.get(MIAMOAPIKEYSTORE.API_KEY));
            apiKeyStore.setExpirationTime(row.get(MIAMOAPIKEYSTORE.EXPIRATION_TIME));
            apiKeyStore.setCreatedTime(row.get(MIAMOAPIKEYSTORE.CREATED_TIME));
            apiKeyStore.setUpdatedTime(row.get(MIAMOAPIKEYSTORE.UPDATED_TIME));

            return apiKeyStore;
        }
    }

    /**
     * Gets the {@code ApiKeyStore} by its key.
     *
     * @param key Refers the api key.
     * @return The {@code ApiKeyStore}.
     */
    public ApiKeyStore getByKey(final String key) {
        final Table table = getTable();
        final Row row = readableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(MIAMOAPIKEYSTORE.API_KEY), key))).getRow(table);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final ApiKeyStore apiKeyStore = new ApiKeyStore(row.getOrgId(), row.get(MIAMOAPIKEYSTORE.USER_ID));

            apiKeyStore.setApiKey(row.get(MIAMOAPIKEYSTORE.API_KEY));
            apiKeyStore.setExpirationTime(row.get(MIAMOAPIKEYSTORE.EXPIRATION_TIME));
            apiKeyStore.setCreatedTime(row.get(MIAMOAPIKEYSTORE.CREATED_TIME));
            apiKeyStore.setUpdatedTime(row.get(MIAMOAPIKEYSTORE.UPDATED_TIME));

            return apiKeyStore;
        }
    }

    /**
     * Deletes the {@code ApiKeyStore} by its org and user id.
     *
     * @param orgId  Refers the organization ID.
     * @param userId Refers the user ID.
     */
    public void delete(final long orgId, final long userId) {
        final Table table = getTable();

        orgDataStore(orgId).delete(table, new WhereClause(Criteria.eq(table.getColumn(MIAMOAPIKEYSTORE.USER_ID), userId)));
    }

    /**
     * Checks if the {@code ApiKeyStore} is existed for the given org and user id.
     *
     * @param orgId  Refers the organization ID.
     * @param userId Refers the user ID.
     */
    public boolean exists(final long orgId, final long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMOAPIKEYSTORE.USER_ID), userId)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }
}
