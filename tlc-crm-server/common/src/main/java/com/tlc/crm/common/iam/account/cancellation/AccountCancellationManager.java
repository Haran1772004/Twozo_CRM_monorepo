package com.tlc.crm.common.iam.account.cancellation;

import com.tlc.crm.common.iam.api.model.account.cancellation.AccountCancellationEntry;
import com.tlc.crm.common.iam.api.model.account.cancellation.Survey;
import com.tlc.crm.common.sql.resource.OMORGCANCELLATION;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Objects;

/**
 * <p>
 * Manages the {@link AccountCancellationEntry} related database actions
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class AccountCancellationManager extends AbstractCancellationManager<AccountCancellationEntry> {

    private AccountCancellationManager() {
    }

    private static class Instance {
        private static final AccountCancellationManager ACCOUNT_CANCELLATION_MANAGER = new AccountCancellationManager();
    }

    /**
     * <p>
     * Gets the {@link AccountCancellationManager} instance
     * </p>
     *
     * @return The instance of {@link AccountCancellationManager}
     */
    public static AccountCancellationManager getInstance() {
        return Instance.ACCOUNT_CANCELLATION_MANAGER;
    }

    /**
     * <p>
     * Gets the {@link Table}
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMORGCANCELLATION.TABLE);
    }

    /**
     * <p>
     * Processes the account cancellation
     * </p>
     *
     * @param accountCancellationEntry The {@link AccountCancellationEntry}
     */
    @Override
    public void cancelAccount(final AccountCancellationEntry accountCancellationEntry) {
        final Table table = getTable();
        final Row row = new Row(table);
        final Long orgId = accountCancellationEntry.getOrgId();
        final Short reasonId = accountCancellationEntry.getReason().getId();

        row.setOrgId(orgId);
        row.set(OMORGCANCELLATION.REASON_ID, reasonId);
        row.set(OMORGCANCELLATION.RETENTION_END_TIME, accountCancellationEntry.getRetentionEndTime());

        if (Objects.equals(AccountCancellationEntry.ReasonType.OTHER_REASONS.getId(), reasonId)) {
            row.set(OMORGCANCELLATION.OTHER_REASON, accountCancellationEntry.getOtherReason());
        }
        orgDataStore(orgId).addRow(row);
    }

    /**
     * <p>
     * Checks whether the cancellation request have already raised
     * </p>
     *
     * @param orgId Represents the organization Id
     * @return True if request already raised, false otherwise
     */
    public boolean exists(final Long orgId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(getOrgIdWhereClause(orgId));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link AccountCancellationEntry}
     * </p>
     *
     * @param orgId Represents the organization Id
     * @return The {@link AccountCancellationEntry}
     */
    @Override
    public AccountCancellationEntry get(final Long orgId) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, getOrgIdWhereClause(orgId));
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : convertRowToModel(orgId, row);
    }

    /**
     * <p>
     * Stores the survey for the account cancellation
     * </p>
     *
     * @param orgId  Represents the organization Id
     * @param survey Represents the {@link Survey}
     */
    public void submitSurvey(final Long orgId, final Survey survey) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMORGCANCELLATION.SURVEY_INFO), survey.getSurveyJson());
        updateQuery.setWhereClause(getOrgIdWhereClause(orgId));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Checks whether the user can retain the account cancellation request
     * </p>
     *
     * @param orgId Represents the organization Id
     * @return True if the user can retain, false otherwise
     */
    @Override
    public boolean canRetain(final Long orgId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final long currentTime = Instant.now().toEpochMilli();

        countQuery.setWhereClause(getOrgIdWhereClause(orgId).and(new WhereClause(Criteria.ge(table
                .getColumn(OMORGCANCELLATION.RETENTION_END_TIME), currentTime))));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retains the cancellation
     * </p>
     *
     * @param orgId Represents the organization
     */
    @Override
    public void retain(final Long orgId) {
        orgDataStore(orgId).delete(getTable(), getOrgIdWhereClause(orgId));
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link AccountCancellationEntry} model
     * </p>
     *
     * @param row The {@link Row}
     * @return The {@link AccountCancellationEntry} model
     */
    @Override
    public AccountCancellationEntry convertRowToModel(final Long orgId, final Row row) {
        final AccountCancellationEntry accountCancellationEntry = new AccountCancellationEntry(orgId);

        accountCancellationEntry.setId(row.getPKValue());
        accountCancellationEntry.setReason(AccountCancellationEntry.ReasonType.getReasonType(row.get(OMORGCANCELLATION.REASON_ID)));
        accountCancellationEntry.setOtherReason(row.get(OMORGCANCELLATION.OTHER_REASON));
        accountCancellationEntry.setRetentionEndTime(row.get(OMORGCANCELLATION.RETENTION_END_TIME));
        accountCancellationEntry.setSurveyDone(Objects.nonNull(row.get(OMORGCANCELLATION.SURVEY_INFO)));

        return accountCancellationEntry;
    }

    /**
     * <p>
     * Gets the orgId criteria
     * </p>
     *
     * @param orgId The organization ID
     * @return The org id {@link WhereClause}
     */
    private WhereClause getOrgIdWhereClause(final Long orgId) {
        return new WhereClause(Criteria.eq(getTable().getColumn(OMORGCANCELLATION.ORG_ID), orgId));
    }
}