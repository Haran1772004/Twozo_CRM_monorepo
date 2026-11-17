package com.tlc.iam.server.internal.session;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.SessionFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.server.internal.api.manager.AbstractIamManager;
import com.tlc.iam.server.internal.status.IamServerErrorCodes;
import com.tlc.iam.server.sql.resource.MIAMOSESSION;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A manager that manages the {@link UserSession} related DB actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public class SessionManager extends AbstractIamManager<UserSession> {

    private final ReadableDataStore readableDataStore;

    private static class Instance {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
        final SQLService sqlService = SqlServiceProvider.get();

        readableDataStore = sqlService.getAdminDataStore();
    }

    public static SessionManager getInstance() {
        return SessionManager.Instance.INSTANCE;
    }

    @Override
    protected Table getTable() {
        return Table.get(MIAMOSESSION.TABLE);
    }

    /**
     * Fetches the session information by its id
     *
     * @param id Session identifier
     * @return {@link UserSession}
     */
    @Override
    public UserSession get(final Long id) {
        Objects.requireNonNull(id);
        final Row sessionRow = readableDataStore.get(getTable(), id);

        return Objects.isNull(sessionRow) ? null : convertRowToSession(sessionRow);
    }

    /**
     * Fetches the session details
     *
     * @param orgId Organization id
     * @param id    User id
     * @return {@link UserSession}
     */
    public UserSession get(final Long orgId, final Long id) {
        final Row sessionRow = orgDataStore(orgId).get(getTable(), id);

        return Objects.isNull(sessionRow) ? null : convertRowToSession(sessionRow);
    }

    /**
     * Fetches the number of non-expired sessions available for a user
     *
     * @param orgId  Organization id
     * @param userId User id
     * @return Number of non-expired sessions
     */
    public long count(final Long orgId, final Long userId) {
        final Table sessionTable = getTable();
        final CountQuery countQuery = CountQuery.get(sessionTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(sessionTable.getColumn(MIAMOSESSION.USER_ID), userId));

        whereClause.and(Criteria.gt(sessionTable.getColumn(MIAMOSESSION.CREATED_TIME), Instant.now().minus(8, ChronoUnit.HOURS).toEpochMilli()));
        countQuery.setWhereClause(whereClause);
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Persists the session
     *
     * @param userSession Represents the session information of a user
     */
    @Override
    public void save(UserSession userSession) {
        final DataContainer dataContainer = DataContainer.create();

        loadSessionIntoContainer(true, userSession, null, dataContainer);
        orgDataStore(userSession.orgId()).commitChanges(dataContainer);
    }

    /**
     * Updates the given sessions
     *
     * @param userSessions Collection of Sessions
     * @return Failed records and updated records
     */
    @Override
    public Map<String, Object> update(Collection<UserSession> userSessions) {
        final UserSession firstUserSession = userSessions.stream().findFirst().get();
        final Map<String, Object> response = new HashMap<>();
        final List<Map<String, Object>> failedEntities = new ArrayList<>();
        final List<Long> updatedRecords = new ArrayList<>();
        final Table sessionTable = getTable();
        final DataContainer sourceContainer = orgDataStore(firstUserSession.orgId()).get(sessionTable, new WhereClause(Criteria.in(sessionTable.getColumn(MIAMOSESSION.ID),
                userSessions.stream().map(UserSession::sessionId).filter(Objects::nonNull).toList())));

        for (final UserSession userSession : userSessions) {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadSessionIntoContainer(false, userSession, sourceContainer, targetContainer);
                orgDataStore(userSession.orgId()).commitChanges(targetContainer);
                updatedRecords.add(userSession.id());
            } catch (final ErrorCode errorCode) {
                final Map<String, Object> entity = new HashMap<>();

                entity.put(SessionFields.ID, userSession.sessionId());
                entity.put(IamFields.REASON, "i18n.iam.accounts.session.update.failed");
                failedEntities.add(entity);
            }
        }

        response.put(IamFields.UPDATED_RECORDS, updatedRecords);
        response.put(IamFields.FAILED_RECORDS, failedEntities);
        return response;
    }

    /**
     * Loads the session details into target container
     *
     * @param isCreateEvent   A flag that indicates whether an action is related to create event or update event
     * @param userSession         An entity that holds the session information
     * @param sourceContainer A container that holds the details of existing session
     * @param targetContainer A container that holds the updated details of session
     */
    private void loadSessionIntoContainer(final boolean isCreateEvent, final UserSession userSession, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table sessionTable = getTable();
        final Row sessionRow = isCreateEvent ? new Row(sessionTable) : sourceContainer.getRow(sessionTable, userSession.id());
        final DateTime currentDateTime = new DateTime(Instant.now().toEpochMilli());

        if (!isCreateEvent && Objects.isNull(sessionRow)) {
            throw ErrorCode.get(IamServerErrorCodes.SESSION_NOT_FOUND);
        }

        sessionRow.set(MIAMOSESSION.USER_ID, userSession.getUserId());
        sessionRow.set(MIAMOSESSION.ACCESS_DEVICE, userSession.getDevice());
        sessionRow.set(MIAMOSESSION.BROWSER, userSession.getBrowser());
        sessionRow.set(MIAMOSESSION.OS, userSession.getOs());

        if (sessionRow.isNewRow()) {
            sessionRow.set(MIAMOSESSION.CREATED_TIME, currentDateTime);
            sessionRow.setOrgId(userSession.orgId());
            userSession.setId(sessionRow.getPKValue());
            userSession.setCreatedTime(currentDateTime);
        }

        targetContainer.updateRow(sessionRow);
    }

    /**
     * Converts the row into {@link UserSession}
     *
     * @param row Holds session information
     * @return {@link UserSession}
     */
    private UserSession convertRowToSession(final Row row) {
        final UserSession userSession = new UserSession(row.getOrgId(), row.get(MIAMOSESSION.USER_ID));

        userSession.setId(row.getPKValue());
        userSession.setCreatedTime(row.get(MIAMOSESSION.CREATED_TIME));
        userSession.setDevice(row.get(MIAMOSESSION.ACCESS_DEVICE));
        userSession.setOs(row.get(MIAMOSESSION.OS));
        userSession.setBrowser(row.get(MIAMOSESSION.BROWSER));
        //TODO store country on session

        return userSession;
    }

    /**
     * Terminates the given session
     *
     * @param userSession Represents the session information
     */
    public void terminateSession(final UserSession userSession) {
        delete(userSession.id());
    }

    /**
     * Fetches the active sessions available for a user
     *
     * @param orgId  Organization id
     * @param userId User id
     * @return Active sessions
     */
    public Collection<UserSession> getUserSessions(final Long orgId, final Long userId) {
        final Table sessionTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(sessionTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(sessionTable.getColumn(MIAMOSESSION.USER_ID), userId));

        whereClause.and(Criteria.gt(sessionTable.getColumn(MIAMOSESSION.CREATED_TIME), Instant.now().minus(8, ChronoUnit.HOURS).toEpochMilli()));
        selectQuery.setWhereClause(whereClause);

        final DataContainer sessionsContainer = orgDataStore(orgId).get(selectQuery);

        return sessionsContainer.getRows(sessionTable).map(this::convertRowToSession).toList();
    }

    /**
     * Fetches the last sessions available for a users
     *
     * @param orgId   Represents an organization id
     * @param userIds Represents the {@link Collection} of user ids
     * @return the {@link Collection} of {@link UserSession}
     */
    public Collection<UserSession> getUsersLastSession(final Long orgId, final Collection<Long> userIds) {
        final Table sessionTable = getTable();
        final Column sessionTablePkColumn = sessionTable.getPKColumn();
        final Column sessionUserIdColumn = sessionTable.getColumn(MIAMOSESSION.USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(sessionTable);
        final SelectQuery innerQuery = SelectQuery.get(sessionTable);

        innerQuery.addSelectClause(sessionTablePkColumn.max());
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(sessionTable.getColumn(MIAMOSESSION.ORG_ID), orgId))
                .and(Criteria.in(sessionUserIdColumn, userIds)));
        innerQuery.addGroupByClause(new GroupByClause(sessionUserIdColumn));

        selectQuery.addSelectClause(List.of(sessionUserIdColumn, sessionTable.getColumn(MIAMOSESSION.CREATED_TIME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(sessionTablePkColumn, innerQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(sessionTable).map(this::convertRowToSession).toList();
    }

    /**
     * Fetches the expired sessions count
     *
     * @return Expired sessions count
     */
    public long fetchExpiredSessionsCount() {
        final Table sessionTable = getTable();
        final CountQuery countQuery = CountQuery.get(sessionTable);
        final WhereClause whereClause = new WhereClause(Criteria.lt(sessionTable.getColumn(MIAMOSESSION.CREATED_TIME), Instant.now().minus(8, ChronoUnit.HOURS).toEpochMilli()));

        countQuery.setWhereClause(whereClause);

        return readableDataStore().get(countQuery);
    }

    /**
     * Fetches the expired session ids
     *
     * @return Expired session ids
     */
    public Collection<Long> fetchExpiredSessionIds(final int maxNumberOfRecords) {
        final Table sessionTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(sessionTable);
        final WhereClause whereClause = new WhereClause(Criteria.lt(sessionTable.getColumn(MIAMOSESSION.CREATED_TIME), Instant.now().minus(8, ChronoUnit.HOURS).toEpochMilli()));

        selectQuery.addSelectClause(sessionTable.getPKColumn());
        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(new LimitClause(maxNumberOfRecords));
        selectQuery.addOrderByClause(new OrderByClause(sessionTable.getColumn(MIAMOSESSION.CREATED_TIME)));

        return readableDataStore().get(selectQuery).getRows(sessionTable).map(Row::getPKValue).toList();
    }

}
