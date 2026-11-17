package com.tlc.crm.core.notification.email.timeline.impl;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.timeline.EmailTimelineManager;
import com.tlc.crm.core.sql.resource.MTLEMAILTIMELINE;

import com.tlc.crm.entity.timeline.model.EmailTimeline;
import com.tlc.crm.entity.timeline.model.EntityTimelines;

import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;

import com.tlc.email.common.model.type.Email;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Performs email - timeline DB related actions
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class EmailTimelineManagerImpl implements EmailTimelineManager {

    private final SQLService sqlService;

    private EmailTimelineManagerImpl() {
        sqlService = SqlServiceProvider.get();
    }

    private static class Instance {
        private static final EmailTimelineManagerImpl INSTANCE = new EmailTimelineManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailTimelineManagerImpl} Instance
     * </p>
     *
     * @return {@link EmailTimelineManagerImpl}
     */
    public static EmailTimelineManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table} of email timeline
     */
    @Override
    public Table getTable() {
        return Table.get(MTLEMAILTIMELINE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization's id
     * @param ids   Holds {@link Collection} of ids
     * @return {@link Map} of {@link Long} timeline id  {@link EntityTimelines}
     */
    @Override
    public Map<Long, EntityTimelines> get(final long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = fetchDataContainer(orgId, ids);
        final Map<Long, EntityTimelines> entityTimelinesMap = new HashMap<>();

        if (Objects.nonNull(dataContainer)) {
            final Table conversationsTable = CoreMailManager.getInstance().getConversationsTable();
            final Stream<Row> emailTimelineRows = dataContainer.getRows(getTable());

            emailTimelineRows.forEach(emailTimelineRow -> {
                final EmailTimeline emailTimeline = new EmailTimeline();
                final EntityTimelines entityTimelines = new EntityTimelines();
                final Long conversationId = emailTimelineRow.get(MTLEMAILTIMELINE.CONVERSATION_ID);
                final Row conversationRow = dataContainer.getRow(conversationsTable, new WhereClause(Criteria.eq(
                        conversationsTable.getPKColumn(), conversationId)));

                if (Objects.nonNull(conversationRow)) {
                    emailTimeline.setContent(conversationRow.get(conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT)));
                    emailTimeline.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
                    emailTimeline.setUserId(conversationRow.get(OMEMCONVERSATIONS.USER_ID));
                    emailTimeline.setOrgId(conversationRow.get(OMEMCONVERSATIONS.ORG_ID));
                    emailTimeline.setMessageCategory(Email.MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));
                    emailTimeline.setId(conversationId);
                    entityTimelines.set(emailTimeline);
                }

                entityTimelinesMap.put(emailTimelineRow.get(MTLEMAILTIMELINE.TIMELINE_ID), entityTimelines);
            });
        }

        return entityTimelinesMap;
    }

    /**
     * <p>
     * Gets a container holding email related details
     * </p>
     *
     * @param orgId Represents an organization's id
     * @param ids   Holds {@link Collection} of timeline ids
     * @return {@link DataContainer} holding email related details
     */
    private DataContainer fetchDataContainer(final Long orgId, final Collection<Long> ids) {
        final Table conversationTable = CoreMailManager.getInstance().getConversationsTable();
        final Table emailTimelineTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(List.of(new JoinClause(conversationTable.getPKColumn(),
                emailTimelineTable.getColumn(MTLEMAILTIMELINE.CONVERSATION_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(emailTimelineTable.getColumn(MTLEMAILTIMELINE.TIMELINE_ID),
                emailTimelineTable.getColumn(MTLEMAILTIMELINE.CONVERSATION_ID), conversationTable.getColumn(OMEMCONVERSATIONS.ORG_ID),
                conversationTable.getColumn(OMEMCONVERSATIONS.SUBJECT), conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID),
                conversationTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), conversationTable.getColumn(OMEMCONVERSATIONS.CONTENT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailTimelineTable.getColumn(MTLEMAILTIMELINE.TIMELINE_ID), ids)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return sqlService.getOrgDataStore(orgId);
    }
}