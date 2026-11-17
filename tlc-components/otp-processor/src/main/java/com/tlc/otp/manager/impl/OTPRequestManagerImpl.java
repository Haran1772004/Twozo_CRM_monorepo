package com.tlc.otp.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.otp.code.OTPErrorCodes;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.OTPApplication;
import com.tlc.otp.model.Category;
import com.tlc.otp.model.CategoryConfig;
import com.tlc.otp.model.Channel;
import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.sql.resource.MOTPAPPLICATION;
import com.tlc.otp.sql.resource.MOTPCATEGORY;
import com.tlc.otp.sql.resource.OMOTPREQUEST;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages the {@link OTPRequest} related DB actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OTPRequestManagerImpl implements OTPRequestManager {

    private final AdminDataStore dataStore;

    private OTPRequestManagerImpl() {
        dataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static class Instance {
        private static final OTPRequestManagerImpl INSTANCE = new OTPRequestManagerImpl();
    }

    /**
     * <p>
     * Fetches the {@link OTPRequestManagerImpl} instance
     * </p>
     *
     * @return the {@link OTPRequestManagerImpl} instance
     */
    public static OTPRequestManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the otp request {@link Table}
     * </p>
     *
     * @return The otp request {@link Table}
     */
    private Table getTable() {
        return Table.get(OMOTPREQUEST.TABLE);
    }

    /**
     * <p>
     * Fetches the category table
     * </p>
     *
     * @return The category table
     */
    private Table getCategoryTable() {
        return Table.get(MOTPCATEGORY.TABLE);
    }

    /**
     * <p>
     * Fetches the application table
     * </p>
     *
     * @return The application table
     */
    private Table getApplicationTable() {
        return Table.get(MOTPAPPLICATION.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest Represents the {@link OTPRequest}
     */
    @Override
    public void save(final OTPRequest otpRequest) {
        final Table table = getTable();
        final Category category = otpRequest.getCategory();
        final DataContainer targetContainer = DataContainer.create();
        final String receiver = otpRequest.getReceiver();
        final Channel channel = otpRequest.getChannel();
        final DataContainer sourceContainer = getDataStore().get(table, buildWhereClause(receiver, channel, category));
        final Row row = Optional.ofNullable(sourceContainer).map(container -> container.getRow(table))
                .orElseGet(() -> new Row(table));

        if (row.isNewRow()) {
            row.set(OMOTPREQUEST.RECEIVER, receiver);
            row.set(OMOTPREQUEST.CHANNEL, channel.getId());
            row.set(OMOTPREQUEST.CATEGORY_ID, category.getId());
            row.setOrgId(otpRequest.getOrgId());
        }
        row.set(OMOTPREQUEST.ADDED_AT, Instant.now().toEpochMilli());
        row.set(OMOTPREQUEST.VERIFIED_AT, null);
        row.set(OMOTPREQUEST.OTP, otpRequest.getOtp());
        row.set(OMOTPREQUEST.EXPIRES_AT, otpRequest.getExpiresAt());
        row.set(OMOTPREQUEST.USER_ID, otpRequest.getUserId());
        row.set(OMOTPREQUEST.TOTAL_ATTEMPTS, otpRequest.getTotalAttempts());
        row.set(OMOTPREQUEST.VERIFCATION_TOKEN, otpRequest.getVerificationToken());

        otpRequest.setId(row.getPKValue());
        targetContainer.updateRow(row);
        getDataStore().commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     * @return The {@link OTPRequest}
     */
    @Override
    public Optional<OTPRequest> get(final String receiver, final Channel channel, final Category category) {
        final Table table = getTable();
        final Table categoryTable = getCategoryTable();
        final Table applicationTable = getApplicationTable();
        final SelectQuery selectQuery = SelectQuery.get(applicationTable);

        selectQuery.addSelectClause(table, categoryTable, applicationTable);
        selectQuery.addJoinClause(List.of(new JoinClause(applicationTable.getPKColumn(), categoryTable.getColumn(MOTPCATEGORY.APPLICATION_ID)),
                new JoinClause(categoryTable.getPKColumn(), table.getColumn(OMOTPREQUEST.CATEGORY_ID))));
        selectQuery.setWhereClause(buildWhereClause(receiver, channel, category));
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(convertRowToModel(dataContainer.getRow(table), dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param category Represents the {@link Category}
     */
    @Override
    public void create(final Category category) {
        final Table categoryTable = getCategoryTable();
        final String name = category.getName();
        final DataContainer dataContainer = getReadableDataStore().get(categoryTable, new WhereClause(Criteria.eq(categoryTable.getColumn(MOTPCATEGORY.NAME), name))
                .and(Criteria.eq(categoryTable.getColumn(MOTPCATEGORY.APPLICATION_ID), category.getOtpApplication().getId())));

        if (dataContainer.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();
            final Row row = new Row(categoryTable);

            row.set(categoryTable.getColumn(MOTPCATEGORY.NAME), name);
            row.set(categoryTable.getColumn(MOTPCATEGORY.APPLICATION_ID), category.getOtpApplication().getId());
            row.set(categoryTable.getColumn(MOTPCATEGORY.CONFIG), category.getCategoryConfig().getConfigAsJson().toString());

            targetContainer.addNewRow(row);
            dataStore.commitChanges(targetContainer);
        } else {
            throw ErrorCode.get(OTPErrorCodes.OTP_CATEGORY_ALREADY_EXISTS);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param applicationName Represents an application name
     * @param categoryName    Represents the name of the category
     * @return An Optional containing the {@link Category} if found, otherwise empty
     */
    @Override
    public Optional<Category> getCategory(final String applicationName, final String categoryName) {
        final Table categoryTable = getCategoryTable();
        final Table applicationTable = getApplicationTable();
        final SelectQuery selectQuery = SelectQuery.get(applicationTable);

        selectQuery.addSelectClause(categoryTable, applicationTable);
        selectQuery.addJoinClause(new JoinClause(applicationTable.getPKColumn(), categoryTable.getColumn(MOTPCATEGORY.APPLICATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(categoryTable.getColumn(MOTPCATEGORY.NAME),
                categoryName)).and(Criteria.eq(applicationTable.getColumn(MOTPAPPLICATION.NAME), applicationName)));
        final Row categoryRow = getReadableDataStore().get(selectQuery).getRow(categoryTable);

        if (Objects.nonNull(categoryRow)) {
            final Category category = new Category(new OTPApplication(applicationName), categoryName);

            category.setId(categoryRow.getPKValue());
            category.setCategoryConfig(new CategoryConfig(categoryRow.get(MOTPCATEGORY.CONFIG)));

            return Optional.of(category);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @return true if the OTP is valid, false otherwise
     */
    @Override
    public boolean isValidOTP(final OTPRequest otpRequest) {
        final Table otpRequestTable = getTable();
        final CountQuery countQuery = buildCountQuery(buildWhereClause(otpRequest.getReceiver(),
                otpRequest.getChannel(), otpRequest.getCategory())
                .and(Criteria.ge(otpRequestTable.getColumn(OMOTPREQUEST.EXPIRES_AT), Instant.now().toEpochMilli()))
                .and(Criteria.eq(otpRequestTable.getColumn(OMOTPREQUEST.OTP), otpRequest.getOtp())));

        return getReadableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     */
    @Override
    public void updateVerifiedTime(final String receiver, final Channel channel, final Category category) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMOTPREQUEST.VERIFIED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(buildWhereClause(receiver, channel, category));

        getDataStore().update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     * @return true if the channel is verified, false otherwise
     */
    @Override
    public boolean isReceiverVerified(final String receiver, final Channel channel, final Category category) {
        final Table table = getTable();
        final CountQuery countQuery = buildCountQuery(buildWhereClause(receiver, channel, category)
                .and(Criteria.notEq(table.getColumn(OMOTPREQUEST.VERIFIED_AT), null)));

        return getReadableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest The {@link OTPRequest}
     */
    @Override
    public void updateTotalAttempts(final OTPRequest otpRequest) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMOTPREQUEST.TOTAL_ATTEMPTS), otpRequest.getTotalAttempts());
        updateQuery.addUpdateColumn(table.getColumn(OMOTPREQUEST.ADDED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(buildWhereClause(otpRequest.getReceiver(), otpRequest.getChannel(), otpRequest.getCategory()));

        getDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link OTPRequest}
     * </p>
     *
     * @param row           Represents the {@link Row}
     * @param dataContainer Holds the otp requests related data
     * @return The {@link OTPRequest}
     */
    private OTPRequest convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table categoryTable = getCategoryTable();
        final OTPRequest otpRequest = new OTPRequest(row.get(OMOTPREQUEST.RECEIVER), Channel.getChannelById(row.get(OMOTPREQUEST.CHANNEL)));

        otpRequest.setId(row.getPKValue());
        otpRequest.setUserId(row.get(OMOTPREQUEST.USER_ID));
        otpRequest.setOrgId(row.get(OMOTPREQUEST.ORG_ID));
        otpRequest.setOtp(row.get(OMOTPREQUEST.OTP));
        otpRequest.setTotalAttempts(row.get(OMOTPREQUEST.TOTAL_ATTEMPTS));
        otpRequest.setVerifiedAt(row.get(OMOTPREQUEST.VERIFIED_AT));
        otpRequest.setExpiresAt(row.get(OMOTPREQUEST.EXPIRES_AT));
        otpRequest.setAddedAt(row.get(OMOTPREQUEST.ADDED_AT));
        otpRequest.setVerificationToken(row.get(OMOTPREQUEST.VERIFCATION_TOKEN));
        final Row categoryRow = dataContainer.getRow(categoryTable, new WhereClause(Criteria.eq(categoryTable.getPKColumn(),
                row.get(OMOTPREQUEST.CATEGORY_ID))));

        if (Objects.nonNull(categoryRow)) {
            final Table applicationTable = getApplicationTable();
            final Long applicationId = categoryRow.get(MOTPCATEGORY.APPLICATION_ID);
            final Row applicationRow = dataContainer.getRow(applicationTable, new WhereClause(Criteria.eq(applicationTable.getPKColumn(), applicationId)));

            if (Objects.nonNull(applicationRow)) {
                final OTPApplication otpApplication = new OTPApplication(applicationRow.get(MOTPAPPLICATION.NAME));
                final Category category = new Category(otpApplication, categoryRow.get(MOTPCATEGORY.NAME));

                otpApplication.setId(applicationId);
                category.setId(categoryRow.getPKValue());
                category.setCategoryConfig(new CategoryConfig(categoryRow.get(MOTPCATEGORY.CONFIG)));
                otpRequest.setCategory(category);
            }
        }
        return otpRequest;
    }

    /**
     * <p>
     * Builds the {@link WhereClause} based on the receiver, channel, and category.
     * </p>
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     * @return The {@link WhereClause}
     */
    private WhereClause buildWhereClause(final String receiver, final Channel channel, final Category category) {
        final Table table = getTable();

        return new WhereClause(Criteria.eq(table.getColumn(OMOTPREQUEST.RECEIVER), receiver))
                .and(Criteria.eq(table.getColumn(OMOTPREQUEST.CHANNEL), channel.getId()))
                .and(Criteria.eq(table.getColumn(OMOTPREQUEST.CATEGORY_ID), category.getId()));
    }

    /**
     * <p>
     * Builds the {@link CountQuery} for the provided criteria
     * </p>
     *
     * @param whereClause Represents the {@link WhereClause}
     * @return The {@link CountQuery}
     */
    private CountQuery buildCountQuery(final WhereClause whereClause) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(whereClause);
        return countQuery;
    }

    /**
     * <p>
     * Fetches the {@link AdminDataStore}
     * </p>
     *
     * @return {@link AdminDataStore}
     */
    private AdminDataStore getDataStore() {
        return dataStore;
    }

    /**
     * <p>
     * Fetches the {@link ReadableDataStore}
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return getDataStore();
    }
}
