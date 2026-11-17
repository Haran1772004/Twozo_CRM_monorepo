package com.tlc.crm.core.notification.email.bulk.api.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.functions.TriConsumer;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.MetricsInsight;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCONTACT;

import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMCONTACTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDBULKEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.address.AddressType;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.NO_PRIMARY_EMAIL;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.BOUNCED;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.UNSUBSCRIBED;

/**
 * Manages the DB related actions for bulk mail {@link Metrics}
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class BulkMailMetricsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkMailMetricsManager.class);

    private BulkMailMetricsManager() {
    }

    private static class Instance {
        private static final BulkMailMetricsManager INSTANCE = new BulkMailMetricsManager();
    }

    /**
     * Gets the {@link BulkMailMetricsManager} instance
     *
     * @return {@link BulkMailMetricsManager}
     */
    public static BulkMailMetricsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the bulk mail metrics table
     *
     * @return the {@link Table} of bulk mail metrics
     */
    private Table getTable() {
        return Table.get(OMEMBULKEMAILMETRICS.TABLE);
    }

    /**
     * Gets the outbound bulk mail metrics table
     *
     * @return the {@link Table} of outbound bulk mail metrics
     */
    private Table getOutboundMetricsTable() {
        return Table.get(OMEMOUTBOUNDBULKEMAILMETRICS.TABLE);
    }

    /**
     * Gets the outbound bulk mail table
     *
     * @return the {@link Table} of outbound bulk mail
     */
    public Table getOutboundBulkMailTable() {
        return Table.get(OMEMOUTBOUNDBULKEMAIL.TABLE);
    }

    /**
     * Gets the outbound mail table
     *
     * @return the {@link Table} of outbound mail
     */
    private Table getOutboundMailTable() {
        return Table.get(OMEMOUTBOUNDEMAIL.TABLE);
    }

    /**
     * Gets the conversation recipients table
     *
     * @return the {@link Table} of conversation recipients
     */
    private Table getConversationRecipientsTable() {
        return Table.get(OMEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * Gets the email address table
     *
     * @return the {@link Table} of email address
     */
    private Table getemailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Loads the outbound bulk mail metrics in to target {@link DataContainer} for the provided organization and user.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents the user id
     * @param bulkMailId         Represents the bulk mail id
     * @param mailId             Represents the outbound mail id
     * @param outboundBulkMailId Represents the outbound bulk mail id
     * @param metricTypes        Represents the {@link Collection} of {@link MetricsType}
     * @param targetContainer    Holds the updated information of bulk mail metrics data
     */
    public void loadOutboundMetricsIntoContainer(final long orgId, final long userId, final Long bulkMailId,
                                                 final Long mailId, final Long outboundBulkMailId, final Collection<MetricsType> metricTypes,
                                                 final DataContainer targetContainer) {
        final DataContainer existingMetricsContainer = getExistingMetricsContainer(orgId, userId, mailId);
        final Table outboundBulkMailMetricsTable = getOutboundMetricsTable();
        final Row outboundBulkMetricsRow = existingMetricsContainer.isEmpty() ? new Row(outboundBulkMailMetricsTable)
                : existingMetricsContainer.getRow(outboundBulkMailMetricsTable);
        final Long id = Objects.isNull(bulkMailId) ?
                existingMetricsContainer.getRow(getOutboundBulkMailTable()).get(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID) : bulkMailId;

        if (outboundBulkMetricsRow.isNewRow()) {
            outboundBulkMetricsRow.set(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID, outboundBulkMailId);
            outboundBulkMetricsRow.setOrgId(orgId);
        }
        populateDataContainerWithMetrics(outboundBulkMetricsRow, metricTypes, true, targetContainer);
        loadMetricsIntoContainer(orgId, userId, id, metricTypes, existingMetricsContainer, targetContainer);
    }

    /**
     * Checks whether the bulk mail exists in metrics or not
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param bulkMailId Represents the bulk mail id
     * @return true, when the bulk mail exists in metrics
     */
    public boolean exists(final long orgId, final long userId, final long bulkMailId) {
        final Table bulkMailTable = Table.get(OMEMBULKEMAIL.TABLE);
        final Table bulkMailMetricsTable = getTable();
        final CountQuery countQuery = CountQuery.get(bulkMailTable);

        countQuery.addJoinClause(new JoinClause(bulkMailTable.getPKColumn(), bulkMailMetricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(bulkMailMetricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID), bulkMailId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_DRAFT), false)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the outbound bulk mail ID for a given organization, user, and mail ID.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param mailId Represents the outbound mail id
     * @return {@link Long}, the outbound bulk mail id
     */
    public Long getOutboundBulkMailId(final long orgId, final long userId, final long mailId) {
        final Table bulkMailTable = Table.get(OMEMBULKEMAIL.TABLE);
        final Table outboundMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Column outboundBulkMailPkColumn = outboundBulkMailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailTable.getPKColumn(), outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailPkColumn, outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), mailId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_DRAFT), false)));
        selectQuery.addSelectClause(outboundBulkMailPkColumn);
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRow(outboundBulkMailTable).getPKValue();
    }

    /**
     * Retrieves the {@link Metrics} data related to a particular bulk mail, within a specific organization and user.
     *
     * @param orgId      Represents an organization id
     * @param senderId   Represents the sender id
     * @param bulkMailId Represents the bulk mail id
     * @return the bulk mail {@link Metrics}
     */
    public Metrics getBulkMailMetrics(final long orgId, final long senderId, final long bulkMailId) {
        final Table metricsTable = getTable();
        final Table outboundMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Column metricsBulkMailIdColumn = metricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(metricsTable);

        selectQuery.addJoinClause(List.of(new JoinClause(metricsBulkMailIdColumn, outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getPKColumn(), outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID))));
        selectQuery.addSelectClause(metricsTable, outboundMetricsTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID), senderId))
                .and(Criteria.eq(metricsBulkMailIdColumn, bulkMailId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(metricsTable);
        final Metrics metrics = new Metrics();

        if (Objects.nonNull(row)) {
            metrics.setId(bulkMailId);
            metrics.setOrgId(orgId);
            metrics.setSenderId(senderId);
            metrics.setClickedCount(row.get(metricsTable.getColumn(OMEMBULKEMAILMETRICS.CLICKED)));
            metrics.setRepliedCount(row.get(metricsTable.getColumn(OMEMBULKEMAILMETRICS.REPLIED)));
            metrics.setSentCount(row.get(metricsTable.getColumn(OMEMBULKEMAILMETRICS.SENT)));
            metrics.setOpenedCount(row.get(metricsTable.getColumn(OMEMBULKEMAILMETRICS.OPENED)));
            metrics.setOpenedNotClickedCount((int) dataContainer.getRows(outboundMetricsTable, getOpenedNotClickedWhereClause(outboundMetricsTable)).count());
            metrics.setOpenedNotRepliedCount((int) dataContainer.getRows(outboundMetricsTable, getOpenedNotRepliedWhereClause(outboundMetricsTable)).count());
            metrics.setNotOpenedCount((int) dataContainer.getRows(outboundMetricsTable,
                    new WhereClause(Criteria.eq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED), 0))).count());
            metrics.setNotSentCount((int) dataContainer.getRows(outboundMetricsTable, getUnsentMetricsQuery(outboundMetricsTable)).count());
        }

        return metrics;
    }

    /**
     * Fetches the {@link Collection} of {@link MetricsInsight} based on the {@link MetricsType} for a specified organization.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param bulkMailId  Represents the bulk mail id
     * @param metricsType Represents the {@link MetricsType}
     * @return {@link Collection} of {@link MetricsInsight}
     */
    public Collection<MetricsInsight> getMetricsInsights(final long orgId, final long userId, final long bulkMailId, final MetricsType metricsType) {
        final Table outboundMailTable = getOutboundMailTable();
        final DataContainer dataContainer = getMetricsInsightDataContainer(orgId, userId, bulkMailId, metricsType);
        final Collection<Long> conversationIds = new ArrayList<>(dataContainer.getRows(outboundMailTable)
                .map(row -> (Long) row.get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)).toList());

        return acquireMetricsInsight(orgId, conversationIds, metricsType, dataContainer);
    }

    /**
     * Gets the existing {@link DataContainer} of metrics insights which is based on provided bulk mail and its metrics type.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param bulkMailId  Represents the bulk mail id
     * @param metricsType Represents the {@link MetricsType}
     * @return {@link DataContainer} of metrics insights
     */
    private DataContainer getMetricsInsightDataContainer(final long orgId, final long userId, final long bulkMailId, final MetricsType metricsType) {
        final Table outboundBulkMailMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table emailAddressTable = getemailAddressTable();

        final Column bulkEmailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final Column outboundConversationIdColumn = outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Column recipientsConversationIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column recipientsEmailIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final Column outboundPkColumn = outboundMailTable.getPKColumn();
        final Column outboundBulkMailSenderIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID);
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);
        final SelectQuery innerSelectQuery = SelectQuery.get(outboundMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getPKColumn(),
                        outboundBulkMailMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID)),
                new JoinClause(bulkEmailOutboundIdColumn, outboundPkColumn),
                new JoinClause(outboundConversationIdColumn, recipientsConversationIdColumn),
                new JoinClause(recipientsEmailIdColumn, emailAddressTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(bulkEmailOutboundIdColumn, outboundConversationIdColumn,
                recipientsConversationIdColumn, conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), recipientsEmailIdColumn));
        selectQuery.addSelectClause(outboundBulkMailMetricsTable);

        innerSelectQuery.addSelectClause(outboundConversationIdColumn.distinct());
        innerSelectQuery.addJoinClause(List.of(new JoinClause(outboundPkColumn, bulkEmailOutboundIdColumn)));
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailId))
                .and(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.ORG_ID), orgId))
                .and(Criteria.eq(outboundBulkMailSenderIdColumn, userId)));
        final WhereClause whereClause = getMetricTypeWhereClause(metricsType, outboundBulkMailMetricsTable);

        selectQuery.setWhereClause(whereClause.and(Criteria.in(outboundConversationIdColumn, innerSelectQuery))
                .and(Criteria.eq(outboundBulkMailSenderIdColumn, userId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Loads the bulk mail metrics in to target {@link DataContainer} for the provided organization and user.
     *
     * @param orgId           Represents an organization id
     * @param bulkMailId      Represents the bulk mail id
     * @param metricTypes     Represents the {@link Collection} of {@link MetricsType}
     * @param dataContainer   Stores the existing {@link DataContainer} of a given user and organization
     * @param targetContainer Holds the updated information of bulk mail metrics data
     */
    private void loadMetricsIntoContainer(final long orgId, final long userId, final Long bulkMailId, final Collection<MetricsType> metricTypes,
                                          final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table metricsTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(metricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID), bulkMailId));
        final Row updatedMetricsRow = targetContainer.getRow(metricsTable, whereClause);
        final boolean isRowModified = Objects.isNull(updatedMetricsRow);
        final DataContainer existingMetricsDataContainer = getExistingMetricsDataContainer(orgId, userId, bulkMailId);
        final Row metricsRow = existingMetricsDataContainer.isEmpty() && isRowModified
                ? new Row(metricsTable) : !isRowModified ? updatedMetricsRow : existingMetricsDataContainer.getRow(metricsTable, whereClause);

        if (metricsRow.isNewRow()) {
            metricsRow.setOrgId(orgId);
            metricsRow.set(OMEMBULKEMAILMETRICS.BULK_MAIL_ID, bulkMailId);
            metricsRow.set(OMEMBULKEMAILMETRICS.CREATED_TIME, Instant.now().toEpochMilli());
        }
        populateDataContainerWithMetrics(metricsRow, metricTypes, false, targetContainer);
    }

    /**
     * Gets the existing {@link DataContainer} of {@link MetricsType} for the provided outbound id.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param mailId Represents the mail id
     * @return the existing bulk mail metrics {@link DataContainer}
     */
    private DataContainer getExistingMetricsContainer(final long orgId, final long userId, final Long mailId) {
        final Table outboundBulkMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table bulkMailMetricsTable = getTable();
        final Column outboundBulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getPKColumn(), outboundBulkMetricsTable
                        .getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailIdColumn, bulkMailMetricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID))));
        selectQuery.addSelectClause(bulkMailMetricsTable, outboundBulkMetricsTable);
        selectQuery.addSelectClause(outboundBulkMailIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), mailId))
                .and(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID), userId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the existing metrics {@link DataContainer}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param bulkMailId Represents the bulk mail id
     * @return the {@link DataContainer}
     */
    private DataContainer getExistingMetricsDataContainer(final long orgId, final long userId, final Long bulkMailId) {
        final Table outboundBulkMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table bulkMailMetricsTable = getTable();
        final Column outboundBulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getPKColumn(), outboundBulkMetricsTable
                        .getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailIdColumn, bulkMailMetricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID))));
        selectQuery.addSelectClause(bulkMailMetricsTable, outboundBulkMetricsTable);
        selectQuery.addSelectClause(outboundBulkMailIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailId))
                .and(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID), userId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Populates the bulk mail in to target {@link DataContainer} based on {@link MetricsType}.
     *
     * @param row                      Represents the {@link Row} of bulk mail metrics
     * @param metricsTypes             Represents the {@link Collection} of {@link MetricsType}
     * @param hasOutboundDependentMail A flag that indicates whether the bulk mail is outbound or not.
     * @param targetContainer          Holds the updated information of bulk mail metrics data
     */
    private void populateDataContainerWithMetrics(final Row row, final Collection<MetricsType> metricsTypes,
                                                  final boolean hasOutboundDependentMail, final DataContainer targetContainer) {
        final Consumer<MetricsType> setMetricsType = (type) -> {
            final String columnKey = type.name();
            final int count = row.get(columnKey);

            row.set(columnKey, count + 1);
        };

        for (final MetricsType type : metricsTypes) {

            if (MetricsType.hasCommonMetrics(type) || (hasOutboundDependentMail && MetricsType.hasOutboundMetrics(type))) {
                setMetricsType.accept(type);
            }
        }
        targetContainer.updateRow(row);
    }

    /**
     * Retrieves the {@link WhereClause} that is determined by the criteria applied according to the metric type.
     *
     * @param metricsType                  Represents the {@link MetricsType}
     * @param outboundBulkMailMetricsTable Represents the {@link Table} of outbound bulk mail metrics
     * @return {@link WhereClause}
     */
    private WhereClause getMetricTypeWhereClause(final MetricsType metricsType, final Table outboundBulkMailMetricsTable) {
        final int count = 0;
        final Function<String, WhereClause> getWhereClause = (columnKey) ->
                new WhereClause(Criteria.notEq(outboundBulkMailMetricsTable.getColumn(columnKey), count));

        return switch (metricsType) {
            case SENT -> getWhereClause.apply(OMEMOUTBOUNDBULKEMAILMETRICS.SENT);
            case REPLIED -> getWhereClause.apply(OMEMOUTBOUNDBULKEMAILMETRICS.REPLIED);
            case OPENED -> getWhereClause.apply(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED);
            case CLICKED -> getWhereClause.apply(OMEMOUTBOUNDBULKEMAILMETRICS.CLICKED);
            case NOT_SENT -> getUnsentMetricsQuery(outboundBulkMailMetricsTable);
            case OPENED_NOT_CLICKED -> getOpenedNotClickedWhereClause(outboundBulkMailMetricsTable);
            case OPENED_NOT_REPLIED -> getOpenedNotRepliedWhereClause(outboundBulkMailMetricsTable);
            case NOT_OPENED -> new WhereClause(Criteria.eq(outboundBulkMailMetricsTable
                    .getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED), count));
            default -> throw ErrorCode.get(BulkMailErrorCodes.INVALID_METRICS_TYPE);
        };
    }

    /**
     * Gets the {@link Collection} of bulk mail {@link MetricsInsight}.
     *
     * @param orgId           Represents an organization id
     * @param conversationIds Represents the {@link Collection} of conversation ids
     * @param metricsType     Represents the {@link MetricsType}
     * @param dataContainer   A {@link DataContainer} that holds the updated bulk metrics related data
     * @return {@link Collection} of {@link MetricsInsight}
     */
    private Collection<MetricsInsight> acquireMetricsInsight(final long orgId, final Collection<Long> conversationIds,
                                                             final MetricsType metricsType, final DataContainer dataContainer) {
        final Collection<MetricsInsight> metricsInsights = new ArrayList<>();

        if (!conversationIds.isEmpty()) {
            final Table contactConversationsTable = Table.get(OMEMCONTACTCONVERSATION.TABLE);
            final Table contactTable = Table.get(MCOCONTACT.TABLE);
            final Column contactConversationIdColumn = contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID);
            final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);

            contactSelectQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID)));
            contactSelectQuery.addSelectClause(List.of(contactConversationIdColumn, contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID),
                    contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
            contactSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactConversationIdColumn, conversationIds)));
            final DataContainer contactDataContainer = getOrgDataStore(orgId).get(contactSelectQuery);
            final Collection<Long> contactConversationIds = contactDataContainer.getRows(contactConversationsTable)
                    .map(row -> (Long) row.get(OMEMCONTACTCONVERSATION.CONVERSATION_ID)).toList();
            final Table outboundMetricsTable = getOutboundMetricsTable();

            for (final Row row : dataContainer.getRows(outboundMetricsTable).toList()) {
                loadMetricsInsightsData(conversationIds, contactConversationIds, metricsType, metricsInsights, row, dataContainer, contactDataContainer);
            }
        }

        return metricsInsights;
    }

    /**
     * Loads the {@link MetricsInsight} data based on the provided {@link DataContainer} for a specific {@link MetricsType}.
     *
     * @param conversationIds        Represents the {@link Collection} of conversation which is linked with the outbound bulk mail
     * @param contactConversationIds Represents the {@link Collection} of contact conversation
     * @param metricsType            Represents the {@link MetricsType}
     * @param metricsInsights        Represents the {@link MetricsInsight}
     * @param row                    Represents the {@link Row} of outbound bulk mail metrics
     * @param dataContainer          A {@link DataContainer} that holds the updated bulk metrics related data
     * @param contactDataContainer   A {@link DataContainer} that holds the bulk mail related contact data
     */
    private void loadMetricsInsightsData(final Collection<Long> conversationIds, final Collection<Long> contactConversationIds,
                                         final MetricsType metricsType, final Collection<MetricsInsight> metricsInsights, final Row row,
                                         final DataContainer dataContainer, final DataContainer contactDataContainer) {
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table contactConversationsTable = Table.get(OMEMCONTACTCONVERSATION.TABLE);
        final Table contactTable = Table.get(MCOCONTACT.TABLE);
        final Collection<MetricsType> metricTypes = getMetricTypes(row, metricsType);
        final Long outboundId = dataContainer.getRow(getOutboundBulkMailTable(),
                (Long) row.get(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID)).get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final Long outboundConversationId = dataContainer.getRow(getOutboundMailTable(), outboundId).get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);

        if (contactConversationIds.contains(outboundConversationId)) {

            if (conversationIds.remove(outboundConversationId)) {
                final Row contactConversationRow = contactDataContainer.getRow(contactConversationsTable,
                        new WhereClause(Criteria.eq(contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), outboundConversationId)));
                final Row contactRow = contactDataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactTable.getPKColumn(),
                        contactConversationRow.get(OMEMCONTACTCONVERSATION.CONTACT_ID))));

                metricsInsights.add(new MetricsInsight(Contact.getFullName(contactRow.get(MCOCONTACT.FIRST_NAME), contactRow.get(MCOCONTACT.LAST_NAME)), metricTypes));
            }
        } else {
            final Row conversationRecipientsRow = dataContainer.getRow(conversationRecipientsTable,
                    new WhereClause(Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), outboundConversationId))
                            .and(Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                                    (int) AddressType.TO.getId())));

            if (Objects.nonNull(conversationRecipientsRow)) {
                final Long emailId = conversationRecipientsRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                final Row emailAddressRow = dataContainer.getRow(getemailAddressTable(), emailId);

                if (Objects.nonNull(emailAddressRow)) {
                    metricsInsights.add(new MetricsInsight(emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS), metricTypes));
                }
            }
        }
    }

    /**
     * Gets the {@link Collection} of {@link MetricsType}
     *
     * @param row         Represents the {@link Row} of outbound bulk mail
     * @param metricsType Represents the {@link MetricsType}
     * @return {@link Collection} of {@link MetricsType}
     */
    private Collection<MetricsType> getMetricTypes(final Row row, final MetricsType metricsType) {

        if (MetricsType.NOT_SENT.equals(metricsType)) {
            final MetricsType unsubscribeType = UNSUBSCRIBED;
            final MetricsType bouncedType = BOUNCED;
            final MetricsType noPrimaryEmailType = NO_PRIMARY_EMAIL;
            final Collection<MetricsType> metricTypes = new HashSet<>();

            if (0 < (int) row.get(unsubscribeType.name())) {
                metricTypes.add(unsubscribeType);
            }

            if (0 < (int) row.get(bouncedType.name())) {
                metricTypes.add(bouncedType);
            }

            if (0 < (int) row.get(noPrimaryEmailType.name())) {
                metricTypes.add(noPrimaryEmailType);
            }
            return metricTypes;
        } else {
            return List.of(metricsType);
        }
    }

    /**
     * Gets the {@link WhereClause} for unsent count
     *
     * @param outboundMetricsTable Represents the {@link Table} of outbound metrics
     * @return {@link WhereClause} of not sent
     */
    private WhereClause getUnsentMetricsQuery(final Table outboundMetricsTable) {
        return new WhereClause(Criteria.notEq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.UNSUBSCRIBED), 0))
                .or(Criteria.notEq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.BOUNCED), 0))
                .or(Criteria.notEq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.NO_PRIMARY_EMAIL), 0));
    }

    /**
     * Retrieves the count of opened without a reply for the specified criteria, referring to the table of outbound metrics.
     *
     * @param outboundMetricsTable Represents the {@link Table} of outbound metrics
     * @return {@link WhereClause} of opened with not replied
     */
    private WhereClause getOpenedNotRepliedWhereClause(final Table outboundMetricsTable) {
        return new WhereClause(Criteria.notEq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED), 0))
                .and(Criteria.eq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.REPLIED), 0));
    }

    /**
     * Retrieves the count of opened without a clicked for the specified criteria, referring to the table of outbound metrics.
     *
     * @param outboundMetricsTable Represents the {@link Table} of outbound metrics
     * @return {@link WhereClause} of opened with not clicked
     */
    private WhereClause getOpenedNotClickedWhereClause(final Table outboundMetricsTable) {
        return new WhereClause(Criteria.notEq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED), 0))
                .and(Criteria.eq(outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.CLICKED), 0));
    }

    /**
     * <p>
     * Updates the bulk outbound mail and bulk metrics for a given organization based on the specified metrics type
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param outboundId          Represents the outbound mail id
     * @param metricsType         Represents the {@link MetricsType} indicating the metrics type to update
     * @param targetContainer     Represents the  {@link DataContainer} which holds the updated information of bulk mail metrics
     * @param totalRecipientCount Represents the total recipient count
     */
    public void updateBulkMetrics(final long orgId, final long outboundId, final MetricsType metricsType,
                                  final DataContainer targetContainer, final long totalRecipientCount) {
        final Table bulkMetricTable = getTable();
        final Table outboundMetricsTable = getOutboundMetricsTable();
        final DataContainer entityContainer = getBulkMetrics(orgId, outboundId);

        if (!entityContainer.isEmpty()) {
            final Row outboundMetricsRow = entityContainer.getRow(outboundMetricsTable);
            final Row bulkMetricsRow = entityContainer.getRow(bulkMetricTable);

            if (Objects.nonNull(outboundMetricsRow) && Objects.nonNull(bulkMetricsRow)) {
                final TriConsumer<Row, Column, Boolean> metricsColumnHandler = (row, column, isBulkMetric) -> {
                    final String columnName = column.getColumnName();
                    final Counter counter = new Counter(row.get(columnName));

                    row.set(columnName, totalRecipientCount == 0 || isBulkMetric ? counter.incrementAndGet() : totalRecipientCount);
                };

                switch (metricsType) {
                    case CLICKED -> {
                        metricsColumnHandler.accept(outboundMetricsRow, outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.CLICKED), false);
                        metricsColumnHandler.accept(bulkMetricsRow, bulkMetricTable.getColumn(OMEMBULKEMAILMETRICS.CLICKED), true);
                    }
                    case OPENED -> {
                        metricsColumnHandler.accept(outboundMetricsRow, outboundMetricsTable.getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OPENED), false);
                        metricsColumnHandler.accept(bulkMetricsRow, bulkMetricTable.getColumn(OMEMBULKEMAILMETRICS.OPENED), true);
                    }
                }

                targetContainer.updateRow(outboundMetricsRow);
                targetContainer.updateRow(bulkMetricsRow);
            }
        }
    }

    /**
     * <p>
     * Retrieves the bulk metrics based on outbound message id for the given organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param outboundId Represents the outbound mail id
     * @return The {@link DataContainer}
     */
    private DataContainer getBulkMetrics(final long orgId, final long outboundId) {
        final Table bulkMailMetricsTable = getTable();
        final Table outboundBulkMetricsTable = getOutboundMetricsTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);

        selectQuery.addSelectClause(outboundBulkMetricsTable, bulkMailMetricsTable);
        selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getPKColumn(), outboundBulkMetricsTable
                        .getColumn(OMEMOUTBOUNDBULKEMAILMETRICS.OUTBOUND_BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID),
                        bulkMailMetricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }
}