package com.tlc.queue.internal.manager.consumer;

import com.tlc.queue.internal.manager.AbstractManager;
import com.tlc.queue.internal.model.ConsumerGroupInfo;
import com.tlc.queue.sql.resource.MQCONSUMERGROUP;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Defines DB operations for the consumer offered by the {@link ConsumerGroupManager}
 *
 * @author sundar
 * @version 1.0
 * @see ConsumerGroupManager
 */
class ConsumerGroupManagerImpl extends AbstractManager<ConsumerGroupInfo> implements ConsumerGroupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerGroupManagerImpl.class);

    private ConsumerGroupManagerImpl() {
    }

    private static class Instance {
        private static final ConsumerGroupManager CONSUMER_MANAGER = new ConsumerGroupManagerImpl();
    }

    /**
     * Gets the {@link ConsumerGroupManager}
     *
     * @return {@link ConsumerGroupManager}
     */
    static ConsumerGroupManager getInstance() {
        return Instance.CONSUMER_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the collection of {@link ConsumerGroupInfo}
     */
    @Override
    public Collection<ConsumerGroupInfo> getAll() {
        final Table table = getTable();

        return getReadableDataStore().get(table, new WhereClause(Criteria.notEq(table.getPKColumn(), null)))
                .getRows(table).map(this::convertRowToModel).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId Represents the consumer group ID
     */
    @Override
    public void createIfNotExist(final String groupId) {

        if (!exist(groupId)) {
            final DataContainer dataContainer = DataContainer.create();
            final Row row = new Row(getTable());

            row.set(MQCONSUMERGROUP.GROUP_ID, groupId);
            dataContainer.addNewRow(row);
            getWritableDataStore().commitChanges(dataContainer);
            LOGGER.debug("Queue: Consumer group is created on DB, Group ID: {}", groupId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId Represents the consumer group ID
     * @return The {@link ConsumerGroupInfo} if exist
     */
    @Override
    public Optional<ConsumerGroupInfo> get(final String groupId) {
        final Table table = getTable();
        final Row row = getReadableDataStore().get(table, new WhereClause(Criteria.eq(table.getColumn(MQCONSUMERGROUP.GROUP_ID), groupId))).getRow(table);

        if (Objects.isNull(row)) {
            return Optional.empty();
        }
        return Optional.of(convertRowToModel(row));
    }

    /**
     * {@inheritDoc}
     *
     * @param row To be converted
     * @return {@link ConsumerGroupInfo}
     */
    @Override
    protected ConsumerGroupInfo convertRowToModel(final Row row) {
        return new ConsumerGroupInfo(row.getPKValue(), row.get(MQCONSUMERGROUP.GROUP_ID));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    @Override
    protected Table getTable() {
        return Table.get(MQCONSUMERGROUP.TABLE);
    }

    /**
     * Checks whether the {@link ConsumerGroupInfo} associated with the consumer group ID
     *
     * @param groupId Represents the consumer group ID
     * @return true if the consumer group is exist; false otherwise
     */
    private boolean exist(final String groupId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MQCONSUMERGROUP.GROUP_ID), groupId)));
        return getReadableDataStore().get(countQuery) == 1;
    }
}