package com.tlc.email.server.internal.webhook.receiver.manager;

import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.server.internal.ReadableManager;
import com.tlc.email.server.internal.WritableManager;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.provider.google.GoogleEmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.OutlookEmailWebhookReceiver;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMEMAILWEBHOOKRECEIVER;
import com.tlc.email.server.sql.resource.MEMGOOGLEWEBHOOKRECEIVER;
import com.tlc.email.server.sql.resource.MEMOUTLOOKWEBHOOKRECEIVER;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages webhook subscriptions for email providers.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see ReadableManager
 * @see WritableManager
 * @see EmailWebHookReceiverManager
 */
public class EmailWebhookReceiverManagerImpl implements EmailWebHookReceiverManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailWebhookReceiverManagerImpl.class);

    private EmailWebhookReceiverManagerImpl() {
    }

    /**
     * <p>
     * Holds the single instance of {@link EmailWebhookReceiverManagerImpl}.
     * </p>
     */
    private static class Instance {
        private static final EmailWebhookReceiverManagerImpl INSTANCE = new EmailWebhookReceiverManagerImpl();
    }

    /**
     * Gets the instance of {@link EmailWebhookReceiverManagerImpl}
     *
     * @return {@link EmailWebhookReceiverManagerImpl}
     */
    public static EmailWebhookReceiverManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    private Table getTable() {
        return Table.get(MEMEMAILWEBHOOKRECEIVER.TABLE);
    }

    /**
     * Gets the {@link Table} of email address.
     *
     * @return the email address {@link Table}
     */
    private Table getAddressTable() {
        return Table.get(MEMADDRESS.TABLE);
    }

    /**
     * Gets the {@link Table} of outlook webhook receiver.
     *
     * @return the outlook webhook receiver {@link Table}
     */
    private Table getOutlookWebhookReceiverTable() {
        return Table.get(MEMOUTLOOKWEBHOOKRECEIVER.TABLE);
    }

    /**
     * Gets the {@link Table} of outlook webhook receiver.
     *
     * @return the Google webhook receiver {@link Table}
     */
    private Table getGoogleWebhookReceiverTable() {
        return Table.get(MEMGOOGLEWEBHOOKRECEIVER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the email id.
     * @return {@link Optional} of {@link EmailWebhookReceiver}
     */
    @Override
    public Optional<EmailWebhookReceiver> get(final long id) {
        final Table table = getTable();
        final DataContainer dataContainer = getExistingWebhook(List.of(id));
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? Optional.empty() : Optional.ofNullable(convertRowToModel(row, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Refers the email ids.
     * @return {@link Collection} of {@link EmailWebhookReceiver}
     */
    @Override
    public Collection<EmailWebhookReceiver> get(final Collection<Long> ids) {
        final Table table = getTable();
        final DataContainer dataContainer = getExistingWebhook(ids);
        final Collection<Row> rows = dataContainer.getRows(table).toList();

        return rows.stream().map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailWebhookReceiver Represents the {@link EmailWebhookReceiver}
     */
    @Override
    public void create(final EmailWebhookReceiver emailWebhookReceiver) {
        loadWebhookIntoContainer(true, emailWebhookReceiver);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailWebhookReceiver Represents the {@link EmailWebhookReceiver}
     */
    @Override
    public void update(final EmailWebhookReceiver emailWebhookReceiver) {
        loadWebhookIntoContainer(false, emailWebhookReceiver);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailWebhookReceivers Represents the {@link Collection} of {@link EmailWebhookReceiver}
     */
    @Override
    public void update(final Collection<EmailWebhookReceiver> emailWebhookReceivers) {
        loadWebhookIntoContainer(false, emailWebhookReceivers);
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the webhook ids
     */
    @Override
    public void delete(Collection<Long> ids) {
        final Table table = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        writableDataStore().delete(deleteQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the reference of email webhook
     */
    @Override
    public void delete(final Long id) {
        final Table table = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        writableDataStore().delete(deleteQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the email id
     * @return true, if the webhook is expired; otherwise false
     */
    @Override
    public boolean isExpired(final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID), id))
                .and(Criteria.le(table.getColumn(MEMEMAILWEBHOOKRECEIVER.EXPIRY_TIME), Instant.now().toEpochMilli())));

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailId Represents the email id
     * @return subscription id
     */
    @Override
    public Optional<String> getSubscriptionId(final Long emailId) {
        final Table table = getTable();
        final Table outlookWebhookReceiverTable = getOutlookWebhookReceiverTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(),
                outlookWebhookReceiverTable.getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria
                .eq(table.getColumn(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID), emailId)));
        final Row row = readableDataStore().get(selectQuery).getRow(outlookWebhookReceiverTable);

        return Optional.ofNullable(row.get(MEMOUTLOOKWEBHOOKRECEIVER.SUBSCRIPTION_ID));
    }

    /**
     * {@inheritDoc}
     *
     * @param emailId the reference of email address
     * @return true, if the email is unsubscribed; otherwise false
     */
    @Override
    public boolean isUnsubscribed(final Long emailId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID), emailId)));

        return readableDataStore().get(countQuery) == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return renewable webhook receivers
     */
    @Override
    public Collection<EmailWebhookReceiver> getRenewableWebhookReceivers(final int limit) {
        final Table table = getTable();
        final Instant nowInstant = Instant.now();
        final Instant minuteLaterInstant = nowInstant.plus(30, ChronoUnit.MINUTES);
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                        getEmailWebhookJoinClause(),
                        new JoinClause(pkColumn, getOutlookWebhookReceiverTable()
                                .getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin),
                        new JoinClause(pkColumn, getGoogleWebhookReceiverTable()
                                .getColumn(MEMGOOGLEWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin)
                )
        );
        selectQuery.setWhereClause(new WhereClause(
                        Criteria.between(table.getColumn(MEMEMAILWEBHOOKRECEIVER.EXPIRY_TIME), nowInstant.toEpochMilli(),
                                minuteLaterInstant.toEpochMilli())
                )
        );
        selectQuery.setLimitClause(new LimitClause(limit));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Collection<Row> rows = dataContainer.getRows(table).toList();

        return rows.stream().map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the emailAddress
     * @return true if the emailAddress is already exists;otherwise false
     */
    @Override
    public boolean exists(final String emailAddress) {
        final Table table = getTable();
        final Table addressTable = getAddressTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.addJoinClause(getEmailWebhookJoinClause());
        countQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailWebhookReceiver}
     */
    @Override
    public Optional<EmailWebhookReceiver> get(final String emailAddress) {
        final Table table = getTable();
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                        getEmailWebhookJoinClause(),
                        new JoinClause(pkColumn, getOutlookWebhookReceiverTable()
                                .getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin),
                        new JoinClause(pkColumn, getGoogleWebhookReceiverTable()
                                .getColumn(MEMGOOGLEWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? Optional.empty() : Optional.ofNullable(convertRowToModel(row, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param subscriptionId Represents the subscription ID
     * @return {@link SyncAddressInfo}
     */
    @Override
    public Optional<SyncAddressInfo> getSyncAddressInfo(final String subscriptionId) {
        final Table table = getTable();
        final Table addressTable = getAddressTable();
        final Table outlookWebhookTable = getOutlookWebhookReceiverTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(List.of(
                        getEmailWebhookJoinClause(),
                        new JoinClause(table.getPKColumn(), outlookWebhookTable
                                .getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID))
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outlookWebhookTable.getColumn(MEMOUTLOOKWEBHOOKRECEIVER.SUBSCRIPTION_ID), subscriptionId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row row = dataContainer.getRow(addressTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(new SyncAddressInfo(row.getPKValue(), row.get(MEMADDRESS.EMAIL_ADDRESS)));
    }

    /**
     * {@inheritDoc}
     *
     * @param row           email row to be converted.
     * @param dataContainer A container which holds the updated information about the emails
     * @return {@link EmailWebhookReceiver}
     */
    @Override
    public EmailWebhookReceiver convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Row addressRow = dataContainer.getRow(getAddressTable(), (Long) row.get(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID));
        final EmailProvider emailProvider = EmailProvider.valueOf((short) row.get(MEMEMAILWEBHOOKRECEIVER.PROVIDER_ID));
        final SyncAddressInfo emailAddress = new SyncAddressInfo(addressRow.getPKValue(), addressRow.get(MEMADDRESS.EMAIL_ADDRESS));
        final EmailWebhookReceiver emailWebhookReceiver = switch (emailProvider) {
            case GOOGLE -> {
                final GoogleEmailWebhookReceiver googleEmailWebhookReceiver = new GoogleEmailWebhookReceiver(emailAddress);
                final Row googleWebHookReceiverRow = dataContainer.getRow(getGoogleWebhookReceiverTable());

                if (Objects.nonNull(googleWebHookReceiverRow)) {
                    googleEmailWebhookReceiver.setHistoryId(BigInteger.valueOf(googleWebHookReceiverRow.get(MEMGOOGLEWEBHOOKRECEIVER.HISTORY_ID)));
                }

                yield googleEmailWebhookReceiver;
            }
            case OUTLOOK -> {
                final OutlookEmailWebhookReceiver outlookEmailWebhookReceiver = new OutlookEmailWebhookReceiver(emailAddress);
                final Row outlookWebhookReceiverRow = dataContainer.getRow(getOutlookWebhookReceiverTable());

                if (Objects.nonNull(outlookWebhookReceiverRow)) {
                    outlookEmailWebhookReceiver.setSubscriptionId(outlookWebhookReceiverRow.get(MEMOUTLOOKWEBHOOKRECEIVER.SUBSCRIPTION_ID));
                }

                yield outlookEmailWebhookReceiver;
            }
            case ZOHO, ICLOUD, AOL, YAHOO -> null;
        };

        if (Objects.nonNull(emailWebhookReceiver)) {
            emailWebhookReceiver.setExpiryTime(row.get(MEMEMAILWEBHOOKRECEIVER.EXPIRY_TIME));
            emailWebhookReceiver.setId(row.getPKValue());
        }

        return emailWebhookReceiver;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the email webhook receiver id
     * @return true, if the given email webhook reference is exists; otherwise false
     */
    @Override
    public boolean exists(final long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the collection of email model exists or not.
     * </p>
     *
     * @param ids Represents the {@link Collection} of email ids
     * @return true, if the given {@link Collection} of webhook reference are exists; otherwise false
     */
    @Override
    public boolean exists(final Set<Long> ids) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return ids.size() == readableDataStore().get(countQuery);
    }

    /**
     * Loads the {@link EmailWebhookReceiver} into container.
     *
     * @param isCreateEvent        A flag which indicates whether the action is related to create or update event
     * @param emailWebhookReceiver Represents the {@link EmailWebhookReceiver}
     */
    private void loadWebhookIntoContainer(final boolean isCreateEvent, final EmailWebhookReceiver emailWebhookReceiver) {
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer existingWebhookContainer = isCreateEvent ? null : getExistingWebhook(List.of(emailWebhookReceiver.id()));

        loadWebhookIntoContainer(isCreateEvent, emailWebhookReceiver, existingWebhookContainer, targetContainer);

        writableDataStore().commitChanges(targetContainer);
    }

    /**
     * Loads the {@link EmailWebhookReceiver} into container.
     *
     * @param isCreateEvent         A flag which indicates whether the action is related to create or update event
     * @param emailWebhookReceivers Represents the {@link Collection} of {@link EmailWebhookReceiver}
     */
    private void loadWebhookIntoContainer(final boolean isCreateEvent, final Collection<EmailWebhookReceiver> emailWebhookReceivers) {
        final DataContainer targetContainer = DataContainer.create();
        final Collection<Long> ids = emailWebhookReceivers.stream().map(EmailWebhookReceiver::id).collect(Collectors.toSet());
        final DataContainer existingWebhookContainer = isCreateEvent ? null : getExistingWebhook(ids);

        for (final EmailWebhookReceiver emailWebhookReceiver : emailWebhookReceivers) {
            loadWebhookIntoContainer(isCreateEvent, emailWebhookReceiver, existingWebhookContainer, targetContainer);
        }

        writableDataStore().commitChanges(targetContainer);
    }

    /**
     * Loads the {@link EmailWebhookReceiver}into container.
     *
     * @param isCreateEvent            A flag which indicates whether the action is related to create or update event
     * @param emailWebhookReceiver     Represents the {@link EmailWebhookReceiver}
     * @param existingWebhookContainer Holds the existing information of webhook related field values
     * @param targetContainer          Holds the updated information of webhook related field values to store it in db
     */
    private void loadWebhookIntoContainer(final boolean isCreateEvent, final EmailWebhookReceiver emailWebhookReceiver,
                                          final DataContainer existingWebhookContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long id = emailWebhookReceiver.id();
        final Row existingWebhookRow = Objects.isNull(existingWebhookContainer) ? null : existingWebhookContainer
                .getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final Row row = isCreateEvent || Objects.isNull(existingWebhookRow) ? new Row(table) : existingWebhookRow;

        if (row.isNewRow()) {
            row.set(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID, emailWebhookReceiver.getEmailAddress().id());
            row.set(MEMEMAILWEBHOOKRECEIVER.PROVIDER_ID, emailWebhookReceiver.getProvider().getId());
        }
        row.set(MEMEMAILWEBHOOKRECEIVER.EXPIRY_TIME, emailWebhookReceiver.getExpiryTime());

        emailWebhookReceiver.setId(row.getPKValue());

        targetContainer.updateRow(row);

        if (emailWebhookReceiver instanceof OutlookEmailWebhookReceiver outlookEmailWebhookReceiver) {
            loadOutlookWebhookReceiverIntoContainer(isCreateEvent, outlookEmailWebhookReceiver, existingWebhookContainer, targetContainer);
        } else if (emailWebhookReceiver instanceof GoogleEmailWebhookReceiver googleEmailWebhookReceiver) {
            loadGoogleWebhookReceiverIntoContainer(isCreateEvent, googleEmailWebhookReceiver, existingWebhookContainer, targetContainer);
        }
    }

    /**
     * Loads the outlook {@link EmailWebhookReceiver} into container.
     *
     * @param isCreateEvent               A flag which indicates whether the action is related to create or update event
     * @param outlookEmailWebhookReceiver Represents the {@link OutlookEmailWebhookReceiver}
     * @param existingWebhookContainer    Holds the existing information of webhook related field values
     * @param targetContainer             Holds the updated information of webhook related field values to store it in db
     */
    private void loadOutlookWebhookReceiverIntoContainer(final boolean isCreateEvent, final OutlookEmailWebhookReceiver outlookEmailWebhookReceiver,
                                                         final DataContainer existingWebhookContainer, final DataContainer targetContainer) {
        final Table table = getOutlookWebhookReceiverTable();
        final Long id = outlookEmailWebhookReceiver.id();
        final Row existingOutlookWebhookRow = Objects.isNull(existingWebhookContainer) ? null : existingWebhookContainer
                .getRow(table, new WhereClause(Criteria.eq(table.getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), id)));
        final Row row = isCreateEvent || Objects.isNull(existingOutlookWebhookRow) ? new Row(table) : existingOutlookWebhookRow;

        if (row.isNewRow()) {
            row.set(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID, id);
        }

        row.set(MEMOUTLOOKWEBHOOKRECEIVER.SUBSCRIPTION_ID, outlookEmailWebhookReceiver.getSubscriptionId());

        targetContainer.updateRow(row);
    }

    /**
     * Loads the Google {@link GoogleEmailWebhookReceiver} into container.
     *
     * @param isCreateEvent              A flag which indicates whether the action is related to create or update event
     * @param googleEmailWebhookReceiver Represents the {@link GoogleEmailWebhookReceiver}
     * @param existingWebhookContainer   Holds the existing information of webhook related field values
     * @param targetContainer            Holds the updated information of webhook related field values to store it in db
     */
    private void loadGoogleWebhookReceiverIntoContainer(final boolean isCreateEvent, final GoogleEmailWebhookReceiver googleEmailWebhookReceiver,
                                                        final DataContainer existingWebhookContainer, final DataContainer targetContainer) {
        final Table table = getGoogleWebhookReceiverTable();
        final Long id = googleEmailWebhookReceiver.id();
        final Row existingOutlookWebhookRow = Objects.isNull(existingWebhookContainer) ? null : existingWebhookContainer
                .getRow(table, new WhereClause(Criteria.eq(table.getColumn(MEMGOOGLEWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), id)));
        final Row row = isCreateEvent || Objects.isNull(existingOutlookWebhookRow) ? new Row(table) : existingOutlookWebhookRow;

        if (row.isNewRow()) {
            row.set(MEMGOOGLEWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID, id);
        }

        row.set(MEMGOOGLEWEBHOOKRECEIVER.HISTORY_ID, googleEmailWebhookReceiver.getHistoryId());

        targetContainer.updateRow(row);
    }

    /**
     * Gets the existing webhook container for the given {@link Collection} of ids.
     *
     * @param ids Represents the {@link Collection} of webhook ids.
     * @return {@link DataContainer}
     */
    private DataContainer getExistingWebhook(final Collection<Long> ids) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                        getEmailWebhookJoinClause(),
                        new JoinClause(pkColumn, getOutlookWebhookReceiverTable()
                                .getColumn(MEMOUTLOOKWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin),
                        new JoinClause(pkColumn, getGoogleWebhookReceiverTable()
                                .getColumn(MEMGOOGLEWEBHOOKRECEIVER.WEBHOOK_RECEIVER_ID), leftJoin)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return readableDataStore().get(selectQuery);
    }

    /**
     * Gets the generic {@link JoinClause} for to retrieve the {@link EmailWebhookReceiver}.
     *
     * @return {@link JoinClause}
     */
    private JoinClause getEmailWebhookJoinClause() {
        return new JoinClause(getTable().getColumn(MEMEMAILWEBHOOKRECEIVER.EMAIL_ID), getAddressTable().getPKColumn());
    }
}