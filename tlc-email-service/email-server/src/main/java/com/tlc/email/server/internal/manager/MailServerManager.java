package com.tlc.email.server.internal.manager;

import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.SecurityMode;
import com.tlc.email.server.internal.protocols.models.Server;
import com.tlc.email.server.model.Protocol;
import com.tlc.email.server.sql.resource.MEMSERVER;
import com.tlc.email.server.sql.resource.MEMSERVERAUTHTYPE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manager class responsible for CRUD operations and mapping database rows
 * to {@link Server} objects for email server configurations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MailServerManager extends AbstractEmailManager<Server> {

    private MailServerManager() {
    }

    private static class Instance {
        private static final MailServerManager INSTANCE = new MailServerManager();
    }

    public static MailServerManager getInstance() {
        return MailServerManager.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Table getTable() {
        return Table.get(MEMSERVER.TABLE);
    }

    /**
     * <p>
     * Returns the table that holds server authentication types.
     * </p>
     *
     * @return The {@link Table} containing server authentication types.
     */
    private Table getServerAuthTypeTable() {
        return Table.get(MEMSERVERAUTHTYPE.TABLE);
    }

    /**
     * <p>
     * Persists a new {@link Server} into the database.
     * </p>
     *
     * @param server The {@link Server} to be created.
     */
    @Override
    public void create(final Server server) {
        final DataContainer targetContainer = DataContainer.create();

        loadServerDataIntoContainer(server, null, targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates an existing {@link Server} in the database.
     * </p>
     *
     * @param server The {@link Server} to be updated.
     */
    @Override
    public void update(final Server server) {
        final DataContainer targetContainer = DataContainer.create();

        loadServerDataIntoContainer(server, getExistingContainer(server.getHost(), server.getPort()), targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves a {@link Server} by its unique identifier.
     * </p>
     *
     * @param id The ID of the server to retrieve.
     * @return An {@link Optional} containing the server if found, or empty otherwise.
     */
    @Override
    public Optional<Server> get(final long id) {
        final DataContainer dataContainer = getExistingContainer(List.of(id));

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getTable(), id), dataContainer));
    }

    /**
     * <p>
     * Retrieves multiple {@link Server} objects for a collection of IDs.
     * </p>
     *
     * @param ids A collection of server IDs.
     * @return A collection of {@link Server} objects found for the given IDs.
     */
    @Override
    public Collection<Server> get(final Collection<Long> ids) {
        return ids.stream().map(this::get).flatMap(Optional::stream).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts a database {@link Row} into a {@link Server} model.
     * </p>
     *
     * @param row           The database row to convert.
     * @param dataContainer The {@link DataContainer} containing additional related rows.
     * @return A {@link Server} object, or {@code null} if the row is null.
     */
    @Override
    public Server convertRowToModel(final Row row, final DataContainer dataContainer) {
        if (Objects.isNull(row)) {
            return null;
        }
        final Server server = new Server();

        server.setId(row.getPKValue());
        server.setHost(row.get(MEMSERVER.HOST));
        server.setPort(row.get(MEMSERVER.PORT));
        server.setProtocol(Protocol.get((short) row.get(MEMSERVER.PROTOCOL)));
        server.setSecurityMode(SecurityMode.get(row.get(MEMSERVER.SECURITY_MODE)));
        server.setAuthTypes(getAuthTypes(row, dataContainer));

        return server;
    }

    /**
     * <p>
     * Retrieves a {@link Server} by its unique identifier.
     * </p>
     *
     * @param host Represent the host name.
     * @param port Represent the port name.
     * @return An {@link Optional} containing the server if found, or empty otherwise.
     */
    public Optional<Server> get(final String host, final int port) {
        final DataContainer dataContainer = getExistingContainer(host, port);

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(getTable(),
                getServerCriteria(host, port)), dataContainer));
    }

    /**
     * <p>
     * Retrieves authentication types associated with a server row.
     * </p>
     *
     * @param row           The server {@link Row}.
     * @param dataContainer The {@link DataContainer} containing related authentication rows.
     * @return A collection of {@link AuthType} objects.
     */
    private Collection<AuthType> getAuthTypes(final Row row, final DataContainer dataContainer) {
        final Table serverAuthTypeTable = getServerAuthTypeTable();
        final Stream<Row> authTypes = dataContainer.getRows(serverAuthTypeTable,
                new WhereClause(Criteria.eq(serverAuthTypeTable.getColumn(MEMSERVERAUTHTYPE.SERVER_ID), row.getPKValue())));

        return authTypes.map(value -> AuthType.get(value.get(MEMSERVERAUTHTYPE.AUTH_TYPE)))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing the server and its authentication types.
     * </p>
     *
     * @param host Represent the host.
     * @param port Represent the port.
     * @return A {@link DataContainer} with server and auth type rows.
     */
    private DataContainer getExistingContainer(final String host, final int port) {
        return getExistingContainer(getServerCriteria(host, port));
    }

    /**
     * <p>
     * Builds a {@link WhereClause} used to identify a server by its host and port.
     * </p>
     *
     * @param host Represent the host.
     * @param port Represent the port.
     * @return A {@link WhereClause} matching the specified server.
     */
    private WhereClause getServerCriteria(final String host, final int port) {
        final Table table = getTable();

        return new WhereClause(Criteria.eq(table.getColumn(MEMSERVER.HOST), host))
                .and(Criteria.eq(table.getColumn(MEMSERVER.PORT), port));
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing the server and its authentication types.
     * </p>
     *
     * @param ids Represent the ids.
     * @return A {@link DataContainer} with server and auth type rows.
     */
    private DataContainer getExistingContainer(final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return DataContainer.create();
        }
        return getExistingContainer(new WhereClause(Criteria.in(getTable().getPKColumn(), ids)));
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing the server and its authentication types.
     * </p>
     *
     * @param whereClause Represent the criteria.
     * @return A {@link DataContainer} with server and auth type rows.
     */
    private DataContainer getExistingContainer(final WhereClause whereClause) {
        final Table table = getTable();
        final Table serverAuthTypeTable = getServerAuthTypeTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), serverAuthTypeTable.getColumn(MEMSERVERAUTHTYPE.SERVER_ID)));
        selectQuery.addSelectClause(table);
        selectQuery.addSelectClause(serverAuthTypeTable);
        selectQuery.setWhereClause(whereClause);

        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads server data and authentication types into the target {@link DataContainer}.
     * </p>
     *
     * @param server          The {@link Server} to load.
     * @param sourceContainer The source {@link DataContainer}, can be null.
     * @param targetContainer The target {@link DataContainer} to populate.
     */
    private void loadServerDataIntoContainer(final Server server, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final WhereClause criteria = new WhereClause(Criteria.eq(table.getPKColumn(), server.id()));
        final Row existingSourceRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table, criteria);
        final Row existingRow = Objects.isNull(existingSourceRow) ? targetContainer.getRow(table, criteria) : existingSourceRow;
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;

        row.set(MEMSERVER.HOST, server.getHost());
        row.set(MEMSERVER.PORT, server.getPort());
        row.set(MEMSERVER.PROTOCOL, server.getProtocol().getId());
        row.set(MEMSERVER.SECURITY_MODE, server.getSecurityMode().getId());
        final long pkValue = row.getPKValue();

        targetContainer.updateRow(row);
        server.setId(pkValue);

        loadServerAuthTypeIntoContainer(pkValue, server.getAuthTypes(), sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads authentication types for a server into the target {@link DataContainer}.
     * </p>
     *
     * @param serverId        The server ID.
     * @param authTypes       A collection of {@link AuthType} objects.
     * @param sourceContainer The source {@link DataContainer}, can be null.
     * @param targetContainer The target {@link DataContainer}.
     */
    private void loadServerAuthTypeIntoContainer(final long serverId, final Collection<AuthType> authTypes,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getServerAuthTypeTable();
        final WhereClause criteria = new WhereClause(Criteria.eq(table.getColumn(MEMSERVERAUTHTYPE.SERVER_ID), serverId));

        final Map<Short, Row> existingTargetRows = targetContainer.getRows(table, criteria)
                .collect(Collectors.toMap(
                        row -> row.get(table.getColumn(MEMSERVERAUTHTYPE.AUTH_TYPE)),
                        Function.identity()
                ));

        final Map<Short, Row> existingSourceRows = Objects.isNull(sourceContainer) ? Collections.emptyMap() :
                sourceContainer.getRows(table, criteria)
                        .collect(Collectors.toMap(
                                row -> row.get(table.getColumn(MEMSERVERAUTHTYPE.AUTH_TYPE)),
                                Function.identity()
                        ));

        final Collection<Short> authTypeIds = authTypes.stream()
                .map(AuthType::getId)
                .collect(Collectors.toSet());

        for (final short authTypeId : authTypeIds) {

            if (!existingTargetRows.containsKey(authTypeId) && !existingSourceRows.containsKey(authTypeId)) {
                final Row row = new Row(table);

                row.set(MEMSERVERAUTHTYPE.SERVER_ID, serverId);
                row.set(MEMSERVERAUTHTYPE.AUTH_TYPE, authTypeId);
                targetContainer.addNewRow(row);
            }
        }

        existingTargetRows.values().stream()
                .filter(row -> !authTypeIds.contains((Short) row.get(table.getColumn(MEMSERVERAUTHTYPE.AUTH_TYPE))))
                .forEach(targetContainer::deleteRow);

        existingSourceRows.values().stream()
                .filter(row -> !authTypeIds.contains((Short) row.get(table.getColumn(MEMSERVERAUTHTYPE.AUTH_TYPE))))
                .forEach(targetContainer::deleteRow);
    }
}