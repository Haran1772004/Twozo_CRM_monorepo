package com.tlc.crm.core.notification.email.api.manager.impl;

import com.tlc.crm.core.notification.email.api.manager.EmailDomainFilterManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.core.sql.resource.OMEMDOMAINFILTERPREFERENCE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implementation of {@link EmailDomainFilterManager} that handles
 * CRUD operations for email domain filter preferences at the organization level.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainFilterManagerImpl implements EmailDomainFilterManager {

    private static class Instance {
        private static final EmailDomainFilterManagerImpl INSTANCE = new EmailDomainFilterManagerImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link EmailDomainFilterManagerImpl}.
     * </p>
     *
     * @return Singleton instance of {@link EmailDomainFilterManagerImpl}.
     */
    public static EmailDomainFilterManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Private constructor to enforce singleton pattern.
     * </p>
     */
    private EmailDomainFilterManagerImpl() {
    }

    /**
     * <p>
     * Returns the {@link OrgDataStore} instance for a specific organization.
     * </p>
     *
     * @param orgId Represents the organization ID.
     * @return The {@link OrgDataStore} for the given organization.
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the primary table for storing email domain filter preferences.
     * </p>
     *
     * @return The {@link Table} representing the email domain filter preference table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMDOMAINFILTERPREFERENCE.TABLE);
    }

    /**
     * <p>
     * Saves or updates an {@link EmailDomainFilterPreference} for a given user.
     * If a preference already exists, it is updated; otherwise, a new record is created.
     * </p>
     *
     * @param orgId                       Represents the organization ID.
     * @param emailDomainFilterPreference The {@link EmailDomainFilterPreference} to save.
     */
    @Override
    public void save(final Long orgId, final EmailDomainFilterPreference emailDomainFilterPreference) {
        final Table table = getTable();
        final Long userId = emailDomainFilterPreference.getUserId();
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer existingContainer = getExistingContainer(orgId, userId);
        final Row row = Optional.ofNullable(existingContainer.getRow(table, getCriteria(userId)))
                .orElseGet(() -> new Row(table));
        final long currentTime = Instant.now().toEpochMilli();

        if (row.isNewRow()) {
            row.set(OMEMDOMAINFILTERPREFERENCE.USER_ID, userId);
            row.set(OMEMDOMAINFILTERPREFERENCE.CREATED_TIME, currentTime);
        }
        row.set(OMEMDOMAINFILTERPREFERENCE.STATUS, emailDomainFilterPreference.getStatus());
        row.set(OMEMDOMAINFILTERPREFERENCE.UPDATED_TIME, currentTime);

        emailDomainFilterPreference.setId(row.getPKValue());
        targetContainer.updateRow(row);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves an {@link EmailDomainFilterPreference} for a given user within an organization.
     * </p>
     *
     * @param orgId  Represents the organization ID.
     * @param userId Represents the user ID for which the preference is retrieved.
     * @return An {@link Optional} containing the user's {@link EmailDomainFilterPreference}, or empty if not found.
     */
    @Override
    public Optional<EmailDomainFilterPreference> get(final Long orgId, final Long userId) {
        final Table table = getTable();
        final DataContainer container = getExistingContainer(orgId, userId);
        final Row row = container.getRow(table,
                new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINFILTERPREFERENCE.USER_ID), userId)));

        if (Objects.isNull(row)) {
            return Optional.empty();
        }
        final EmailDomainFilterPreference preference = new EmailDomainFilterPreference();

        preference.setId(row.getPKValue());
        preference.setOrgId(row.get(OMEMDOMAINFILTERPREFERENCE.ORG_ID));
        preference.setUserId(row.get(OMEMDOMAINFILTERPREFERENCE.USER_ID));
        preference.setStatusFilter(row.get(OMEMDOMAINFILTERPREFERENCE.STATUS));

        return Optional.of(preference);
    }

    /**
     * <p>
     * Deletes the {@link EmailDomainFilterPreference} for a given user within an organization.
     * </p>
     *
     * @param orgId  Represents the organization ID.
     * @param userId Represents the user ID whose preference should be deleted.
     */
    @Override
    public void delete(final Long orgId, final Long userId) {
        final Table table = getTable();

        orgDataStore(orgId).delete(table, getCriteria(userId));
    }

    /**
     * <p>
     * Retrieves the existing {@link DataContainer} containing preferences for a user.
     * </p>
     *
     * @param orgId  Represents the organization ID.
     * @param userId Represents the user ID for which to fetch data.
     * @return The {@link DataContainer} containing the existing preferences.
     */
    private DataContainer getExistingContainer(final Long orgId, final Long userId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(table.getColumn(OMEMDOMAINFILTERPREFERENCE.USER_ID), userId)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds a {@link WhereClause} to select a user's preference by user ID.
     * </p>
     *
     * @param userId Represents the user ID.
     * @return The {@link WhereClause} for the user's preference.
     */
    private WhereClause getCriteria(final Long userId) {
        return new WhereClause(
                Criteria.eq(getTable().getColumn(OMEMDOMAINFILTERPREFERENCE.USER_ID), userId));
    }
}