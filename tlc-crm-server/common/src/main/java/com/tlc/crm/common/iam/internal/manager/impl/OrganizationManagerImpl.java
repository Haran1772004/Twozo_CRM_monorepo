package com.tlc.crm.common.iam.internal.manager.impl;

import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.PreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.resource.FGORGANISATION;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link OrganizationManagerImpl} that provides the various methods for the {@link OrganizationManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 * @see OrganizationManager
 */
public class OrganizationManagerImpl implements OrganizationManager {

    private final PreferenceManager<OrgPreferenceHolder> preferenceManager;

    private OrganizationManagerImpl() {
        preferenceManager = OrgPreferenceManager.getInstance();
    }

    private static class Instance {
        private static final OrganizationManagerImpl INSTANCE = new OrganizationManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link OrganizationManagerImpl} Instance
     * </p>
     *
     * @return {@link OrganizationManagerImpl}
     */
    public static OrganizationManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the admin data store
     * </p>
     *
     * @return The {@link WritableDataStore}
     */
    private WritableDataStore getWritableDataStore() {
        return DataStoreProvider.getWritableDataStore();
    }

    /**
     * <p>
     * Gets the FG organisation table
     * </p>
     *
     * @return The FG organisation table
     */
    private Table getTable() {
        return Table.get(FGORGANISATION.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the org id
     * @param status Represents the {@link CrmOrg.InitializationStatus}
     */
    @Override
    public void updateInitializationStatus(final Long orgId, final CrmOrg.InitializationStatus status) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), orgId)));
        updateQuery.addUpdateColumn(table.getColumn(FGORGANISATION.INITIALIZATION_STATUS), status.getId());
        getWritableDataStore().update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId Represents the iam org id
     * @return The {@link CrmOrg} details
     */
    @Override
    public CrmOrg getOrgByIamId(final Long iamOrgId) {
        final Table table = getTable();
        final Column iamOrgIdColumn = table.getColumn(FGORGANISATION.IAM_ORG_ID);
        final Row row = getWritableDataStore().get(table, new WhereClause(Criteria.eq(iamOrgIdColumn, iamOrgId))).getRow(table);

        return Optional.ofNullable(row).map(this::convertRowToModel).orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the organization id
     * @return The {@link CrmOrg} details
     */
    @Override
    public CrmOrg get(final Long id) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(FGORGANISATION.IAM_ORG_ID),
                table.getColumn(FGORGANISATION.INITIALIZATION_STATUS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(id).get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Optional.ofNullable(row).map(this::convertRowToModel).orElse(null);
    }

    /**
     * <p>
     * Gets all {@link CrmOrg} records
     * </p>
     *
     * @return A collection of {@link CrmOrg} details
     */
    @Override
    public Collection<CrmOrg> getAll() {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(FGORGANISATION.IAM_ORG_ID),
                table.getColumn(FGORGANISATION.INITIALIZATION_STATUS)));
        final DataContainer dataContainer = DataStoreProvider.getAdminDataStore().get(selectQuery);

        return dataContainer.getRows(table).map(this::convertRowToModel).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Saves the given {@link CrmOrg} details
     * </p>
     *
     * @param crmOrg Represents the {@link CrmOrg}
     */
    public void create(final CrmOrg crmOrg) {
        final DataContainer dataContainer = DataContainer.create();
        final Row row = new Row(getTable());
        final OrgPreferenceHolder preferenceHolder = crmOrg.getPreferenceHolder();

        preferenceHolder.setOrgId(row.getPKValue());
        row.set(FGORGANISATION.IAM_ORG_ID, crmOrg.getIamOrgId());
        row.set(FGORGANISATION.INITIALIZATION_STATUS, crmOrg.getInitializationStatus().getId());
        crmOrg.setId(row.getPKValue());
        dataContainer.addNewRow(row);

        getWritableDataStore().commitChanges(dataContainer);
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link CrmOrg}
     * </p>
     *
     * @param row A row represents the crm org
     * @return The {@link CrmOrg}
     */
    private CrmOrg convertRowToModel(final Row row) {
        final CrmOrg crmOrg = new CrmOrg();
        final Integer initializationStatusId = row.get(FGORGANISATION.INITIALIZATION_STATUS);

        if (Objects.nonNull(initializationStatusId)) {
            crmOrg.setInitializationStatus(CrmOrg.InitializationStatus.get(initializationStatusId));
        }
        crmOrg.setId(row.getPKValue());
        crmOrg.setIamOrgId(row.get(FGORGANISATION.IAM_ORG_ID));

        return crmOrg;
    }
}
