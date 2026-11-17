package com.tlc.crm.core.notification.email.api.manager.impl;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.crm.core.notification.email.api.manager.CrmEmailDomainManager;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDnsRecord;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.crm.email.notification.sql.resource.OMEMDOMAIN;
import com.tlc.crm.email.notification.sql.resource.OMEMDOMAINDNSRECORDS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Manager class responsible for CRUD operations and mapping database rows
 * to {@link CrmEmailDomain} objects for entity domain configurations and their DNS records.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class CrmEmailDomainManagerImpl implements CrmEmailDomainManager {

    private static class Instance {
        private static final CrmEmailDomainManagerImpl INSTANCE = new CrmEmailDomainManagerImpl();
    }

    private CrmEmailDomainManagerImpl() {
    }

    /**
     * <p>
     * Gets the {@link CrmEmailDomainManagerImpl} Instance
     * </p>
     *
     * @return {@link CrmEmailDomainManager}
     */
    public static CrmEmailDomainManager getInstance() {
        return CrmEmailDomainManagerImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents the orgId.
     * @return the {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the primary table.
     * </p>
     *
     * @return The {@link Table} containing Domain.
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMDOMAIN.TABLE);
    }

    /**
     * <p>
     * Gets the DNS record table.
     * </p>
     *
     * @return The {@link Table} containing DNS records.
     */
    private Table getDnsRecordTable() {
        return Table.get(OMEMDOMAINDNSRECORDS.TABLE);
    }

    /**
     * <p>
     * Persists a new {@link CrmEmailDomain} into the database.
     * </p>
     *
     * @param orgId          Represent the orgId.
     * @param crmEmailDomain The {@link CrmEmailDomain} to be created.
     */
    @Override
    public void create(final Long orgId, final CrmEmailDomain crmEmailDomain) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainDataIntoContainer(crmEmailDomain, null, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates an existing {@link CrmEmailDomain} in the database.
     * </p>
     *
     * @param orgId          Represent the orgId.
     * @param crmEmailDomain The {@link CrmEmailDomain} to be updated.
     */
    @Override
    public void update(final Long orgId, final CrmEmailDomain crmEmailDomain) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainDataIntoContainer(crmEmailDomain, getExistingContainer(orgId, List.of(crmEmailDomain.id())), targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Deletes an {@link CrmEmailDomain} from the database by its CRM ID for the specified organization.
     * </p>
     *
     * @param orgId Represents the organization ID to which the domain belongs.
     * @param id    The CRM ID of the {@link CrmEmailDomain} to delete.
     */
    @Override
    public void delete(final Long orgId, final Long id) {
        orgDataStore(orgId).delete(getTable(), id);
    }

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by its unique identifier.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param id    The CRM ID of the entity domain to retrieve.
     * @return An {@link Optional} containing the entity domain if found, or empty otherwise.
     */
    @Override
    public Optional<CrmEmailDomain> get(final Long orgId, final Long id) {
        if (Objects.isNull(id)) {
            return Optional.empty();
        }
        final Table table = getTable();
        final WhereClause criteria = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.IS_ACTIVE), true))
                .and(Criteria.notIn(table.getColumn(OMEMDOMAIN.STATUS), List.of(DomainStatus.NEW.getId())))
                .and(Criteria.in(table.getPKColumn(), List.of(id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(getSelectQuery(criteria));

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(table, id), dataContainer));
    }

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by its domain name.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param name  Represents the domain name.
     * @return An {@link Optional} containing the entity domain if found, or empty otherwise.
     */
    @Override
    public Optional<CrmEmailDomain> get(final Long orgId, final String name) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.NAME), name));
        final DataContainer dataContainer = getExistingContainer(orgId, whereClause);

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getTable(), whereClause), dataContainer));
    }

    /**
     * <p>
     * Retrieves a paged collection of {@link CrmEmailDomain} objects.
     * </p>
     *
     * @param orgId              Represents the orgId.
     * @param start              Represents the start offset.
     * @param limit              Represents the max number of results.
     * @param additionalCriteria Represents the additional criteria.
     * @return A collection of {@link CrmEmailDomain} objects.
     */
    @Override
    public Collection<CrmEmailDomain> get(final Long orgId, final int start, final int limit, final WhereClause additionalCriteria) {
        final DataContainer dataContainer = getExistingContainer(orgId, new LimitClause(start, limit), additionalCriteria);

        return dataContainer.getRows(getTable())
                .map(row -> convertRowToModel(row, dataContainer))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves multiple {@link CrmEmailDomain} objects for a collection of IDs.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param ids   A collection of entity domain CRM IDs.
     * @return A collection of {@link CrmEmailDomain} objects found for the given IDs.
     */
    @Override
    public Collection<CrmEmailDomain> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = getExistingContainer(orgId, ids);

        return dataContainer.getRows(getTable())
                .map(row -> convertRowToModel(row, dataContainer))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Checks whether a domain exists by its CRM ID.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param id    Represent the domain id.
     * @return Whether the domain exists.
     */
    @Override
    public boolean exists(final Long orgId, final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return orgDataStore(orgId).get(countQuery) == 1;
    }

    /**
     * <p>
     * Checks whether a domain exists by its name.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param name  Represent the domain name.
     * @return Whether the domain exists.
     */
    @Override
    public boolean exists(final Long orgId, final String name) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.NAME), name))
                .and(Criteria.notEq(table.getColumn(OMEMDOMAIN.STATUS), DomainStatus.NEW.getId()))
                .and(Criteria.eq(table.getColumn(OMEMDOMAIN.IS_ACTIVE), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves the total number of active, non-new entity domains for a given organization.
     * </p>
     *
     * @param orgId              Represents the organization ID.
     * @param additionalCriteria Represents the additional criteria.
     * @return the total count of domains.
     */
    @Override
    public long getDomainCount(final Long orgId, final WhereClause additionalCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.IS_ACTIVE), true))
                .and(Criteria.notIn(table.getColumn(OMEMDOMAIN.STATUS), List.of(DomainStatus.NEW.getId())));

        countQuery.setWhereClause(WhereClauseUtil.connect(whereClause, additionalCriteria, Connector.AND));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the CRM primary key of a domain given its domain name.
     * </p>
     *
     * @param orgId      Represents the organization ID.
     * @param domainName Represents the domainName.
     * @return An {@link Optional} containing the CRM primary key if found, or empty otherwise.
     */
    @Override
    public Optional<Long> getId(final Long orgId, final String domainName) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.NAME), domainName));
        final DataContainer dataContainer = orgDataStore(orgId).get(table, whereClause);

        return Optional.ofNullable(dataContainer.getRow(table, whereClause)).map(Row::getPKValue);
    }

    /**
     * <p>
     * Builds the domain for the given id from the container.
     * </p>
     *
     * @param id            Represents the id.
     * @param dataContainer Represents the data container.
     * @return The entity domain for the id and the container.
     */
    @Override
    public Optional<CrmEmailDomain> getDomain(final Long id, final DataContainer dataContainer) {
        return Optional.ofNullable(dataContainer.getRow(getTable(), id)).map(row -> convertRowToModel(row, dataContainer));
    }

    /**
     * <p>
     * Updates the status for the domain.
     * </p>
     *
     * @param orgId        organization ID
     * @param id           domain ID
     * @param domainStatus Represents the domain status.
     */
    @Override
    public void updateStatus(final Long orgId, final Long id, final DomainStatus domainStatus) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMEMDOMAIN.STATUS), domainStatus.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Loads domain data into the target container for the given org and event type.
     * </p>
     *
     * @param orgId           Represents the orgId.
     * @param isCreateEvent   Represents whether this is a create event.
     * @param targetContainer Represents the target container.
     * @param crmEmailDomain  The {@link CrmEmailDomain} to load.
     */
    @Override
    public void loadDomainDataIntoContainer(final Long orgId, final boolean isCreateEvent,
                                            final DataContainer targetContainer, final CrmEmailDomain crmEmailDomain) {
        loadDomainDataIntoContainer(
                crmEmailDomain,
                isCreateEvent ? null : getExistingContainer(orgId, List.of(crmEmailDomain.id())),
                targetContainer
        );
    }

    /**
     * <p>
     * Updates the active state for the given domain id.
     * </p>
     *
     * @param orgId    organization ID
     * @param id       domain ID
     * @param isActive Represents whether the state is active or not.
     */
    @Override
    public void updateState(final Long orgId, final Long id, final boolean isActive) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMEMDOMAIN.IS_ACTIVE), isActive);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Converts a database {@link Row} into a {@link CrmEmailDomain} model.
     * </p>
     *
     * @param row           The database row to convert.
     * @param dataContainer The {@link DataContainer} containing additional related rows.
     * @return A {@link CrmEmailDomain} object, or {@code null} if the row is null.
     */
    private CrmEmailDomain convertRowToModel(final Row row, final DataContainer dataContainer) {
        if (Objects.isNull(row)) {
            return null;
        }
        final CrmEmailDomain crmEmailDomain = new CrmEmailDomain();
        final Long crmId = row.getPKValue();

        crmEmailDomain.setId(crmId);
        crmEmailDomain.setEmailDomainId(row.get(OMEMDOMAIN.DOMAIN_ID));
        crmEmailDomain.setOrgId(row.get(OMEMDOMAIN.ORG_ID));
        crmEmailDomain.setUserId(row.get(OMEMDOMAIN.USER_ID));
        crmEmailDomain.setName(row.get(OMEMDOMAIN.NAME));
        crmEmailDomain.setActive(row.get(OMEMDOMAIN.IS_ACTIVE));
        crmEmailDomain.setDomainStatus(DomainStatus.get(row.get(OMEMDOMAIN.STATUS)));
        crmEmailDomain.setCreatedAt(row.get(OMEMDOMAIN.CREATED_TIME));
        crmEmailDomain.setDnsRecords(getDnsRecords(crmId, dataContainer));

        return crmEmailDomain;
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} for the given pagination clause.
     * </p>
     *
     * @param orgId              Represents the orgId.
     * @param limitClause        Represents the limit clause.
     * @param additionalCriteria Represents the where clause.
     * @return A {@link DataContainer} containing the entity domains and DNS records.
     */
    private DataContainer getExistingContainer(final Long orgId, final LimitClause limitClause, final WhereClause additionalCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final WhereClause criteria = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAIN.IS_ACTIVE), true))
                .and(Criteria.notIn(table.getColumn(OMEMDOMAIN.STATUS), List.of(DomainStatus.NEW.getId())));

        selectQuery.setWhereClause(WhereClauseUtil.connect(criteria, additionalCriteria, Connector.AND));
        selectQuery.setLimitClause(limitClause);
        selectQuery.addOrderByClause(new OrderByClause(getTable().getPKColumn()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> domainIds = dataContainer.getRows(table).map(Row::getPKValue).collect(Collectors.toSet());

        dataContainer.append(getDnsContainer(orgId, domainIds));

        return dataContainer;
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} for the given pagination clause.
     * </p>
     *
     * @param orgId     Represents the orgId.
     * @param domainIds Represent the domain ids.
     * @return A {@link DataContainer} containing the DNS records.
     */
    private DataContainer getDnsContainer(final Long orgId, final Collection<Long> domainIds) {
        if (Objects.isNull(domainIds) || domainIds.isEmpty()) {
            return DataContainer.create();
        }
        final Table dnsRecordTable = getDnsRecordTable();
        final SelectQuery selectQuery = SelectQuery.get(dnsRecordTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(dnsRecordTable.getColumn(OMEMDOMAINDNSRECORDS.DOMAIN_ID), domainIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} for the given collection of CRM domain IDs.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param ids   Collection of entity domain CRM IDs.
     * @return A {@link DataContainer} containing the entity domains and DNS records.
     */
    private DataContainer getExistingContainer(final Long orgId, final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return DataContainer.create();
        }
        return getExistingContainer(orgId, new WhereClause(Criteria.in(getTable().getPKColumn(), ids)));
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} filtered by the provided where clause.
     * </p>
     *
     * @param orgId       Represents the orgId.
     * @param whereClause The where clause for filtering.
     * @return A {@link DataContainer} containing the filtered entity domains and DNS records.
     */
    private DataContainer getExistingContainer(final Long orgId, final WhereClause whereClause) {
        return orgDataStore(orgId).get(getSelectQuery(whereClause));
    }

    /**
     * <p>
     * Constructs a {@link SelectQuery} to retrieve entity domains joined with their DNS records.
     * </p>
     *
     * @param whereClause The {@link WhereClause} to filter the query, can be {@code null}.
     * @return A configured {@link SelectQuery}.
     */
    private SelectQuery getSelectQuery(final WhereClause whereClause) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), getDnsRecordTable()
                .getColumn(OMEMDOMAINDNSRECORDS.DOMAIN_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * <p>
     * Loads entity domain data and associated DNS records into the target {@link DataContainer}.
     * </p>
     *
     * @param crmEmailDomain  The {@link CrmEmailDomain} to load.
     * @param sourceContainer The source {@link DataContainer}, can be null.
     * @param targetContainer The target {@link DataContainer} to populate.
     */
    private void loadDomainDataIntoContainer(final CrmEmailDomain crmEmailDomain, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table table = getTable();
        final Long domainId = crmEmailDomain.id();
        final WhereClause criteria = new WhereClause(
                Objects.isNull(domainId)
                        ? Criteria.eq(table.getColumn(OMEMDOMAIN.DOMAIN_ID), crmEmailDomain.getEmailDomainId())
                        : Criteria.eq(table.getPKColumn(), domainId)
        );
        final Row existingSourceRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table, criteria);
        final Row existingRow = Objects.isNull(existingSourceRow) ? targetContainer.getRow(table, criteria) : existingSourceRow;
        final Row row = getRow(crmEmailDomain, existingRow, table);
        final Long pkValue = row.getPKValue();

        crmEmailDomain.setId(pkValue);
        targetContainer.updateRow(row);
        final Collection<CrmEmailDnsRecord> dnsRecords = crmEmailDomain.getCrmDnsRecords();

        if (Objects.nonNull(dnsRecords) && !dnsRecords.isEmpty()) {
            loadDnsRecordsIntoContainer(pkValue, crmEmailDomain.getOrgId(), crmEmailDomain.getUserId(), dnsRecords, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Builds and returns the domain {@link Row}, setting fields from the given {@link CrmEmailDomain}.
     * </p>
     *
     * @param crmEmailDomain The {@link CrmEmailDomain} to load.
     * @param existingRow    Represents the existing row.
     * @param table          Represents the domain table.
     * @return The constructed or updated {@link Row}.
     */
    private Row getRow(final CrmEmailDomain crmEmailDomain, final Row existingRow, final Table table) {
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;
        final boolean isNewRow = row.isNewRow();

        if (isNewRow) {
            row.set(OMEMDOMAIN.NAME, crmEmailDomain.getName());
        }
        final Long createdTime = crmEmailDomain.getCreatedAt();

        row.set(OMEMDOMAIN.USER_ID, crmEmailDomain.getUserId());
        row.set(OMEMDOMAIN.CREATED_TIME, Objects.isNull(createdTime) ? Instant.now().toEpochMilli() : createdTime);
        row.set(OMEMDOMAIN.DOMAIN_ID, crmEmailDomain.getEmailDomainId());
        row.set(OMEMDOMAIN.IS_ACTIVE, crmEmailDomain.isActive());
        final DomainStatus domainStatus = crmEmailDomain.getDomainStatus();

        if (Objects.nonNull(domainStatus)) {
            row.set(OMEMDOMAIN.STATUS, domainStatus.getId());
        }

        return row;
    }

    /**
     * <p>
     * Loads the given collection of {@link CrmEmailDnsRecord} objects into the target {@link DataContainer}.
     * </p>
     *
     * @param crmDomainId     The CRM ID of the domain these DNS records belong to.
     * @param orgId           The organization ID.
     * @param userId          The user ID.
     * @param dnsRecords      A collection of {@link CrmEmailDnsRecord} to load.
     * @param sourceContainer The source {@link DataContainer}, can be {@code null}.
     * @param targetContainer The target {@link DataContainer} to populate.
     */
    private void loadDnsRecordsIntoContainer(final Long crmDomainId, final Long orgId, final Long userId,
                                             final Collection<CrmEmailDnsRecord> dnsRecords, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table dnsRecordTable = getDnsRecordTable();
        final Column domainColumn = dnsRecordTable.getColumn(OMEMDOMAINDNSRECORDS.DOMAIN_ID);
        final Collection<Long> incomingCrmDnsIds = dnsRecords.stream()
                .map(CrmEmailDnsRecord::getEmailDnsRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (Objects.nonNull(sourceContainer)) {
            sourceContainer.getRows(dnsRecordTable, new WhereClause(Criteria.eq(domainColumn, crmDomainId)))
                    .forEach(row -> {

                        if (!incomingCrmDnsIds.contains((Long) row.get(OMEMDOMAINDNSRECORDS.DNS_RECORD_ID))) {
                            targetContainer.deleteRow(row);
                        }
                    });
        }

        for (final CrmEmailDnsRecord dnsRecord : dnsRecords) {
            final Long dnsRecordId = dnsRecord.id();
            final WhereClause dnsCriteria = new WhereClause(
                    Objects.isNull(dnsRecordId)
                            ? Criteria.eq(dnsRecordTable.getColumn(OMEMDOMAINDNSRECORDS.DNS_RECORD_ID), dnsRecord.getEmailDnsRecordId())
                            : Criteria.eq(dnsRecordTable.getPKColumn(), dnsRecordId)
            );
            final WhereClause criteria = dnsCriteria.and(Criteria.eq(domainColumn, crmDomainId));
            final Row existingSourceRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(dnsRecordTable, criteria);
            final Row existingRow = Objects.isNull(existingSourceRow) ? targetContainer.getRow(dnsRecordTable, criteria) : existingSourceRow;
            final Row row = Objects.isNull(existingRow) ? new Row(dnsRecordTable) : existingRow;

            if (row.isNewRow()) {
                row.set(OMEMDOMAINDNSRECORDS.USER_ID, userId);
                row.set(OMEMDOMAINDNSRECORDS.DOMAIN_ID, crmDomainId);
                row.set(OMEMDOMAINDNSRECORDS.DNS_RECORD_ID, dnsRecord.getEmailDnsRecordId());
                targetContainer.addNewRow(row);
            }

            dnsRecord.setDomainId(crmDomainId);
            dnsRecord.setOrgId(orgId);
            dnsRecord.setUserId(userId);
        }
    }

    /**
     * <p>
     * Retrieves entity DNS records for a domain from the provided {@link DataContainer}.
     * </p>
     *
     * @param crmDomainId   The CRM domain ID.
     * @param dataContainer The {@link DataContainer} containing DNS record rows.
     * @return A collection of {@link CrmEmailDnsRecord} objects.
     */
    private Collection<DnsRecord> getDnsRecords(final Long crmDomainId, final DataContainer dataContainer) {
        final Table dnsRecordTable = getDnsRecordTable();
        final Collection<DnsRecord> entityDnsRecords = new HashSet<>();

        dataContainer.getRows(dnsRecordTable, new WhereClause(Criteria.eq(dnsRecordTable.getColumn(OMEMDOMAINDNSRECORDS.DOMAIN_ID), crmDomainId)))
                .forEach(row -> {
                    final CrmEmailDnsRecord dnsRecord = new CrmEmailDnsRecord();

                    dnsRecord.setEmailDnsRecordId(row.get(OMEMDOMAINDNSRECORDS.DNS_RECORD_ID));
                    dnsRecord.setOrgId(row.get(OMEMDOMAINDNSRECORDS.ORG_ID));
                    dnsRecord.setUserId(row.get(OMEMDOMAINDNSRECORDS.USER_ID));
                    dnsRecord.setDomainId(crmDomainId);
                    dnsRecord.setId(row.getPKValue());

                    entityDnsRecords.add(dnsRecord);
                });

        return entityDnsRecords;
    }
}