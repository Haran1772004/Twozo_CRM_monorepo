package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.BasicAuthCredentials;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.model.Protocol;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMEMAILAUTHTYPE;
import com.tlc.email.server.sql.resource.MEMEMAILSERVERCONFIG;
import com.tlc.email.server.sql.resource.MEMPASSWORDAUTHCREDENTIALS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.JoinClause.JoinType;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages creation, update, retrieval, and conversion of {@link MailBoxConfiguration} data,
 * including email addresses, server configurations, and authentication credentials.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailBoxConfManager extends AbstractEmailManager<MailBoxConfiguration> {

    private final MailServerManager mailServerManager;

    /**
     * <p>
     * Private constructor to enforce singleton pattern and initialize {@link MailServerManager}.
     * </p>
     */
    private MailBoxConfManager() {
        mailServerManager = MailServerManager.getInstance();
    }

    private static class Instance {
        private static final MailBoxConfManager INSTANCE = new MailBoxConfManager();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link MailBoxConfManager}.
     * </p>
     *
     * @return {@link MailBoxConfManager} instance.
     */
    public static MailBoxConfManager getInstance() {
        return MailBoxConfManager.Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns the table containing mail server conf.
     * </p>
     *
     * @return {@link Table} for server conf.
     */
    @Override
    protected Table getTable() {
        return Table.get(MEMEMAILSERVERCONFIG.TABLE);
    }

    /**
     * <p>
     * Returns the table containing authentication type data.
     * </p>
     *
     * @return {@link Table} for auth type.
     */
    protected Table getAuthTable() {
        return Table.get(MEMEMAILAUTHTYPE.TABLE);
    }

    /**
     * <p>
     * Returns the table containing password credentials.
     * </p>
     *
     * @return {@link Table} for password credentials.
     */
    protected Table getPasswordCredTable() {
        return Table.get(MEMPASSWORDAUTHCREDENTIALS.TABLE);
    }

    /**
     * <p>
     * Persists a new {@link MailBoxConfiguration} including email, server, and auth info.
     * </p>
     *
     * @param mailBoxConfiguration {@link MailBoxConfiguration} to create.
     */
    @Override
    public void create(final MailBoxConfiguration mailBoxConfiguration) {
        final DataContainer targetContainer = DataContainer.create();

        loadDataIntoContainer(mailBoxConfiguration, null, targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates an existing {@link MailBoxConfiguration} in the database.
     * </p>
     *
     * @param mailBoxConfiguration {@link MailBoxConfiguration} to update.
     */
    @Override
    public void update(final MailBoxConfiguration mailBoxConfiguration) {
        final DataContainer targetContainer = DataContainer.create();

        loadDataIntoContainer(mailBoxConfiguration, getContainer(mailBoxConfiguration.getEmailAddress().getEmailAddress()), targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves a {@link MailBoxConfiguration} by email ID.
     * </p>
     *
     * @param emailId Email ID to query.
     * @return Optional {@link MailBoxConfiguration}.
     */
    @Override
    public Optional<MailBoxConfiguration> get(final long emailId) {
        final DataContainer dataContainer = getContainer(emailId);

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getAddressTable()), dataContainer));
    }

    /**
     * <p>
     * Retrieves multiple {@link MailBoxConfiguration} objects by their email IDs.
     * </p>
     *
     * @param emailIds Collection of email IDs.
     * @return Collection of {@link MailBoxConfiguration}.
     */
    @Override
    public Collection<MailBoxConfiguration> get(final Collection<Long> emailIds) {
        return emailIds.stream().map(this::get).flatMap(Optional::stream).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts a database {@link Row} and {@link DataContainer} into {@link MailBoxConfiguration}.
     * </p>
     *
     * @param row           Row representing the email address.
     * @param dataContainer Container holding related tables.
     * @return {@link MailBoxConfiguration} instance.
     */
    @Override
    public MailBoxConfiguration convertRowToModel(final Row row, final DataContainer dataContainer) {
        if (Objects.isNull(row)) {
            return null;
        }
        final MailBoxConfiguration mailBoxConfiguration = new MailBoxConfiguration();
        final Long emailId = row.getPKValue();
        final EmailAddress emailAddress = getAddress(row);

        mailBoxConfiguration.setEmailAddress(emailAddress);
        mailBoxConfiguration.setServer(getServers(emailId, dataContainer));
        mailBoxConfiguration.setAuthCredentials(getAuthCredentials(emailAddress, dataContainer));

        return mailBoxConfiguration;
    }

    /**
     * <p>
     * Retrieves the mailbox configuration for the given email address.
     * </p>
     *
     * @param emailAddress Email address to look up.
     * @return {@link Optional} containing {@link MailBoxConfiguration} if found.
     */
    public Optional<MailBoxConfiguration> get(final String emailAddress) {
        final DataContainer dataContainer = getContainer(emailAddress);

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getAddressTable()), dataContainer));
    }

    /**
     * <p>
     * Loads mailbox configuration data into the target container.
     * </p>
     *
     * @param mailBoxConfiguration Source mailbox configuration.
     * @param sourceContainer      Existing data container.
     * @param targetContainer      Container to persist changes.
     */
    private void loadDataIntoContainer(final MailBoxConfiguration mailBoxConfiguration,
                                       final DataContainer sourceContainer, final DataContainer targetContainer) {
        final EmailAddress emailAddress = mailBoxConfiguration.getEmailAddress();

        loadEmailIntoContainer(emailAddress, sourceContainer, targetContainer);
        final Long emailId = emailAddress.id();

        loadEmailServerIntoContainer(emailId, mailBoxConfiguration.getServer(), sourceContainer, targetContainer);
        loadAuthDataIntoContainer(emailId, mailBoxConfiguration.getAuthCredentials(), sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Persists authentication type data for the given email ID.
     * </p>
     *
     * @param emailId         Email identifier.
     * @param authCredentials Authentication credentials.
     * @param sourceContainer Existing data container.
     * @param targetContainer Container to persist changes.
     */
    private void loadAuthDataIntoContainer(final Long emailId, final AuthCredentials authCredentials,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table authTable = getAuthTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(authTable.getColumn(MEMEMAILAUTHTYPE.EMAIL_ID), emailId));
        final Row targetContainerRow = targetContainer.getRow(authTable, whereClause);
        final Row existingRow = Objects.isNull(targetContainerRow) ? sourceContainer.getRow(authTable, whereClause) : targetContainerRow;
        final Row row = Objects.isNull(existingRow) ? new Row(authTable) : existingRow;
        final AuthType authType = authCredentials.authType();

        row.set(MEMEMAILAUTHTYPE.EMAIL_ID, emailId);
        row.set(MEMEMAILAUTHTYPE.AUTH_TYPE, authType.getId());

        targetContainer.updateRow(row);

        switch (authType) {
            case LOGIN, PLAIN -> loadCredentialsIntoContainer(row.getPKValue(),
                    (BasicAuthCredentials) authCredentials,
                    sourceContainer,
                    targetContainer);
        }
    }

    /**
     * <p>
     * Loads password-based authentication credentials into the container.
     * </p>
     *
     * @param authId          Authentication record ID.
     * @param authCredentials Password credentials.
     * @param sourceContainer Existing data container.
     * @param targetContainer Container to persist changes.
     */
    private void loadCredentialsIntoContainer(final Long authId, final BasicAuthCredentials authCredentials,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table passwordCredTable = getPasswordCredTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(passwordCredTable.getColumn(MEMPASSWORDAUTHCREDENTIALS.AUTH_ID), authId));
        final Row targetContainerRow = targetContainer.getRow(passwordCredTable, whereClause);
        final Row existingRow = Objects.isNull(targetContainerRow) ? sourceContainer.getRow(passwordCredTable, whereClause) : targetContainerRow;
        final Row row = Objects.isNull(existingRow) ? new Row(passwordCredTable) : existingRow;

        if (row.isNewRow()) {
            row.set(MEMPASSWORDAUTHCREDENTIALS.AUTH_ID, authId);
        }
        row.set(MEMPASSWORDAUTHCREDENTIALS.PASSWORD, authCredentials.encryptedPassword());
        row.set(MEMPASSWORDAUTHCREDENTIALS.USER_NAME, authCredentials.username());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Maps email servers to the given email ID and removes stale mappings.
     * </p>
     *
     * @param emailId         Email identifier.
     * @param emailServers    Protocol-to-server mapping.
     * @param sourceContainer Existing data container.
     * @param targetContainer Container to persist changes.
     */
    private void loadEmailServerIntoContainer(final Long emailId, final Map<Protocol, Server> emailServers,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<Long> serverIds = new HashSet<>();
        final Table emailServerTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(emailServerTable.getColumn(MEMEMAILSERVERCONFIG.EMAIL_ID), emailId));

        for (final Server server : emailServers.values()) {
            final Long serverId = server.id();
            final WhereClause criteria = whereClause.and(Criteria.eq(emailServerTable.getColumn(MEMEMAILSERVERCONFIG.SERVER_ID), serverId));
            final Row targetContainerRow = targetContainer.getRow(emailServerTable, criteria);
            final Row existingRow = Objects.isNull(targetContainerRow) ? sourceContainer.getRow(emailServerTable, criteria) : targetContainerRow;
            final Row row = Objects.isNull(existingRow) ? new Row(emailServerTable) : existingRow;

            if (row.isNewRow()) {
                row.set(MEMEMAILSERVERCONFIG.SERVER_ID, serverId);
                row.set(MEMEMAILSERVERCONFIG.EMAIL_ID, emailId);
                targetContainer.addNewRow(row);
            }
            serverIds.add(serverId);
            server.setId(row.getPKValue());
        }
        removeUnmappedRows(emailId, serverIds, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Removes server mappings that are no longer associated with the email.
     * </p>
     *
     * @param emailId         Email identifier.
     * @param serverIds       Active server IDs.
     * @param sourceContainer Existing data container.
     * @param targetContainer Container to persist changes.
     */
    private void removeUnmappedRows(final Long emailId, final Collection<Long> serverIds,
                                    final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(MEMEMAILSERVERCONFIG.EMAIL_ID), emailId));

        removeUnmappedRows(sourceContainer, table, whereClause, serverIds);
        removeUnmappedRows(targetContainer, table, whereClause, serverIds);
    }

    /**
     * <p>
     * Deletes rows that are not present in the provided server ID list.
     * </p>
     *
     * @param container   Data container to clean.
     * @param table       Target table.
     * @param whereClause Filter condition.
     * @param serverIds   Allowed server identifiers.
     */
    private void removeUnmappedRows(final DataContainer container,
                                    final Table table,
                                    final WhereClause whereClause,
                                    final Collection<Long> serverIds) {
        final Collection<Row> rowsToDelete =
                container.getRows(table, whereClause)
                        .filter(row -> !serverIds.contains((Long) row.get(MEMEMAILSERVERCONFIG.SERVER_ID)))
                        .toList();

        rowsToDelete.forEach(container::deleteRow);
    }

    /**
     * <p>
     * Loads or creates an email address record and assigns its ID.
     * </p>
     *
     * @param emailAddress    Email address model.
     * @param sourceContainer Existing data container.
     * @param targetContainer Target data container.
     */
    private void loadEmailIntoContainer(final EmailAddress emailAddress, final DataContainer sourceContainer,
                                        final DataContainer targetContainer) {
        final Table addressTable = getAddressTable();
        final String address = emailAddress.getEmailAddress();
        final WhereClause whereClause = new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), address));
        final Row targetRow = targetContainer.getRow(addressTable, whereClause);
        final Row sourceRow = Objects.isNull(targetRow) ? sourceContainer.getRow(addressTable, whereClause) : targetRow;
        final Row row = Objects.isNull(sourceRow) ? new Row(addressTable) : sourceRow;

        if (row.isNewRow()) {
            row.set(MEMADDRESS.EMAIL_ADDRESS, address);
            targetContainer.addNewRow(row);
        }
        emailAddress.setId(row.getPKValue());
    }

    /**
     * <p>
     * Retrieves server configurations associated with the email ID.
     * </p>
     *
     * @param emailId       Email identifier.
     * @param dataContainer Data container.
     * @return Protocol-to-server mapping.
     */
    private Map<Protocol, Server> getServers(final Long emailId, final DataContainer dataContainer) {
        final Table table = getTable();
        final Collection<Long> serverIds = dataContainer.getRows(table, new WhereClause(Criteria.eq(table.getColumn(MEMEMAILAUTHTYPE.EMAIL_ID), emailId)))
                .map(confRow -> (Long) confRow.get(MEMEMAILSERVERCONFIG.SERVER_ID))
                .collect(Collectors.toSet());

        return mailServerManager.get(serverIds)
                .stream()
                .collect(Collectors.toMap(
                        Server::getProtocol,
                        Function.identity(),
                        (existing, duplicate) -> existing
                ));
    }

    /**
     * <p>
     * Retrieves authentication credentials for the given email ID.
     * </p>
     *
     * @param emailAddress  Represent the email address.
     * @param dataContainer Data container.
     * @return Authentication credentials.
     */
    private AuthCredentials getAuthCredentials(final EmailAddress emailAddress, final DataContainer dataContainer) {
        final Table table = getAuthTable();
        final Row authRow = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getColumn(MEMEMAILAUTHTYPE.EMAIL_ID), emailAddress.id())));

        if (Objects.isNull(authRow)) {
            return null;
        }
        final AuthType authType = AuthType.get(authRow.get(MEMEMAILAUTHTYPE.AUTH_TYPE));

        return switch (authType) {
            case LOGIN, PLAIN -> getPasswordCredentials(authRow.getPKValue(), emailAddress, authType, dataContainer);
        };
    }

    /**
     * <p>
     * Retrieves password-based authentication credentials.
     * </p>
     *
     * @param authId        Authentication identifier.
     * @param emailAddress  Email address.
     * @param authType      Authentication type.
     * @param dataContainer Data container.
     * @return Password credentials.
     */
    private AuthCredentials getPasswordCredentials(final Long authId, final EmailAddress emailAddress, final AuthType authType,
                                                   final DataContainer dataContainer) {
        final Table table = getPasswordCredTable();
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getColumn(MEMPASSWORDAUTHCREDENTIALS.AUTH_ID), authId)));

        if (Objects.isNull(row)) {
            return null;
        }

        return BasicAuthCredentials
                .builder()
                .username(row.get(MEMPASSWORDAUTHCREDENTIALS.USER_NAME))
                .encryptedPassword(row.get(MEMPASSWORDAUTHCREDENTIALS.PASSWORD))
                .authType(authType)
                .emailAddress(emailAddress)
                .build();
    }

    /**
     * <p>
     * Converts an address row into an {@link EmailAddress}.
     * </p>
     *
     * @param row Address table row.
     * @return Email address model.
     */
    private EmailAddress getAddress(final Row row) {
        if (Objects.isNull(row)) {
            return null;
        }
        final EmailAddress emailAddress = new EmailAddress();

        emailAddress.setId(row.getPKValue());
        emailAddress.setEmailAddress(row.get(MEMADDRESS.EMAIL_ADDRESS));

        return emailAddress;
    }

    /**
     * <p>
     * Loads mailbox data using the email identifier.
     * </p>
     *
     * @param emailId Email identifier.
     * @return Data container.
     */
    private DataContainer getContainer(final Long emailId) {
        return getContainer(new WhereClause(Criteria.eq(getAddressTable().getPKColumn(), emailId)));
    }

    /**
     * <p>
     * Loads mailbox data using the email address.
     * </p>
     *
     * @param emailAddress Email address.
     * @return Data container.
     */
    private DataContainer getContainer(final String emailAddress) {
        return getContainer(new WhereClause(Criteria.eq(getAddressTable().getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
    }

    /**
     * <p>
     * Loads mailbox configuration using the provided filter.
     * </p>
     *
     * @param whereClause Query filter.
     * @return Data container.
     */
    private DataContainer getContainer(final WhereClause whereClause) {
        final Table addressTable = getAddressTable();
        final Table mailBoxConfTable = getTable();
        final Table authType = getAuthTable();
        final Table passwordCredTable = getPasswordCredTable();

        final Column addressPkcolumn = addressTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(addressPkcolumn, mailBoxConfTable.getColumn(MEMEMAILAUTHTYPE.EMAIL_ID), JoinType.LEFT),
                        new JoinClause(addressPkcolumn, authType.getColumn(MEMEMAILAUTHTYPE.EMAIL_ID), JoinType.LEFT),
                        new JoinClause(authType.getPKColumn(), passwordCredTable.getColumn(MEMPASSWORDAUTHCREDENTIALS.AUTH_ID), JoinType.LEFT)
                )
        );
        selectQuery.addSelectClause(
                addressTable,
                mailBoxConfTable,
                authType,
                passwordCredTable);
        selectQuery.setWhereClause(whereClause);

        return readableDataStore().get(selectQuery);
    }
}
