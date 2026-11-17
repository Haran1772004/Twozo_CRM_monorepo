package com.tlc.email.server.internal.manager;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.dns.model.RecordType;
import com.tlc.commons.dns.model.Status;
import com.tlc.email.common.model.Category;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.EmailDomainMetaData;
import com.tlc.email.server.sql.resource.MEMDOMAIN;
import com.tlc.email.server.sql.resource.MEMDOMAINDNSRECORDS;
import com.tlc.email.server.sql.resource.MEMDOMAINSENDERCONVERSATION;
import com.tlc.email.server.sql.resource.MEMDOMAINSENDERS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Manager class responsible for CRUD operations and mapping database rows
 * to {@link EmailDomain} objects for domain configurations and their DNS records.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainManager extends AbstractEmailManager<EmailDomain> {

    private EmailDomainManager() {
    }

    private static class Instance {
        private static final EmailDomainManager INSTANCE = new EmailDomainManager();
    }

    public static EmailDomainManager getInstance() {
        return EmailDomainManager.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Table getTable() {
        return Table.get(MEMDOMAIN.TABLE);
    }

    /**
     * <p>
     * Returns the table reference for domain sender records.
     * </p>
     *
     * @return the {@link Table} representing the domain senders table
     */
    protected Table getDomainSenderTable() {
        return Table.get(MEMDOMAINSENDERS.TABLE);
    }

    /**
     * <p>
     * Returns the table reference for domain sender conversation mappings.
     * </p>
     *
     * @return the {@link Table} representing the domain sender conversation table
     */
    protected Table getDomainSendersConversation() {
        return Table.get(MEMDOMAINSENDERCONVERSATION.TABLE);
    }


    /**
     * <p>
     * Returns the table that holds DNS records for domains.
     * </p>
     *
     * @return The {@link Table} containing DNS records.
     */
    private Table getDnsRecordTable() {
        return Table.get(MEMDOMAINDNSRECORDS.TABLE);
    }

    /**
     * <p>
     * Persists a new {@link EmailDomain} into the database.
     * </p>
     *
     * @param emailDomain The {@link EmailDomain} to be created.
     */
    @Override
    public void create(final EmailDomain emailDomain) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainDataIntoContainer(emailDomain, null, targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates an existing {@link EmailDomain} in the database.
     * </p>
     *
     * @param emailDomain The {@link EmailDomain} to be updated.
     */
    @Override
    public void update(final EmailDomain emailDomain) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainDataIntoContainer(emailDomain, getExistingContainer(List.of(emailDomain.getId())), targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves a {@link EmailDomain} by its unique identifier.
     * </p>
     *
     * @param id The ID of the domain to retrieve.
     * @return An {@link Optional} containing the domain if found, or empty otherwise.
     */
    @Override
    public Optional<EmailDomain> get(final long id) {
        final DataContainer dataContainer = getExistingContainer(List.of(id));

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getTable(), id), dataContainer));
    }

    /**
     * <p>
     * Retrieves multiple {@link EmailDomain} objects for a collection of IDs.
     * </p>
     *
     * @param ids A collection of domain IDs.
     * @return A collection of {@link EmailDomain} objects found for the given IDs.
     */
    @Override
    public Collection<EmailDomain> get(final Collection<Long> ids) {
        final DataContainer dataContainer = getExistingContainer(ids);

        return dataContainer.getRows(getTable())
                .map(row -> convertRowToModel(row, dataContainer))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts a database {@link Row} into a {@link EmailDomain} model.
     * </p>
     *
     * @param row           The database row to convert.
     * @param dataContainer The {@link DataContainer} containing additional related rows.
     * @return A {@link EmailDomain} object, or {@code null} if the row is null.
     */
    @Override
    public EmailDomain convertRowToModel(final Row row, final DataContainer dataContainer) {
        if (Objects.isNull(row)) {
            return null;
        }
        final EmailDomain emailDomain = new EmailDomain();
        final Long domainId = row.getPKValue();

        emailDomain.setId(domainId);
        emailDomain.setName(row.get(MEMDOMAIN.NAME));
        emailDomain.setEmailDomainMetaData(getEmailDomainMetaData(row));
        emailDomain.setDnsRecords(getDnsRecords(domainId, dataContainer));

        return emailDomain;
    }

    /**
     * <p>
     * Helps to get the domains by names.
     * </p>
     *
     * @param domainNames Represents the domain names.
     * @return A {@link EmailDomain} object, or {@code null} if the row is null.
     */
    public Collection<EmailDomain> getDomainByNames(final Collection<String> domainNames) {
        final DataContainer dataContainer = getExistingContainerByDomainNames(domainNames);

        return dataContainer.getRows(getTable())
                .map(row -> convertRowToModel(row, dataContainer))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves a {@link EmailDomain} by its name.
     * </p>
     *
     * @param domainName Represents the domain name.
     * @return An {@link Optional} containing the {@link EmailDomain} if found, otherwise {@link Optional#empty()}.
     */
    public Optional<EmailDomain> getDomainByName(final String domainName) {
        final Table table = getTable();
        final DataContainer dataContainer = getExistingContainerByDomainNames(List.of(domainName));

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(table,
                new WhereClause(Criteria.eq(table.getColumn(MEMDOMAIN.NAME), domainName))), dataContainer));
    }

    /**
     * <p>
     * Updates the provided dns records.
     * </p>
     *
     * @param emailDnsRecords Represents the dns records.
     */
    public void updateDnsStatus(final Collection<EmailDnsRecord> emailDnsRecords) {
        final Table dnsRecordTable = getDnsRecordTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Column statusColumn = dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.STATUS);
        final Column dnsRecordPkColumn = dnsRecordTable.getPKColumn();

        for (final EmailDnsRecord emailDnsRecord : emailDnsRecords) {
            final UpdateQuery updateQuery = UpdateQuery.get(dnsRecordTable);

            updateQuery.addUpdateColumn(statusColumn, emailDnsRecord.getStatus().getId());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(dnsRecordPkColumn, emailDnsRecord.getId())));

            updateQueries.add(updateQuery);
        }

        adminDataStore().update(updateQueries);
    }

    /**
     * <p>
     * Retrieves the domain sender identifier for the given domain and email,
     * creating a new domain sender record if one does not already exist.
     * </p>
     *
     * @param domainId the identifier of the domain
     * @param emailId  the identifier of the email address
     * @return the primary key of the existing or newly created domain sender record
     */
    public Long getOrInitDomainSenderId(final Long domainId, final Long emailId) {
        final Table domainSenderTable = getDomainSenderTable();
        final SelectQuery selectQuery = SelectQuery.get(domainSenderTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(domainSenderTable.getColumn(MEMDOMAINSENDERS.DOMAIN_ID), domainId))
                .and(Criteria.eq(domainSenderTable.getColumn(MEMDOMAINSENDERS.EMAIL_ID), emailId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row exisitingRow = dataContainer.getRow(domainSenderTable);
        final Row row = Objects.isNull(exisitingRow) ? new Row(domainSenderTable) : exisitingRow;
        final DataContainer targetContainer = DataContainer.create();

        if (row.isNewRow()) {
            row.set(MEMDOMAINSENDERS.DOMAIN_ID, domainId);
            row.set(MEMDOMAINSENDERS.EMAIL_ID, emailId);

            targetContainer.addNewRow(row);
        }
        adminDataStore().commitChanges(targetContainer);

        return row.getPKValue();
    }

    /**
     * <p>
     * Ensures an association exists between a domain sender and a conversation.
     * </p>
     *
     * @param senderId        the identifier of the domain sender
     * @param conversationId  the identifier of the conversation
     * @param targetContainer the container to which a new association row will be added if required
     */
    public void loadDomainSenderConversations(final Long senderId, final Long conversationId, final DataContainer targetContainer) {
        final Table domainSendersConversation = getDomainSendersConversation();
        final SelectQuery selectQuery = SelectQuery.get(domainSendersConversation);
        final WhereClause whereClause = new WhereClause(Criteria.eq(domainSendersConversation.getColumn(MEMDOMAINSENDERCONVERSATION.CONVERSATION_ID), conversationId))
                .and(Criteria.eq(domainSendersConversation.getColumn(MEMDOMAINSENDERCONVERSATION.SENDER_ID), senderId));

        selectQuery.setWhereClause(whereClause);
        final DataContainer sourceContainer = adminDataStore().get(selectQuery);
        final Row sourceRow = targetContainer.getRow(domainSendersConversation, whereClause);
        final Row conversationRow = Objects.isNull(sourceRow) ? sourceContainer.getRow(domainSendersConversation, whereClause) : sourceRow;
        final Row row = Objects.isNull(conversationRow) ? new Row(domainSendersConversation) : conversationRow;

        if (row.isNewRow()) {
            row.set(MEMDOMAINSENDERCONVERSATION.CONVERSATION_ID, conversationId);
            row.set(MEMDOMAINSENDERCONVERSATION.SENDER_ID, senderId);

            targetContainer.addNewRow(row);
        }
    }

    /**
     * <p>
     * Creates an {@link EmailDomainMetaData} instance from the provided database {@link Row}.
     * </p>
     *
     * @param row the database {@link Row} containing the email domain metadata.
     * @return a new {@link EmailDomainMetaData} instance populated from the row.
     */
    private EmailDomainMetaData getEmailDomainMetaData(final Row row) {
        final EmailDomainMetaData emailDomainMetaData = new EmailDomainMetaData();

        if (Objects.nonNull(row)) {
            emailDomainMetaData.setExternalId(row.get(MEMDOMAIN.EXTERNAL_ID));
        }

        return emailDomainMetaData;
    }

    /**
     * <p>
     * Retrieves an existing {@link DataContainer} filtered by the given domain names.
     * </p>
     *
     * @param domainNames Represents the collection of domain names.
     * @return A {@link DataContainer} containing domain rows matching the given names.
     */
    private DataContainer getExistingContainerByDomainNames(final Collection<String> domainNames) {
        if (Objects.isNull(domainNames) || domainNames.isEmpty()) {
            return DataContainer.create();
        }

        return getExistingContainer(new WhereClause(Criteria.in(getTable().getColumn(MEMDOMAIN.NAME), domainNames)));
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing domains and their DNS records.
     * </p>
     *
     * @param ids Collection of domain IDs.
     * @return A {@link DataContainer} containing the domains and DNS records.
     */
    private DataContainer getExistingContainer(final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return DataContainer.create();
        }

        return getExistingContainer(new WhereClause(Criteria.in(getTable().getPKColumn(), ids)));
    }

    /**
     * <p>
     * Retrieves an existing {@link DataContainer} using the provided {@link WhereClause}.
     * </p>
     *
     * @param whereClause Represents the {@link WhereClause} used to filter domain records.
     * @return A {@link DataContainer} containing the matching domain and DNS record rows.
     */
    private DataContainer getExistingContainer(final WhereClause whereClause) {
        final Table table = getTable();
        final Table dnsRecordTable = getDnsRecordTable();

        final Column pkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(pkColumn, dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.DOMAIN_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads domain data and associated DNS records into the target {@link DataContainer}.
     * </p>
     *
     * @param emailDomain     The {@link EmailDomain} to load.
     * @param sourceContainer The source {@link DataContainer}, can be null.
     * @param targetContainer The target {@link DataContainer} to populate.
     */
    private void loadDomainDataIntoContainer(final EmailDomain emailDomain, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table table = getTable();
        final Long id = emailDomain.getId();
        final WhereClause criteria = new WhereClause(
                Objects.isNull(id) ? Criteria.eq(table.getColumn(MEMDOMAIN.NAME), emailDomain.getName()) : Criteria.eq(table.getPKColumn(), id)
        );
        final Row existingSourceRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table, criteria);
        final Row existingRow = Objects.isNull(existingSourceRow) ? targetContainer.getRow(table, criteria) : existingSourceRow;
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;

        if (row.isNewRow()) {
            row.set(MEMDOMAIN.NAME, emailDomain.getName());
            row.set(MEMDOMAIN.EXTERNAL_ID, emailDomain.getEmailDomainMetaData().getExternalId());
            targetContainer.addNewRow(row);
        }
        final Long pkValue = row.getPKValue();

        emailDomain.setId(pkValue);
        loadDnsRecordIntoContainer(pkValue, emailDomain.getEmailDnsRecords(), sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads DNS records for a domain into the target {@link DataContainer}.
     * </p>
     *
     * @param domainId        The domain ID.
     * @param emailDnsRecords A collection of {@link EmailDnsRecord} objects.
     * @param sourceContainer The source {@link DataContainer}, can be null.
     * @param targetContainer The target {@link DataContainer}.
     */
    private void loadDnsRecordIntoContainer(final Long domainId, final Collection<EmailDnsRecord> emailDnsRecords,
                                            final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dnsRecordTable = getDnsRecordTable();
        final Column domainColumn = dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.DOMAIN_ID);
        final WhereClause domainCriteria = new WhereClause(Criteria.eq(domainColumn, domainId));

        for (final EmailDnsRecord emailDnsRecord : emailDnsRecords) {
            final Long id = emailDnsRecord.getId();
            final WhereClause pkCriteria = new WhereClause(Criteria.eq(dnsRecordTable.getPKColumn(), emailDnsRecord.id()))
                    .and(domainCriteria);
            final WhereClause criteria = Objects.isNull(id) ?
                    new WhereClause(Criteria.eq(dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.HOST), emailDnsRecord.getHost()))
                            .and(Criteria.eq(dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.RECORD_TYPE), emailDnsRecord.getRecordType().getId()))
                            .and(domainCriteria) : pkCriteria;
            final Row existingSourceRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(dnsRecordTable, criteria);
            final Row existingRow = Objects.isNull(existingSourceRow) ? targetContainer.getRow(dnsRecordTable, criteria) : existingSourceRow;
            final Row row = Objects.isNull(existingRow) ? new Row(dnsRecordTable) : existingRow;

            if (row.isNewRow()) {
                row.set(MEMDOMAINDNSRECORDS.DOMAIN_ID, domainId);
            }
            row.set(MEMDOMAINDNSRECORDS.HOST, emailDnsRecord.getHost());
            row.set(MEMDOMAINDNSRECORDS.VALUE, emailDnsRecord.getValue());
            row.set(MEMDOMAINDNSRECORDS.RECORD_TYPE, emailDnsRecord.getRecordType().getId());
            row.set(MEMDOMAINDNSRECORDS.CATEGORY, emailDnsRecord.getCategory().getId());
            row.set(MEMDOMAINDNSRECORDS.STATUS, emailDnsRecord.getStatus().getId());
            row.set(MEMDOMAINDNSRECORDS.PROVIDER, emailDnsRecord.getRecordProvider().getId());

            emailDnsRecord.setDomainId(domainId);
            emailDnsRecord.setId(row.getPKValue());
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Retrieves DNS records for a domain from the provided {@link DataContainer}.
     * </p>
     *
     * @param domainId      The domain ID.
     * @param dataContainer The {@link DataContainer} containing DNS record rows.
     * @return A collection of {@link EmailDnsRecord} objects.
     */
    private Collection<DnsRecord> getDnsRecords(final Long domainId, final DataContainer dataContainer) {
        final Table dnsRecordTable = getDnsRecordTable();
        final Collection<DnsRecord> emailDnsRecords = new HashSet<>();

        dataContainer.getRows(dnsRecordTable, new WhereClause(Criteria.eq(dnsRecordTable.getColumn(MEMDOMAINDNSRECORDS.DOMAIN_ID), domainId)))
                .forEach(row -> {
                    final EmailDnsRecord emailDnsRecord = new EmailDnsRecord();

                    emailDnsRecord.setId(row.getPKValue());
                    emailDnsRecord.setDomainId(domainId);
                    emailDnsRecord.setHost(row.get(MEMDOMAINDNSRECORDS.HOST));
                    emailDnsRecord.setValue(row.get(MEMDOMAINDNSRECORDS.VALUE));
                    emailDnsRecord.setStatus(Status.get(row.get(MEMDOMAINDNSRECORDS.STATUS)));
                    emailDnsRecord.setRecordType(RecordType.get(row.get(MEMDOMAINDNSRECORDS.RECORD_TYPE)));
                    emailDnsRecord.setCategory(Category.get(row.get(MEMDOMAINDNSRECORDS.CATEGORY)));
                    emailDnsRecord.setRecordProvider(EmailDnsRecord.RecordProvider.get(row.get(MEMDOMAINDNSRECORDS.PROVIDER)));

                    emailDnsRecords.add(emailDnsRecord);
                });

        return emailDnsRecords;
    }
}