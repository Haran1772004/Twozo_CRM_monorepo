package com.tlc.queue.internal.manager.topic;

import com.tlc.queue.internal.manager.AbstractManager;
import com.tlc.queue.internal.model.TopicInfo;
import com.tlc.queue.sql.resource.MQTOPIC;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the topic related DB operations
 *
 * @author sundar
 * @version 1.0
 */
class TopicManagerImpl extends AbstractManager<TopicInfo> implements TopicManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicManagerImpl.class);

    private TopicManagerImpl() {
    }

    private static class Instance {
        private static final TopicManager TOPIC_MANAGER = new TopicManagerImpl();
    }

    /**
     * Gets the {@link TopicManager}
     *
     * @return the {@link TopicManager}
     */
    static TopicManager getInstance() {
        return Instance.TOPIC_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param topicNames Which contains the topic names to store
     */
    @Override
    public void createIfNotExist(final Collection<String> topicNames) {

        if (Objects.isNull(topicNames) || topicNames.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final Collection<String> newTopicNames;
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(MQTOPIC.NAME);

        selectQuery.addSelectClause(nameColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(nameColumn, topicNames)));

        final DataContainer existingContainer = getReadableDataStore().get(selectQuery);

        if (existingContainer.size(table) == 0) {
            newTopicNames = topicNames;
        } else {
            final Collection<String> storedTopics = getTopicNames(topicNames)
                    .map(row -> (String) row.get(MQTOPIC.NAME))
                    .toList();

            newTopicNames = new HashSet<>();

            for (final String topicName : topicNames) {

                if (!storedTopics.contains(topicName)) {
                    newTopicNames.add(topicName);
                }
            }
        }

        if (!newTopicNames.isEmpty()) {
            final DataContainer dataContainer = DataContainer.create();

            for (final String topicName : newTopicNames) {
                final Row row = new Row(table);

                row.set(MQTOPIC.NAME, topicName);
                dataContainer.addNewRow(row);
            }
            getWritableDataStore().commitChanges(dataContainer);
            LOGGER.info("Queue: Topics are created on DB, Topics: {}", newTopicNames);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of all topics
     */
    @Override
    public Collection<TopicInfo> getAll() {
        final Table table = getTable();

        return getReadableDataStore().get(table, new WhereClause(Criteria.notEq(table.getPKColumn(), null)))
                .getRows(table).map(this::convertRowToModel).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param row To be converted
     * @return {@link TopicInfo}
     */
    @Override
    protected TopicInfo convertRowToModel(final Row row) {
        return new TopicInfo(row.getPKValue(), row.get(MQTOPIC.NAME));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    @Override
    protected Table getTable() {
        return Table.get(MQTOPIC.TABLE);
    }

    /**
     * Gets the topic rows which contains the topic names
     *
     * @param topicNames Represents the topic names to fetch
     * @return the topic rows
     */
    private Stream<Row> getTopicNames(final Collection<String> topicNames) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(MQTOPIC.NAME);

        selectQuery.addSelectClause(nameColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(nameColumn, topicNames)));

        return getReadableDataStore().get(selectQuery).getRows(table);
    }
}