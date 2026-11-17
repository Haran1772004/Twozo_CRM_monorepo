package com.tlc.queue.internal.manager.offset;

import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.cache.consumer.ConsumerGroupService;
import com.tlc.queue.internal.cache.topic.TopicService;
import com.tlc.queue.internal.manager.AbstractManager;
import com.tlc.queue.internal.model.ConsumerGroupInfo;
import com.tlc.queue.internal.model.ProcessedOffset;
import com.tlc.queue.internal.model.TopicInfo;
import com.tlc.queue.sql.resource.MQPROCESSEDOFFSET;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Manages the processed offsets DB operations
 *
 * @author sundar
 * @version 1.0
 * @see ProcessedOffsetManager
 */
class ProcessedOffsetManagerImpl extends AbstractManager<ProcessedOffset> implements ProcessedOffsetManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedOffsetManagerImpl.class);

    private final ConsumerGroupService consumerGroupService;
    private final TopicService topicService;

    private ProcessedOffsetManagerImpl() {
        this.consumerGroupService = ConsumerGroupService.getInstance();
        this.topicService = TopicService.getInstance();
    }

    private static class Instance {
        private static final ProcessedOffsetManager OFFSET_MANAGER = new ProcessedOffsetManagerImpl();
    }

    /**
     * Gets the {@link ProcessedOffsetManager}
     *
     * @return {@link ProcessedOffsetManager}
     */
    static ProcessedOffsetManager getInstance() {
        return Instance.OFFSET_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param processedOffset To be stored
     */
    @Override
    public void create(final ProcessedOffset processedOffset) {
        final Table table = getTable();
        final Row row = new Row(table);
        final DataContainer dataContainer = DataContainer.create();
        final OffsetHolder offsetHolder = processedOffset.offsetHolder();

        row.set(MQPROCESSEDOFFSET.PARTITION, offsetHolder.partition());
        row.set(MQPROCESSEDOFFSET.OFFSET, offsetHolder.offset());
        row.set(MQPROCESSEDOFFSET.TOPIC_ID, processedOffset.topicInfo().id());
        row.set(MQPROCESSEDOFFSET.CONSUMER_GROUP_ID, processedOffset.consumerGroupInfo().id());

        getWritableDataStore().commitChanges(dataContainer);
        LOGGER.debug("Queue: Processed offset is created on DB, Offset: {}, Group ID: {}", offsetHolder, processedOffset.consumerGroupInfo().groupId());
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic name
     * @param partition Represents the partition
     * @return {@link Collection} of processed offsets
     */
    @Override
    public Collection<ProcessedOffset> get(final String groupId, final String topic, final int partition) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(buildWhereClause(table, groupId, topic, partition));

        return getReadableDataStore().get(selectQuery).getRows(table).map(this::convertRowToModel).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param offsetHolder    Represents the offset
     * @return true if the offset exists, false otherwise
     */
    @Override
    public boolean exists(final String consumerGroupId, final OffsetHolder offsetHolder) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(buildWhereClause(table, consumerGroupId, offsetHolder.topic(), offsetHolder.partition()));
        return getReadableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param processedOffset To be removed
     */
    @Override
    public void remove(final ProcessedOffset processedOffset) {
        try {
            final Table table = getTable();
            final DeleteQuery deleteQuery = DeleteQuery.get(table);
            final OffsetHolder offsetHolder = processedOffset.offsetHolder();

            deleteQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.CONSUMER_GROUP_ID), processedOffset.consumerGroupInfo().id()))
                    .and(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.TOPIC_ID), processedOffset.topicInfo().id()))
                    .and(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.PARTITION), offsetHolder.partition()))
                    .and(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.OFFSET), offsetHolder.offset())));

            getWritableDataStore().delete(deleteQuery);
            LOGGER.debug("Queue: Processed offset is removed from the DB, Offset: {}, Group ID: {}", offsetHolder, processedOffset.consumerGroupInfo().groupId());
        } catch (Throwable throwable) {
            LOGGER.error("Queue: Processed offset is remove failed from the DB, Offset: {}, Group ID: {}",
                    processedOffset.offsetHolder(), processedOffset.consumerGroupInfo().groupId(), throwable);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param row To be converted
     * @return {@link ProcessedOffset}
     */
    @Override
    protected ProcessedOffset convertRowToModel(final Row row) {
        return new ProcessedOffset(consumerGroupService.get((Long) row.get(MQPROCESSEDOFFSET.CONSUMER_GROUP_ID)),
                topicService.get((Long) row.get(MQPROCESSEDOFFSET.TOPIC_ID)),
                new OffsetHolder(row.get(MQPROCESSEDOFFSET.TOPIC_ID), row.get(MQPROCESSEDOFFSET.PARTITION), row.get(MQPROCESSEDOFFSET.OFFSET)));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    protected Table getTable() {
        return Table.get(MQPROCESSEDOFFSET.TABLE);
    }

    /**
     * Builds the {@link WhereClause} to query the processed offset table
     *
     * @param table     Represents the processed offset table
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic
     * @param partition Represents the partition
     * @return {@link WhereClause} to query the processed offset table
     */
    private WhereClause buildWhereClause(final Table table, final String groupId, final String topic, final int partition) {
        final ConsumerGroupInfo consumerGroupInfo = consumerGroupService.get(groupId);
        final TopicInfo topicInfo = topicService.get(topic);

        return new WhereClause(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.CONSUMER_GROUP_ID), consumerGroupInfo.id()))
                .and(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.TOPIC_ID), topicInfo.id()))
                .and(Criteria.eq(table.getColumn(MQPROCESSEDOFFSET.PARTITION), partition));
    }
}