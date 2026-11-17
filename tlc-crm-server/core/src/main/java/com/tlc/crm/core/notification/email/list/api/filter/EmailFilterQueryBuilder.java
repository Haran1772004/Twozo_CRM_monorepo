package com.tlc.crm.core.notification.email.list.api.filter;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.list.api.list.EmailQueryBuilder;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterInterval;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.LinkedCategory;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKER;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKERLOG;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMRECENTTEAMINBOXCONVERSATION;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Builds the query for specifying filtering conditions based on an {@link EmailListFilter}.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
class EmailFilterQueryBuilder extends EmailQueryBuilder {

    private static final int INTERVAL = 1;

    private EmailFilterQueryBuilder() {
        super();
    }

    private static class Instance {
        private static final EmailFilterQueryBuilder INSTANCE = new EmailFilterQueryBuilder();
    }

    /**
     * Gets the {@link EmailFilterQueryBuilder} Instance
     *
     * @return {@link EmailFilterQueryBuilder}
     */
    static EmailFilterQueryBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the tracking table
     *
     * @return the {@link Table} of tracking
     */
    private Table getTrackingTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TABLE);
    }

    /**
     * Gets the tracker history table
     *
     * @return the {@link Table} of tracker history
     */
    private Table getTrackerLogTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TABLE);
    }

    /**
     * Gets the deal table
     *
     * @return the {@link Table} of deal
     */
    private Table getDealTable() {
        return Table.get(MDODEAL.TABLE);
    }

    /**
     * <p>
     * Gets the {@link List} of {@link JoinClause} for threads, based on the provided {@link EmailListFilter}.
     * </p>
     *
     * @param folder                  Represents the {@link Folder}
     * @param emailFilter             Represents the {@link EmailListFilter}.
     * @param recentConversationTable Represents the {@link Table} of recent conversations.
     * @return {@link List} of {@link JoinClause}.
     */
    public List<JoinClause> getThreadJoinClauses(final Folder folder, final EmailListFilter emailFilter, final Table recentConversationTable) {
        return getThreadJoinClauses(emailFilter, recentConversationTable, folder, false);
    }

    /**
     * Builds the {@link List} of {@link JoinClause} for threads, based on the provided parameters.
     *
     * @param emailFilter        Represents the {@link EmailListFilter}.
     * @param baseTable          Represents the {@link Table} for the operation (can be null).
     * @param folder             Represents the {@link Folder} (used only for count-based join clauses, can be null).
     * @param buildForCountQuery Indicates whether to build join clauses for count-based filtering.
     * @return {@link List} of {@link JoinClause}.
     */
    private List<JoinClause> getThreadJoinClauses(final EmailListFilter emailFilter, final Table baseTable,
                                                  final Folder folder, final boolean buildForCountQuery) {
        final Table threadTable = getThreadTable();
        final Table dealEmailTable = getDealEmailTable();

        final LinkedCategory linkedCategory = emailFilter.getLinkedCategory();
        final boolean hasNoLinkedCategory = Objects.isNull(linkedCategory);
        final List<JoinClause> joinClauses = new ArrayList<>(
                buildForCountQuery && hasNoLinkedCategory ? getThreadsCountJoinClauses(baseTable, folder) : getThreadsJoinClauses(folder, baseTable, false)
        );

        loadThreadJoinClauses(emailFilter, joinClauses);
        loadTeamInboxJoinClauses(baseTable, emailFilter, joinClauses);
        loadSyncEmailJoinClauses(Objects.isNull(baseTable) ? getConversationsTable() : baseTable, emailFilter, joinClauses);

        if (!buildForCountQuery && (hasNoLinkedCategory || LinkedCategory.ATTACHMENTS == linkedCategory)) {
            joinClauses.add(new JoinClause(threadTable.getPKColumn(), dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), JoinClause.JoinType.LEFT));
        }

        return joinClauses;
    }

    /**
     * <p>
     * Adds team inbox {@link JoinClause} instances to the given list
     * </p>
     *
     * @param baseTable       The base {@link Table} from which the query originates.
     * @param emailListFilter The {@link EmailListFilter} containing filter attributes.
     * @param joinClauses     The list of {@link JoinClause} objects to which new joins will be added.
     */
    private void loadTeamInboxJoinClauses(final Table baseTable, final EmailListFilter emailListFilter,
                                          final List<JoinClause> joinClauses) {
        if (emailListFilter.getFilterAttribute().containsKey(FilterAttribute.TEAM_INBOX)) {
            final Table teamInboxTable = getTeamInboxTable();

            joinClauses.add(new JoinClause(baseTable.getColumn(CoreEmailFields.TEAM_INBOX_ID.name()), teamInboxTable.getPKColumn()));
        }
    }

    /**
     * <p>
     * Loads the join clause for the synced email conversation table into the provided list of join clauses,
     * if the {@link EmailListFilter} contains the {@link FilterAttribute#SYNC_EMAILS} filter attribute.
     * </p>
     *
     * @param baseTable       Represents the base table from which the join is initiated using the conversation id
     * @param emailListFilter Represents the email list filter used to check if the sync emails filter attribute is present
     * @param joinClauses     Represents the list of join clauses to which the synced email conversation join is added
     */
    private void loadSyncEmailJoinClauses(final Table baseTable, final EmailListFilter emailListFilter,
                                          final List<JoinClause> joinClauses) {
        if (emailListFilter.getFilterAttribute().containsKey(FilterAttribute.SYNC_EMAILS)) {
            final Table syncEmailTable = getSyncedEmailConversationTable();

            joinClauses.add(new JoinClause(baseTable.getColumn(CoreEmailFields.CONVERSATION_ID.name()),
                    syncEmailTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)));
        }
    }

    /**
     * <p>
     * Adds additional {@link JoinClause} instances to the provided list based on the given {@link EmailListFilter}.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter} used to determine which join clauses to add.
     * @param joinClauses The list of {@link JoinClause} to which additional join clauses will be appended.
     */
    private void loadThreadJoinClauses(final EmailListFilter emailFilter, final List<JoinClause> joinClauses) {
        final Table conversationsTable = getConversationsTable();

        if (hasIntervalAttributes(emailFilter.getFolder().hasInterval(), emailFilter.getFilterAttribute())) {
            final Table trackingTable = getTrackingTable();

            joinClauses.addAll(List.of(
                    new JoinClause(conversationsTable.getPKColumn(), trackingTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID)),
                    new JoinClause(trackingTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.ID),
                            getTrackerLogTable().getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID)))
            );
        }

        joinClauses.addAll(buildLinkedCategoryJoinClauses(emailFilter));
    }

    /**
     * Builds the {@link WhereClause} based on {@link EmailListFilter}
     *
     * @param emailFilter Represents the {@link EmailListFilter
     * @return {@link WhereClause}
     */
    WhereClause buildThreadWhereClause(final EmailListFilter emailFilter) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Folder folder = emailFilter.getFolder();
        final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();
        final BinaryOperator<WhereClause> whereClauseCombiner = (criteriaOne, criteriaTwo) ->
                WhereClauseUtil.connect(criteriaOne, criteriaTwo, Connector.AND);

        WhereClause whereClause = null;

        if (hasIntervalAttributes(folder.hasInterval(), filterAttributes)) {
            whereClause = whereClauseCombiner.apply(buildDateIntervalWhereClause(emailFilter), null);
        }

        if (folder.hasUnreadAttribute() && filterAttributes.containsKey(FilterAttribute.UNREAD)) {
            whereClause = whereClauseCombiner.apply(
                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ), false)),
                    whereClause
            );
        }
        final Collection<Long> syncEmails = emailFilter.getSyncEmails();

        if (Objects.nonNull(syncEmails) && !syncEmails.isEmpty()) {
            final Table syncEmailConversationTable = getSyncedEmailConversationTable();

            whereClause = whereClauseCombiner.apply(
                    whereClause,
                    new WhereClause(Criteria.in(syncEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID), syncEmails))
            );
        }
        final Collection<TeamInbox> teamInboxes = emailFilter.getTeamInboxes();

        if (Objects.nonNull(teamInboxes) && !teamInboxes.isEmpty()) {
            final Collection<Long> teamInboxIds = teamInboxes.stream().map(TeamInbox::id).collect(Collectors.toSet());

            whereClause = whereClauseCombiner.apply(
                    new WhereClause(Criteria.in(getTeamInboxRecentConversationTable().getColumn(OMEMRECENTTEAMINBOXCONVERSATION.TEAM_INBOX_ID), teamInboxIds)),
                    whereClause
            );
        }
        final LinkedCategory linkedCategory = emailFilter.getLinkedCategory();

        if (Objects.nonNull(linkedCategory)) {

            switch (LinkedCategory.valueOf(linkedCategory.getId())) {
                case LINKED_WITH_AN_OPEN_DEAL -> whereClause = whereClauseCombiner.apply(
                        new WhereClause(Criteria.eq(getDealTable().getColumn(MDODEAL.STATE), Deal.State.OPEN.getId())),
                        whereClause
                );
                case NOT_LINKED_WITH_A_DEAL -> whereClause = whereClauseCombiner.apply(
                        new WhereClause(Criteria.eq(getDealEmailTable().getPKColumn(), null)),
                        whereClause
                );
                case ATTACHMENTS -> whereClause = whereClauseCombiner.apply(
                        new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ATTACHMENTS), true)),
                        whereClause
                );
            }
        }

        return whereClause;
    }

    /**
     * <p>
     * Builds a list of selected columns based on the provided email filter criteria.
     * </p>
     *
     * @param emailFilter The filter object containing criteria to build the columns.
     * @return A list of columns matching the filter criteria.
     */
    List<Column> getFilteredColumns(final EmailListFilter emailFilter) {
        final List<Column> selectedColumns = new ArrayList<>();
        final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();
        final Folder folder = emailFilter.getFolder();

        if (hasIntervalAttributes(folder.hasInterval(), filterAttributes)) {
            final Table trackerTable = getTrackingTable();
            final Table trackerLogTable = getTrackerLogTable();

            selectedColumns.addAll(List.of(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID),
                    trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE),
                    trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID),
                    trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME)));
        }

        if (hasSyncEmailAttributes(filterAttributes)) {
            final Table syncedEmailConversationTable = getSyncedEmailConversationTable();

            selectedColumns.addAll(
                    List.of(
                            syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID),
                            syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)
                    )
            );
        }

        if (folder.hasUnreadAttribute() && filterAttributes.containsKey(FilterAttribute.UNREAD)) {
            selectedColumns.add(getConversationsTable().getColumn(OMEMCONVERSATIONS.IS_READ));
        }

        return selectedColumns;
    }

    /**
     * Checks whether the interval-related filter attributes, depending on the existence of an interval flag.
     *
     * @param hasInterval      Represents the {@link EmailListFilter}
     * @param filterAttributes Represents the filter attributes
     * @return true, if the interval exists
     */
    private boolean hasIntervalAttributes(final boolean hasInterval, final Map<FilterAttribute, FilterOption> filterAttributes) {
        return hasInterval && (filterAttributes.containsKey(FilterAttribute.OPENED_INTERVAL)
                || filterAttributes.containsKey(FilterAttribute.CLICKED_INTERVAL));
    }

    /**
     * <p>
     * Checks whether the given filter attributes map contains the {@link FilterAttribute#SYNC_EMAILS} attribute.
     * </p>
     *
     * @param filterAttributes Represents the map of filter attributes to be checked
     * @return {@code true} if the {@link FilterAttribute#SYNC_EMAILS} attribute is present, otherwise {@code false}
     */
    private boolean hasSyncEmailAttributes(final Map<FilterAttribute, FilterOption> filterAttributes) {
        return filterAttributes.containsKey(FilterAttribute.SYNC_EMAILS);
    }

    /**
     * Gets the {@link List} of {@link JoinClause} for {@link LinkedCategory} based on the provided email filter.
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return {@link List} of {@link JoinClause}
     */
    private List<JoinClause> buildLinkedCategoryJoinClauses(final EmailListFilter emailFilter) {
        final Table dealEmailTable = getDealEmailTable();
        final Table threadTable = getThreadTable();

        final List<JoinClause> joinClauses = new ArrayList<>();
        final LinkedCategory linkedCategory = emailFilter.getLinkedCategory();
        final Consumer<JoinClause.JoinType> joinClauseConsumer = joinType ->
                joinClauses.add(new JoinClause(threadTable.getPKColumn(), dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), joinType));

        if (Objects.nonNull(linkedCategory)) {

            switch (LinkedCategory.valueOf(linkedCategory.getId())) {
                case LINKED_WITH_A_DEAL -> joinClauseConsumer.accept(JoinClause.JoinType.INNER);
                case NOT_LINKED_WITH_A_DEAL -> joinClauseConsumer.accept(JoinClause.JoinType.LEFT);
                case LINKED_WITH_AN_OPEN_DEAL -> {
                    joinClauseConsumer.accept(JoinClause.JoinType.INNER);
                    joinClauses.add(new JoinClause(dealEmailTable.getColumn(OMEMDEALEMAIL.DEAL_ID), getDealTable().getPKColumn()));
                }
            }
        }

        return joinClauses;
    }

    /**
     * Builds the {@link WhereClause} condition based on the provided {@link EmailListFilter}.
     *
     * @param emailFilter Represents the email filter containing the criteria for filtering emails
     * @return {@link WhereClause} appended email filter condition
     */
    private WhereClause buildDateIntervalWhereClause(final EmailListFilter emailFilter) {
        final Table trackingTable = getTrackingTable();
        final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();

        final BiFunction<FilterAttribute, EmailTrackingDetail.Type, WhereClause> getTrackingCriteria = (filterAttribute, type) ->
                new WhereClause(buildCriteriaForTracking(filterAttributes, filterAttribute))
                        .and(Criteria.eq(trackingTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE), type.id));

        WhereClause whereClause = null;

        if (filterAttributes.containsKey(FilterAttribute.OPENED_INTERVAL)) {
            whereClause = getTrackingCriteria.apply(FilterAttribute.OPENED_INTERVAL, EmailTrackingDetail.Type.OPENED);
        }

        if (filterAttributes.containsKey(FilterAttribute.CLICKED_INTERVAL)) {
            whereClause = WhereClauseUtil.connect(
                    whereClause,
                    getTrackingCriteria.apply(FilterAttribute.CLICKED_INTERVAL, EmailTrackingDetail.Type.CLICKED),
                    Connector.OR
            );
        }

        return whereClause;
    }

    /**
     * Builds the {@link Criteria} for tracking
     *
     * @param filterAttributes Represents the filterAttributes which it maps the attribute with corresponding value
     * @param attribute        Represents the {@link FilterAttribute}
     * @return {@link Criteria}
     */
    private Criteria buildCriteriaForTracking(final Map<FilterAttribute, FilterOption> filterAttributes,
                                              final FilterAttribute attribute) {
        final Column timeColumn = getTrackerLogTable().getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME);
        final FilterInterval dateInterval = (FilterInterval) filterAttributes.get(attribute);
        final Interval<Long, Long> interval = getTimeRangeCriteria(dateInterval.getId());

        return FilterInterval.ANYTIME.equals(dateInterval) ? Criteria.le(timeColumn, interval.end())
                : Criteria.between(timeColumn, interval.start(), interval.end());
    }

    /**
     * Gets a criteria when representing the time range condition based on the provided time range id.
     *
     * @param rangeId Represents the id of {@link FilterInterval}
     * @return a criteria for the time range
     */
    private Interval<Long, Long> getTimeRangeCriteria(final int rangeId) {
        final TemporalService temporalService = TemporalServiceProvider.get();

        return switch (FilterInterval.valueOf(rangeId)) {
            case ANYTIME -> new Interval<>(null, temporalService.getEndTimeOfDay(TimezoneInfo.UTC));
            case LONGER_THAN_A_TIME -> {
                final Long startTime = temporalService.getStartTimeOfDay(TimezoneInfo.UTC);

                yield new Interval<>(temporalService.getPreviousDate(startTime, INTERVAL, DateInterval.DAYS), startTime);
            }
            case LONGER_THAN_A_WEEK -> {
                final Long utcTime = temporalService.getUtcDateTime();

                yield new Interval<>(temporalService.getPreviousDate(utcTime, INTERVAL, DateInterval.WEEKS), utcTime);
            }
            case LONGER_THAN_A_MONTH -> {
                final Long utcTime = temporalService.getUtcDateTime();

                yield new Interval<>(temporalService.getPreviousDate(utcTime, INTERVAL, DateInterval.MONTHS), utcTime);
            }
        };
    }

    /**
     * <p>
     * Builds a nested {@code WhereClause} based on the given email filter, table, and selectQuery.
     * </p>
     *
     * @param emailFilter the filter containing criteria for email lists, including interval attributes.
     * @param selectQuery the base selectQuery containing the current {@link WhereClause} and {@link JoinClause}.
     * @param start       Represent the start
     * @param limit       Represent the limit
     * @return an {@link Optional} containing the nested {@link WhereClause}.
     */
    public Optional<SelectQuery> buildConversationsSelectQuery(final EmailListFilter emailFilter, final SelectQuery selectQuery,
                                                               final int start, final int limit) {

        if (hasIntervalAttributes(emailFilter.getFolder().hasInterval(), emailFilter.getFilterAttribute())) {
            final Table conversationsTable = getConversationsTable();
            final Table baseTable = selectQuery.getBaseTable();
            final SelectQuery conversationsSelectQuery = SelectQuery.get(baseTable);

            conversationsSelectQuery.addSelectClause(List.of(conversationsTable.getPKColumn().distinct(),
                    conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME)));
            conversationsSelectQuery.setWhereClause(selectQuery.getWhereClause());
            conversationsSelectQuery.addJoinClause(selectQuery.getJoinClause());
            conversationsSelectQuery.setLimitClause(new LimitClause(start, limit));
            conversationsSelectQuery.addOrderByClause(getOrderByClauses(baseTable));

            return Optional.of(conversationsSelectQuery);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Builds a filter query based on the given email filter, table, and folder context.
     * </p>
     *
     * @param emailFilter the filter containing criteria for email lists, including interval attributes.
     * @param table       the optional table object from which columns may be queried.
     * @param whereClause the base {@link WhereClause} to apply additional filtering conditions.
     * @param folder      the {@link Folder} representing the context of the email filter (e.g., Inbox, Sent).
     * @return a {@link MultiQuery} representing the constructed filter query, either as a select or count query.
     */
    public MultiQuery buildEmailFilterQuery(final EmailListFilter emailFilter, final Table table, final WhereClause whereClause,
                                            final Folder folder, final boolean isCountQuery) {
        final Table conversationsTable = getConversationsTable();
        final Table primaryTable = Objects.isNull(table) ? conversationsTable : table;

        MultiQuery multiQuery;

        if (hasIntervalAttributes(emailFilter.getFolder().hasInterval(), emailFilter.getFilterAttribute()) || !isCountQuery) {
            multiQuery = SelectQuery.get(primaryTable);
        } else {
            multiQuery = CountQuery.get(primaryTable);
        }
        multiQuery.setWhereClause(whereClause);
        multiQuery.addJoinClause(getThreadJoinClauses(emailFilter, table, folder, true));

        return multiQuery;
    }
}