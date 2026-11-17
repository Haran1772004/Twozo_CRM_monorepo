package com.tlc.crm.core.api.deal.manager;

import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.list.model.Statistics.Type;
import com.tlc.crm.entity.list.model.Statistics;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.security.context.SecureTaskProcessor;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Manages the {@link Deal} statistics related DB actions
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ListStatisticsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListStatisticsManager.class);
    private static final String DEAL_COUNT = "DEAL_COUNT";

    private final DealManager dealManager;
    private final FieldService fieldService;

    private static class Instance {
        private static final ListStatisticsManager INSTANCE = new ListStatisticsManager();
    }

    private ListStatisticsManager() {
        dealManager = DealManager.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * Gets the {@link ListStatisticsManager} Instance
     *
     * @return the {@link ListStatisticsManager}
     */
    public static ListStatisticsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Retrieves the count of open deals and without any deals based on the provided contact IDs.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param derivedQuery The SelectQuery used to filter and fetch relevant contact data.
     * @return Deals statistics of the contacts.
     */
    public Statistics getContactStatistics(final long orgId, final SelectQuery derivedQuery) {
        final Statistics statistics = new Statistics();
        final Table dealTable = dealManager.getTable();
        final Table dealContactTable = dealManager.getParticipantTable();
        final Table contactTable = dealManager.getContactTable();
        final Column participantContactIdColumn = dealContactTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final long time = System.currentTimeMillis();
        final Collection<StatQueryMapping> multiQueries = new ArrayList<>();
        final Column column = dealContactTable.getColumn(MDOPARTICIPANT.CONTACT_ID, DEAL_COUNT).countWithDistinct();
        final Table contactActivityTable = SourceActivityManager.getInstance().getContactActivityTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final CountQuery withoutDealsQuery = CountQuery.get(contactTable);
        final SelectQuery withOpenDealsQuery = SelectQuery.get(dealTable);
        final SelectQuery participantQuery = SelectQuery.get(dealContactTable);
        final SelectQuery contactActivityQuery = SelectQuery.get(contactActivityTable);
        final CountQuery untouchedQuery = CountQuery.get(contactTable);

        participantQuery.addSelectClause(participantContactIdColumn);
        contactActivityQuery.addSelectClause(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID));

        withOpenDealsQuery.addSelectClause(column);
        withOpenDealsQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealContactTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        withOpenDealsQuery.setWhereClause(new WhereClause(Criteria.in(participantContactIdColumn, derivedQuery))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), State.OPEN.getId())));
        multiQueries.add(new StatQueryMapping(Type.WITH_OPEN_DEALS, withOpenDealsQuery));

        withoutDealsQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, derivedQuery)).and(Criteria.notIn(contactPkColumn, participantQuery)));
        multiQueries.add(new StatQueryMapping(Type.WITHOUT_DEALS, withoutDealsQuery));

        untouchedQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, derivedQuery)).and(Criteria.notIn(contactPkColumn, contactActivityQuery)));
        multiQueries.add(new StatQueryMapping(Type.UN_TOUCHED, untouchedQuery));

        SecureTaskProcessor.executeInParallel(multiQueries, (statQueryMapping) -> {
            final MultiQuery multiQuery = statQueryMapping.multiQuery();

            if (multiQuery instanceof SelectQuery selectQuery) {
                final List<Map<String, Object>> data = secureReadableOrgDataStore(orgId).getData(selectQuery);

                statistics.addStatistic(Type.WITH_OPEN_DEALS, data.isEmpty() ? 0 : ((Number) data.get(0).get(DEAL_COUNT)).intValue());
            } else if (multiQuery instanceof CountQuery countQuery) {
                final int count = (int) secureReadableOrgDataStore(orgId).get(countQuery);

                switch (statQueryMapping.type()) {
                    case UN_TOUCHED -> statistics.addStatistic(Type.UN_TOUCHED, count);
                    default -> statistics.addStatistic(Type.WITHOUT_DEALS, count);
                }
            }
        });

        LOGGER.debug("List Action: Contact summary multiple queries fetched in {}ms, Org ID: {}", System.currentTimeMillis() - time, orgId);
        return statistics;
    }

    /**
     * <p>
     * Gets the deal statistics of the company ids.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param derivedQuery The SelectQuery used to filter and fetch relevant company data.
     * @return Deal statistics of the companies.
     */
    public Statistics getCompanyStatistics(final long orgId, final SelectQuery derivedQuery) {
        final Statistics statistics = new Statistics();
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final Table dealTable = dealManager.getTable();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final Long companyFieldId = fields.get(DealFields.COMPANY).id();
        final Column companyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
        final Column dealPkColumn = dealTable.getPKColumn();
        final long time = System.currentTimeMillis();
        final Collection<StatQueryMapping> multiQueries = new ArrayList<>();
        final Table companyTable = CompanyManager.getInstance().getTable();
        final Column column = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID, DEAL_COUNT).countWithDistinct();
        final Column companyPkColumn = companyTable.getPKColumn();
        final SelectQuery withOpenDealsQuery = SelectQuery.get(dealTable);
        final SelectQuery withWonDealsQuery = SelectQuery.get(dealTable);
        final CountQuery withoutDealsQuery = CountQuery.get(companyTable);
        final SelectQuery dealCompanyQuery = SelectQuery.get(dealCompanyTable);

        dealCompanyQuery.addSelectClause(companyIdColumn);

        withOpenDealsQuery.addSelectClause(column);
        withOpenDealsQuery.addJoinClause(new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
        withOpenDealsQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))
                .and(Criteria.in(companyIdColumn, derivedQuery))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), State.OPEN.getId())));
        multiQueries.add(new StatQueryMapping(Type.WITH_OPEN_DEALS, withOpenDealsQuery));

        withWonDealsQuery.addSelectClause(column);
        withWonDealsQuery.addJoinClause(new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
        withWonDealsQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))
                .and(Criteria.in(companyIdColumn, derivedQuery))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), State.WON.getId())));
        multiQueries.add(new StatQueryMapping(Type.WITH_WON_DEALS, withWonDealsQuery));

        withoutDealsQuery.setWhereClause(new WhereClause(Criteria.in(companyPkColumn, derivedQuery)).and(Criteria.notIn(companyPkColumn, dealCompanyQuery)));
        multiQueries.add(new StatQueryMapping(Type.WITHOUT_DEALS, withoutDealsQuery));

        SecureTaskProcessor.executeInParallel(multiQueries, (statQueryMapping) -> {
            final MultiQuery multiQuery = statQueryMapping.multiQuery();

            if (multiQuery instanceof SelectQuery selectQuery) {
                final List<Map<String, Object>> data = secureReadableOrgDataStore(orgId).getData(selectQuery);

                statistics.addStatistic(statQueryMapping.type(), data.isEmpty() ? 0 : ((Number) data.get(0).get(DEAL_COUNT)).intValue());
            } else if (multiQuery instanceof CountQuery countQuery) {
                final int count = (int) secureReadableOrgDataStore(orgId).get(countQuery);

                statistics.addStatistic(Type.WITHOUT_DEALS, count);
            }
        });

        LOGGER.debug("List Action: Company summary multiple queries fetched in {}ms, Org ID: {}", System.currentTimeMillis() - time, orgId);
        return statistics;
    }

    /**
     * <p>
     * Gets the count of deal statistics.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param derivedQuery The SelectQuery used to filter and fetch relevant deal data.
     * @return Deal statistics.
     */
    public Statistics getDealStatistics(final long orgId, final SelectQuery derivedQuery) {
        final Statistics statistics = new Statistics();
        final Table dealTable = dealManager.getTable();
        final Column stateColumn = dealTable.getColumn(MDODEAL.STATE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column countColumn = dealTable.getColumn(MDODEAL.ID, DealFields.COUNT).countWithDistinct();

        selectQuery.addSelectClause(List.of(stateColumn, countColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), derivedQuery)));
        selectQuery.addGroupByClause(new GroupByClause(stateColumn));
        final long time = System.currentTimeMillis();
        final List<Map<String, Object>> data = secureReadableOrgDataStore(orgId).getData(selectQuery);

        LOGGER.debug("List Action - [DEAL_LIST]: Summary statistics fetched in {}ms, Org ID: {}", System.currentTimeMillis() - time, orgId);

        statistics.addStatistic(Type.OPEN_DEALS, 0);
        statistics.addStatistic(Type.WON_DEALS, 0);
        statistics.addStatistic(Type.LOST_DEALS, 0);

        data.forEach(row -> {
            final int stateId = (int) row.get(MDODEAL.STATE);
            final int count = ((Long) row.get(DealFields.COUNT)).intValue();
            final State dealState = Deal.State.getStateById(stateId);

            switch (dealState) {
                case OPEN -> statistics.addStatistic(Type.OPEN_DEALS, count);
                case WON -> statistics.addStatistic(Type.WON_DEALS, count);
                case LOST -> statistics.addStatistic(Type.LOST_DEALS, count);
            }
        });

        return statistics;
    }

    /**
     * Computes and returns activity-related statistics for a given organization
     * based on the provided derived query.
     *
     * @param orgId        The ID of the organization for which statistics are to be computed.
     * @param derivedQuery The SelectQuery used to filter and fetch relevant activity data.
     * @return The {@code Statistics} containing computed activity metrics.
     */
    public Statistics getActivityStatistics(final long orgId, final SelectQuery derivedQuery) {
        final Statistics statistics = new Statistics();
        final Table activityTable = SourceActivityManager.getInstance().getTable();
        final Column statusColumn = activityTable.getColumn(OMACACTIVITY.STATUS);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final String activityCountKey = ActivityFields.COUNT.getName();
        final Column countColumn = activityTable.getColumn(OMACACTIVITY.ID, activityCountKey).countWithDistinct();

        selectQuery.addSelectClause(List.of(statusColumn, countColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityTable.getPKColumn(), derivedQuery)));
        selectQuery.addGroupByClause(new GroupByClause(statusColumn));
        final long time = System.currentTimeMillis();
        final List<Map<String, Object>> data = secureReadableOrgDataStore(orgId).getData(selectQuery);

        LOGGER.debug("List Action - [ACTIVITY_LIST]: Summary statistics fetched in {}ms, Org ID: {}", System.currentTimeMillis() - time, orgId);

        statistics.addStatistic(Type.UPCOMING, 0);
        statistics.addStatistic(Type.COMPLETED, 0);
        statistics.addStatistic(Type.OVERDUE, 0);

        data.forEach(row -> {
            final int status = (int) row.get(OMACACTIVITY.STATUS);
            final int count = ((Long) row.get(activityCountKey)).intValue();
            final Status activityStatus = Status.get(status);

            switch (activityStatus) {
                case UPCOMING -> statistics.addStatistic(Type.UPCOMING, count);
                case COMPLETED -> statistics.addStatistic(Type.COMPLETED, count);
                case OVERDUE -> statistics.addStatistic(Type.OVERDUE, count);
            }
        });

        return statistics;
    }

    /**
     * Gets the secure readable org data store
     *
     * @param orgId Represents the organization ID
     * @return the secure readable org data store
     */
    private ReadableDataStore secureReadableOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId);
    }

    /**
     * A record representing a statistical type paired with its corresponding query logic.
     *
     * @param type       The category or type of the statistic.
     * @param multiQuery The associated MultiQuery used to fetch relevant data.
     */
    private record StatQueryMapping(Type type, MultiQuery multiQuery) {
    }
}