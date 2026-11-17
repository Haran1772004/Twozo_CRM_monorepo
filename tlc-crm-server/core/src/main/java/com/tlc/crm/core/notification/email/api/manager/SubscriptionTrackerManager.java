package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.commons.Counter;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.notification.email.api.model.NotificationTrackingDetails;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailMetricsManager;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics;

import com.tlc.crm.core.sql.resource.OMEMCONTACTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKER;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKERLOG;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;

import com.tlc.crm.core.sql.resource.OMEMUNSUBSCRIBEEVENTTRACKER;
import com.tlc.crm.core.sql.resource.OMIVINVOICEEMAILCONVERSATION;
import com.tlc.crm.core.sql.resource.OMQNQUOTATIONEMAILCONVERSATION;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;

import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;

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
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics.MetricsType.UNSUBSCRIBED;

/**
 * <p>
 * Provides the additional functionalities for handling tracking, unsubscription. Designed to track
 * the unsubscription for the emails received by the user.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 * @see CoreMailManager
 */
public final class SubscriptionTrackerManager extends CoreMailManager implements SecureUserTaskProcessor {

    private final TemplateManager templateManager;
    private final EntityMailManager entityMailManager;
    private final BulkMailMetricsManager bulkMailMetricsManager;
    private final EmailManager emailManager;
    private final QuotationManager quotationManager;
    private final InvoiceManager invoiceManager;

    private static class Instance {
        private static final SubscriptionTrackerManager INSTANCE = new SubscriptionTrackerManager();
    }

    private SubscriptionTrackerManager() {
        super();
        this.templateManager = TemplateManager.getInstance();
        this.entityMailManager = EntityMailManager.getInstance();
        this.bulkMailMetricsManager = BulkMailMetricsManager.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.quotationManager = QuotationManager.getInstance();
        this.invoiceManager = InvoiceManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SubscriptionTrackerManager} Instance
     * </p>
     *
     * @return {@link SubscriptionTrackerManager}
     */
    public static SubscriptionTrackerManager getInstance() {
        return SubscriptionTrackerManager.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the outbound conversation tracker table
     * </p>
     *
     * @return The outbound conversation tracker table
     */
    private Table getOutboundConversationTrackerTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TABLE);
    }

    /**
     * <p>
     * Gets the outbound conversation tracker log table
     * </p>
     *
     * @return The outbound conversation tracker log table
     */
    private Table getOutboundConversationTrackerLogTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TABLE);
    }

    /**
     * <p>
     * Gets the outbound conversation tracker table
     * </p>
     *
     * @return The outbound conversation tracker table
     */
    private Table getTrackerTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TABLE);
    }

    /**
     * <p>
     * Gets the outbound conversation tracker log table
     * </p>
     *
     * @return The outbound conversation tracker log table
     */
    private Table getTrackerLogTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TABLE);
    }

    /**
     * <p>
     * Gets the email unsubscribe event tracker table
     * </p>
     *
     * @return The email unsubscribe event tracker table
     */
    private Table getUnsubscribeEventTrackerTable() {
        return Table.get(OMEMUNSUBSCRIBEEVENTTRACKER.TABLE);
    }

    /**
     * <p>
     * Gets the outbound email metrics table.
     * </p>
     *
     * @return The outbound email metrics table
     */
    private Table getOutboundEmailMetricsTable() {
        return Table.get(OMEMOUTBOUNDEMAILMETRICS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}.
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Gets the {@link EntityMailManager} instance.
     * </p>
     *
     * @return {@link EntityMailManager}
     */
    private EntityMailManager getEntityMailManager() {
        return Objects.isNull(entityMailManager) ? EntityMailManager.getInstance() : entityMailManager;
    }

    /**
     * <p>
     * Unsubscribes the email and contact based on the given organization, user id and in respective email address
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address
     */
    public void unsubscribe(final long orgId, final String emailAddress) {
        final DataContainer targetContainer = DataContainer.create();

        unsubscribe(orgId, emailAddress, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Unsubscribes the email and contact based on the given organization, user id and in respective email address
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param emailAddress    Represents the email address
     * @param targetContainer Holds the update information tobe stored into the db
     */
    public void unsubscribe(final long orgId, final String emailAddress, final DataContainer targetContainer) {
        final Table emailTable = getEmailAddressTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table contactConversationsTable = getContactConversationsTable();
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
        final Column emailPkColumn = emailTable.getPKColumn();
        final Column outboundPkColumn = outboundTable.getPKColumn();
        final Column conversationIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column templateOutboundIdColumn = outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID);
        final Column templateIdColumn = outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(emailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(emailPkColumn, conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), leftJoin),
                new JoinClause(conversationIdColumn, outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), leftJoin),
                new JoinClause(conversationIdColumn, contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), leftJoin),
                new JoinClause(outboundPkColumn, outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID), leftJoin)));
        selectQuery.addSelectClause(emailTable, outboundTemplateTable);
        selectQuery.addSelectClause(List.of(outboundPkColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row emailRow = dataContainer.getRow(emailTable);

        if (Objects.nonNull(emailRow)) {
            emailRow.set(emailTable.getColumn(OMEMADDRESS.HAS_UNSUBSCRIBED), true);
            targetContainer.updateRow(emailRow);

            final Row outboundRow = dataContainer.getRow(outboundTable);

            if (Objects.nonNull(outboundRow)) {
                final Row row = dataContainer.getRow(outboundTemplateTable, new WhereClause(Criteria.eq(
                        templateOutboundIdColumn, outboundRow.getPKValue())));

                if (Objects.nonNull(row)) {
                    templateManager.loadTemplateMetricsIntoContainer(orgId, row.get(templateIdColumn),
                            UNSUBSCRIBED, targetContainer);
                }
            }
        }
        getEntityMailManager().markContactAsUnsubscribed(orgId, emailAddress, targetContainer);
    }

    /**
     * <p>
     * Checks if any recipients are unsubscribed based on considering both email recipients and those associated with organisation contacts.
     * </p>
     *
     * @param orgId                    Represents an organization id
     * @param contactEmailRecipientIds Represents the {@link Collection} of contact email recipient ids
     * @param recipients               Represents the {@link Collection} of recipients
     * @return true, if the recipient is unsubscribed
     */
    public boolean hasUnsubscribedRecipients(final Long orgId, final Collection<Long> contactEmailRecipientIds,
                                             final Collection<String> recipients) {
        final Collection<String> emailAddresses = Objects.isNull(recipients) ? new HashSet<>() : new HashSet<>(recipients);

        if (Objects.nonNull(contactEmailRecipientIds) && !contactEmailRecipientIds.isEmpty()) {
            emailManager.get(orgId, contactEmailRecipientIds)
                    .stream()
                    .map(Email::getValue)
                    .forEach(emailAddresses::add);
        }

        if (hasUnsubscribedEmailAddress(orgId, emailAddresses)) {
            return true;
        }

        return Objects.nonNull(contactEmailRecipientIds) && !contactEmailRecipientIds.isEmpty() &&
                getEntityMailManager().hasUnsubscribedContact(orgId, contactEmailRecipientIds);
    }

    /**
     * <p>
     * Checks for the unsubscribed email addresses
     * </p>
     *
     * @param orgId          Represents the Organization ID
     * @param emailAddresses Represents the {@link Collection} of email addresses
     * @return true if contains unsubscribed emails
     */
    public boolean hasUnsubscribedEmailAddress(final Long orgId, final Collection<String> emailAddresses) {
        if (Objects.isNull(emailAddresses) || emailAddresses.isEmpty()) {
            return false;
        }
        final Table mailTable = getEmailAddressTable();
        final CountQuery countQuery = CountQuery.get(mailTable);

        countQuery.setWhereClause(new WhereClause(
                Criteria.in(mailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddresses))
                .and(Criteria.eq(mailTable.getColumn(OMEMADDRESS.HAS_UNSUBSCRIBED), true))
        );

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * Fetches the {@link Collection} of {@link EmailAddress} that have unsubscribed for any of the given contactEmailIds or emailAddresses.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param contactEmailIds Represents the {@link Collection} of contact email ids
     * @param emailAddresses  Represents the {@link Collection} of mail addresses
     * @return the {@link Collection} of unsubscribed {@link EmailAddress}
     */
    public Collection<EmailAddress> fetchUnsubscribedMailAddresses(final long orgId, final long userId,
                                                                   final Collection<Long> contactEmailIds, final Collection<String> emailAddresses) {
        final Collection<String> emails = new ArrayList<>();

        if (!emailAddresses.isEmpty()) {
            final Table mailTable = getEmailAddressTable();
            final SelectQuery selectQuery = SelectQuery.get(mailTable);

            selectQuery.addJoinClause(getEntityMailManager().getContactEmailJoinClauses(mailTable));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(mailTable.getColumn(OMEMADDRESS.HAS_UNSUBSCRIBED), true)));

            emails.addAll(getOrgDataStore(orgId).get(selectQuery).getRows(mailTable).map(mailRow -> (String) mailRow.get(OMEMADDRESS.EMAIL_ADDRESS)).collect(Collectors.toSet()));
        }

        if (!contactEmailIds.isEmpty()) {
            emails.addAll(contactManager.getContactEmailValues(orgId, userId, contactEmailIds));
        }

        return EmailRecipients.formatRecipients(emails.toString());
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link EmailTrackingDetail} mapped for the given conversation id
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param conversationId Represents the conversation id
     * @return {@link Collection} of {@link EmailTrackingDetail}
     */
    public Collection<EmailTrackingDetail> getEmailTrackingDetails(final long orgId, final Long conversationId) {
        final Table outboundConversationTrackerTable = getOutboundConversationTrackerTable();
        final Table outboundConversationTrackerLogTable = getOutboundConversationTrackerLogTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundConversationTrackerTable);

        selectQuery.addJoinClause(new JoinClause(outboundConversationTrackerTable.getPKColumn(),
                outboundConversationTrackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundConversationTrackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID), conversationId)));
        selectQuery.addSelectClause(List.of(outboundConversationTrackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE),
                outboundConversationTrackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME),
                outboundConversationTrackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Collection<EmailTrackingDetail> emailTrackingDetails = new ArrayList<>();

            dataContainer.getRows(outboundConversationTrackerTable).forEach(trackingRow -> {
                final Counter counter = new Counter();
                final EmailTrackingDetail emailTrackerDetail = new EmailTrackingDetail();

                dataContainer.getRows(outboundConversationTrackerLogTable,
                        new WhereClause(Criteria.eq(outboundConversationTrackerLogTable
                                .getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID), trackingRow.getPKValue()))).forEach(value -> {
                    emailTrackerDetail.setTime(value.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME));
                    counter.increment(1);
                });
                emailTrackerDetail.setCount((long) counter.getCount());
                emailTrackerDetail.setType(EmailTrackingDetail.Type.getType((short) trackingRow.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE)));

                emailTrackingDetails.add(emailTrackerDetail);
            });

            return emailTrackingDetails;
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Checks whether a tracker log present for the given tracking id.
     * </p>
     *
     * @param trackingId Represents the {@link String} of tracking id
     * @return true if it doesn't exist
     */
    public boolean hasTrackerLog(final String trackingId) {
        final Table trackerTable = getTrackerTable();
        final CountQuery countQuery = CountQuery.get(trackerTable);

        countQuery.addJoinClause(new JoinClause(trackerTable.getPKColumn(), getTrackerLogTable().getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));

        return getReadableDataStore().get(countQuery) > 0;
    }

    /**
     * Gets the conversation id for a given tracking id.
     *
     * @param trackingId The tracking id.
     * @return The conversation id.
     */
    public Long getConversationId(final String trackingId) {
        final Table trackerTable = getTrackerTable();
        final SelectQuery selectQuery = SelectQuery.get(trackerTable);

        selectQuery.addSelectClause(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));

        final Row row = getReadableDataStore().get(selectQuery).getRow(trackerTable);

        if (Objects.nonNull(row)) {
            return row.get(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID);
        }
        return null;
    }

    /**
     * <p>
     * Checks whether the given tracking id exists or not
     * </p>
     *
     * @param trackingId Represents the {@link String} of tracking id
     * @return true if it doesn't exist
     */
    public boolean trackingExists(final String trackingId) {
        final Table trackerTable = getTrackerTable();
        final CountQuery countQuery = CountQuery.get(trackerTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));

        return getReadableDataStore().get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link NotificationTrackingDetails} matched for the given {@link String} of tracking id
     * </p>
     *
     * @param trackingId Represents the {@link String} of tracking id
     * @return {@link NotificationTrackingDetails}
     */
    public NotificationTrackingDetails getNotificationTrackingDetails(final String trackingId) {
        final Table trackerTable = getTrackerTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(trackerTable);
        final Column trackerConversationIdColumn = trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID);

        selectQuery.addJoinClause(new JoinClause(trackerConversationIdColumn, conversationsTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT), trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.ORG_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE), trackerConversationIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Row trackerRow = dataContainer.getRow(trackerTable);

        return Objects.isNull(trackerRow) ? null : convertRowToNotificationTrackingDetails(trackerRow, dataContainer);
    }

    /**
     * <p>
     * Converts the {@link Row} of tracker table into {@link NotificationTrackingDetails}
     * </p>
     *
     * @param row           Represents the {@link Row} of tracker table
     * @param dataContainer Holds the updated information of the tracker and their related conversation details
     * @return {@link NotificationTrackingDetails}
     */
    private NotificationTrackingDetails convertRowToNotificationTrackingDetails(final Row row,
                                                                                final DataContainer dataContainer) {
        final Long conversationId = row.get(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID);
        final Table conversationsTable = getConversationsTable();
        final Row conversationRow = dataContainer.getRow(conversationsTable, new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)));
        final NotificationTrackingDetails notificationTrackingDetails = new NotificationTrackingDetails();

        if (Objects.nonNull(conversationRow)) {
            notificationTrackingDetails.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            notificationTrackingDetails.setUserId(conversationRow.get(OMEMCONVERSATIONS.USER_ID));
        }

        notificationTrackingDetails.setId(conversationId);
        notificationTrackingDetails.setOrgId(row.getOrgId());
        notificationTrackingDetails.setType(EmailTrackingDetail.Type.getType((short) row.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE)));

        return notificationTrackingDetails;
    }

    /**
     * <p>
     * Loads the email outbound conversation tracking details into container
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param conversationId      Represents the conversation id
     * @param emailTrackingDetail Represents the {@link EmailTrackingDetail} details
     * @param recipientIds        which holds the {@link Collection} of recipient ids
     * @param sourceContainer     Holds the existing records of tracking related details
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    public void loadEmailTrackingDetailsIntoContainer(final Long orgId, final Long conversationId, final EmailTrackingDetail emailTrackingDetail,
                                                      final Collection<Long> recipientIds, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTrackerTable();

        for (final Long recipientId : recipientIds) {
            final Row trackerRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table,
                    new WhereClause(Criteria.eq(table.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_RECIPIENT_ID), recipientId)));
            final Row row = Objects.isNull(trackerRow) ? new Row(table) : trackerRow;

            if (row.isNewRow()) {
                emailTrackingDetail.setId(row.getPKValue());
                row.setOrgId(orgId);
                row.set(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_RECIPIENT_ID, recipientId);
                row.set(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID, emailTrackingDetail.getTrackingId());
                row.set(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE, emailTrackingDetail.getType().id);
                row.set(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID, conversationId);
                targetContainer.addNewRow(row);
            }
        }
    }

    /**
     * <p>
     * Loads the email unsubscribe tracking details into container
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param emailTrackingDetail Represents the {@link EmailTrackingDetail}
     * @param conversationId      Represents the conversation id
     * @param emailIds            which holds the {@link Collection} of email ids
     * @param sourceContainer     Holds the existing records of tracking related details
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    public void loadUnsubscribeDetailsIntoContainer(final long orgId, final EmailTrackingDetail emailTrackingDetail, final Long conversationId,
                                                    final Collection<Long> emailIds, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getUnsubscribeEventTrackerTable();

        for (final Long emailId : emailIds) {
            final Row unsubscribeRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table,
                    new WhereClause(Criteria.eq(table.getColumn(OMEMUNSUBSCRIBEEVENTTRACKER.EMAIL_ID), emailId)));
            final Row row = Objects.isNull(unsubscribeRow) ? new Row(table) : unsubscribeRow;

            if (row.isNewRow() && Objects.nonNull(emailId)) {
                final Row currentRow = targetContainer.getRow(table,
                        new WhereClause(Criteria.eq(table.getColumn(OMEMUNSUBSCRIBEEVENTTRACKER.EMAIL_ID), emailId)));

                if (Objects.isNull(currentRow)) {
                    row.setOrgId(orgId);
                    row.set(OMEMUNSUBSCRIBEEVENTTRACKER.TRACKING_ID, emailTrackingDetail.getTrackingId());
                    row.set(OMEMUNSUBSCRIBEEVENTTRACKER.EMAIL_ID, emailId);
                    row.set(OMEMUNSUBSCRIBEEVENTTRACKER.CONVERSATION_ID, conversationId);
                    targetContainer.addNewRow(row);
                }
            }
        }
    }

    /**
     * <p>
     * Updates the email tracking details for the specified tracking type and tracking id
     * </p>
     *
     * @param trackingType Represents the {@link EmailTrackingDetail.Type}
     * @param trackingId   Represents the tracking id
     */
    public void updateTrackingDetails(final EmailTrackingDetail.Type trackingType, final String trackingId) {
        final Table trackingTable = getTrackerTable();
        final SelectQuery selectQuery = SelectQuery.get(trackingTable);
        final Table conversationsTable = getEntityMailManager().getConversationsTable();

        selectQuery.addSelectClause(trackingTable);
        selectQuery.addSelectClause(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID));
        selectQuery.addJoinClause(new JoinClause(trackingTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID), conversationsTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(trackingTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Stream<Row> trackingRows = dataContainer.getRows(trackingTable);
        final Row trackingRow = dataContainer.getRow(trackingTable);
        final Row conversationRow = dataContainer.getRow(conversationsTable);
        final long totalRecipientCount = trackingRows.count() - 1;

        if (Objects.nonNull(trackingRow) && Objects.nonNull(conversationRow)) {
            final Long orgId = trackingRow.getOrgId();
            final Table trackerLogTable = getTrackerLogTable();
            final Long trackerId = trackingRow.getPKValue();
            final CountQuery countQuery = CountQuery.get(trackerLogTable);

            countQuery.setWhereClause(new WhereClause(Criteria.eq(trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID), trackerId)));

            if (0 == getReadableDataStore().get(countQuery)) {
                final Long userId = conversationRow.get(OMEMCONVERSATIONS.USER_ID);
                final Long conversationId = trackingRow.get(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID);
                final Optional<Map<MessageDispatchType, Long>> messageDispatchTypeMap = getMessageDispatchType(orgId, conversationId);
                final Optional<Long> templateTrackingId = getTemplateTrackingId(orgId, trackerId, trackingTable);
                final DataContainer targetContainer = DataContainer.create();

                if (!hasTrackerLogs(orgId, trackerId)) {
                    final Map<MessageDispatchType, Long> messageDispatchType = messageDispatchTypeMap.orElseGet(() -> null);

                    if (Objects.nonNull(messageDispatchType)) {
                        secureProcess(orgId, userId, () -> messageDispatchType.forEach((type, id) -> {

                            switch (type) {
                                case BULK -> {

                                    switch (trackingType) {
                                        case OPENED -> {
                                            bulkMailMetricsManager.updateBulkMetrics(orgId, id, Metrics.MetricsType.OPENED, targetContainer, totalRecipientCount);
                                            templateTrackingId.ifPresent(templateId -> templateManager.loadTemplateMetricsIntoContainer(orgId,
                                                    templateId, TemplateMetrics.MetricsType.OPENED, targetContainer));
                                        }
                                        case CLICKED -> {
                                            bulkMailMetricsManager.updateBulkMetrics(orgId, id, Metrics.MetricsType.CLICKED, targetContainer, totalRecipientCount);
                                            templateTrackingId.ifPresent(templateId -> templateManager.loadTemplateMetricsIntoContainer(orgId,
                                                    templateId, TemplateMetrics.MetricsType.CLICKED, targetContainer));
                                        }
                                    }
                                }
                                case SINGLE -> {

                                    switch (trackingType) {
                                        case OPENED -> {
                                            updateOutboundMetrics(orgId, id, Metrics.MetricsType.OPENED, targetContainer, totalRecipientCount);
                                            templateTrackingId.ifPresent(templateId -> templateManager.loadTemplateMetricsIntoContainer(orgId,
                                                    templateId, TemplateMetrics.MetricsType.OPENED, targetContainer));
                                        }
                                        case CLICKED -> {
                                            updateOutboundMetrics(orgId, id, Metrics.MetricsType.CLICKED, targetContainer, totalRecipientCount);
                                            templateTrackingId.ifPresent(templateId -> templateManager.loadTemplateMetricsIntoContainer(orgId,
                                                    templateId, TemplateMetrics.MetricsType.CLICKED, targetContainer));
                                        }
                                    }
                                }
                            }
                        }));
                    }
                }
                loadConversationTrackerLogIntoContainer(orgId, trackerId, totalRecipientCount, targetContainer);
                getOrgDataStore(orgId).commitChanges(targetContainer);
            }
        }
    }

    /**
     * <p>
     * Updates the email unsubscribe details for the specified tracking id
     * </p>
     *
     * @param trackingId Represents the tracking id
     */
    public void updateUnsubscribeDetails(final String trackingId) {
        final Table table = getUnsubscribeEventTrackerTable();
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getColumn(OMEMUNSUBSCRIBEEVENTTRACKER.CONVERSATION_ID), conversationTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMUNSUBSCRIBEEVENTTRACKER.TRACKING_ID), trackingId)));
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Collection<Row> unsubscribeEventTrackerRows = dataContainer.getRows(table).collect(Collectors.toSet());

        if (!unsubscribeEventTrackerRows.isEmpty()) {
            final Row unsubscribeRow = unsubscribeEventTrackerRows.stream().findFirst().orElseGet(() -> null);
            final Row conversationRow = dataContainer.getRow(conversationTable, (Long) unsubscribeRow.get(OMEMUNSUBSCRIBEEVENTTRACKER.CONVERSATION_ID));
            final Long userId = conversationRow.get(OMEMCONVERSATIONS.USER_ID);
            final Long orgId = unsubscribeRow.getOrgId();


            secureProcess(orgId, userId, () -> {
                final DataContainer targetContainer = DataContainer.create();

                for (final Row unsubscribeEventTrackerRow : unsubscribeEventTrackerRows) {
                    final Long trackerId = unsubscribeEventTrackerRow.getPKValue();
                    final Long emailId = unsubscribeEventTrackerRow.get(OMEMUNSUBSCRIBEEVENTTRACKER.EMAIL_ID);

                    if (hasUnsubscribeTracker(orgId, trackerId, emailId)) {
                        final Optional<Long> templateTrackingId = getTemplateTrackingId(orgId, trackerId, table);
                        final Optional<EmailAddress> emailAddress = fetchEmailAddress(orgId, emailId);

                        final Long conversationId = unsubscribeEventTrackerRow.get(OMEMUNSUBSCRIBEEVENTTRACKER.CONVERSATION_ID);
                        final Optional<Map<MessageDispatchType, Long>> messageDispatchTypeMap = getMessageDispatchType(orgId, conversationId);
                        final Map<MessageDispatchType, Long> messageDispatchType = messageDispatchTypeMap.orElseGet(() -> null);

                        if (Objects.nonNull(messageDispatchType)) {

                            messageDispatchType.forEach((type, id) -> {

                                switch (type) {
                                    case BULK -> bulkMailMetricsManager.updateBulkMetrics(orgId, id,
                                            Metrics.MetricsType.UNSUBSCRIBED, targetContainer, 0);
                                }
                            });
                        }

                        templateTrackingId.ifPresent(templateId -> templateManager.loadTemplateMetricsIntoContainer(orgId, templateId,
                                TemplateMetrics.MetricsType.UNSUBSCRIBED, targetContainer));
                        emailAddress.ifPresent(address -> unsubscribe(orgId, address.getEmailAddress(), targetContainer));
                        targetContainer.updateRow(unsubscribeEventTrackerRow);
                    }
                }
                getOrgDataStore(orgId).commitChanges(targetContainer);
            });
        }
    }

    /**
     * <p>
     * Retrieves the email address for a given organization
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param emailId Represents the email id
     * @return The email address of the recipient
     */
    private Optional<EmailAddress> fetchEmailAddress(final long orgId, final Long emailId) {
        final Table addressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);
        final Column emailAddressColumn = addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);

        selectQuery.addSelectClause(emailAddressColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getPKColumn(), emailId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        }
        final Row addressRow = dataContainer.getRow(addressTable);

        return Optional.ofNullable(addressRow).map(row -> new EmailAddress(row.get(emailAddressColumn)));
    }

    /**
     * <p>
     * Checks if an unsubscribe tracker exists for the given organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param trackerId Represents the tracker id
     * @param emailId   Represents the email id
     * @return {@code True} if the tracker id exists for the given organization id
     */
    private boolean hasUnsubscribeTracker(final long orgId, final long trackerId, final long emailId) {
        final Table unsubscribeTrackerTable = getUnsubscribeEventTrackerTable();
        final Table emailAddressTable = getEmailAddressTable();
        final Column emailPkColumn = emailAddressTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(unsubscribeTrackerTable);

        countQuery.addJoinClause(new JoinClause(unsubscribeTrackerTable.getColumn(OMEMUNSUBSCRIBEEVENTTRACKER.EMAIL_ID),
                emailAddressTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(unsubscribeTrackerTable.getPKColumn(), trackerId))
                .and(Criteria.eq(emailPkColumn, emailId)).and(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.HAS_UNSUBSCRIBED), false)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Loads the conversation tracker log details into the container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param trackerId       Represents the tracker id that holds the tracking information
     * @param targetContainer Holds the updated information of email conversation tracking values
     */
    private void loadConversationTrackerLogIntoContainer(final long orgId, final long trackerId, final long totalRecipientCount, final DataContainer targetContainer) {

        for (int recipientCount = 0; recipientCount < totalRecipientCount; recipientCount++) {
            final Row row = new Row(getTrackerLogTable());

            row.set(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME, Instant.now().toEpochMilli());
            row.set(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID, trackerId);
            row.setOrgId(orgId);

            targetContainer.addNewRow(row);
        }
    }

    /**
     * <p>
     * Checks if there are any records in the tracker log table for the given tracker id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param trackerId Represents the tracker id
     * @return {@code True} if the tracker id exists for the given organization id
     */
    private boolean hasTrackerLogs(final long orgId, final long trackerId) {
        final Table trackerLogTable = getTrackerLogTable();
        final CountQuery countQuery = CountQuery.get(trackerLogTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID),
                trackerId)));

        return getOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves the template id associated with a given tracker id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param trackerId Represents the tracker id that holds the tracking information
     * @param table     Represents the tracker table
     * @return The template id
     */
    private Optional<Long> getTemplateTrackingId(final long orgId, final long trackerId, final Table table) {
        final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Column templateIdColumn = outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(templateIdColumn);
        selectQuery.addJoinClause(List.of(new JoinClause(table.getColumn(CoreEmailFields.CONVERSATION_ID.name()),
                outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)), new JoinClause(outboundTable.getPKColumn(),
                outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), trackerId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        } else {
            final Row templateRow = dataContainer.getRow(outboundTemplateTable);

            return Optional.ofNullable(templateRow).map(row -> row.get(templateIdColumn));
        }
    }

    /**
     * <p>
     * Retrieves the message dispatch type for a given conversation id in a specific organization
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return An Optional containing a map key as {@link MessageDispatchType} and value as outbound id
     */
    private Optional<Map<MessageDispatchType, Long>> getMessageDispatchType(final long orgId, final long conversationId) {
        final Table outboundTable = getOutboundEmailTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundTable);

        selectQuery.addSelectClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        }
        final Row outboundRow = dataContainer.getRow(outboundTable);

        return Optional.ofNullable(outboundRow).map(row -> {
            final Map<MessageDispatchType, Long> messageDispatchTypeMap = new EnumMap<>(MessageDispatchType.class);
            final Integer outboundType = row.get(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE);

            messageDispatchTypeMap.put(MessageDispatchType.get(outboundType.shortValue()), row.getPKValue());

            return messageDispatchTypeMap;
        });
    }

    /**
     * <p>
     * Updates the outbound mail metrics for a given organization based on the specified metrics type
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param outboundId      Represents the outbound mail id
     * @param metricsType     Represents the {@link Metrics.MetricsType} indicating the metrics type to update
     * @param targetContainer Represents the  {@link DataContainer} which holds the updated information of outbound mail metrics
     */
    private void updateOutboundMetrics(final long orgId, final long outboundId, final Metrics.MetricsType metricsType,
                                       final DataContainer targetContainer, final long trackingCount) {
        final Table outboundMetricsTable = getOutboundEmailMetricsTable();
        final DataContainer entityContainer = getOrgDataStore(orgId).get(outboundMetricsTable,
                new WhereClause(Criteria.eq(outboundMetricsTable.getColumn(OMEMOUTBOUNDEMAILMETRICS.OUTBOUND_ID), outboundId)));
        final Row outboundMetricsRow = entityContainer.isEmpty() ? new Row(outboundMetricsTable) : entityContainer.getRow(outboundMetricsTable);

        if (outboundMetricsRow.isNewRow()) {
            outboundMetricsRow.setOrgId(orgId);
            outboundMetricsRow.set(OMEMOUTBOUNDEMAILMETRICS.OUTBOUND_ID, outboundId);
        }
        final Consumer<Column> metricsColumnHandler = column -> {
            final String columnName = column.getColumnName();

            outboundMetricsRow.set(columnName, trackingCount);
        };

        switch (metricsType) {
            case OPENED -> metricsColumnHandler.accept(outboundMetricsTable.getColumn(OMEMOUTBOUNDEMAILMETRICS.OPENED));
            case CLICKED -> metricsColumnHandler.accept(outboundMetricsTable
                    .getColumn(OMEMOUTBOUNDEMAILMETRICS.CLICKED));
        }

        targetContainer.updateRow(outboundMetricsRow);
    }

    /**
     * <p>
     * Updates the status of the entity (Quotation or Invoice) associated with the given
     * email tracking ID from {@code PUBLISHED} to {@code VIEWED}.
     * </p>
     *
     * @param trackingId Represents the tracking ID used to identify the conversation
     */
    public void updateEntityStatusByTrackingId(final String trackingId) {
        final Table trackerTable = getTrackerTable();
        final Table quotationEmailConversationTable = Table.get(OMQNQUOTATIONEMAILCONVERSATION.TABLE);
        final Table invoiceEmailConversationTable = Table.get(OMIVINVOICEEMAILCONVERSATION.TABLE);
        final Column trackerConversationIdColumn = trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID);

        final SelectQuery selectQuery = SelectQuery.get(trackerTable);

        selectQuery.addJoinClause(new JoinClause(trackerConversationIdColumn,
                quotationEmailConversationTable.getColumn(OMQNQUOTATIONEMAILCONVERSATION.CONVERSATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(trackerConversationIdColumn,
                invoiceEmailConversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.CONVERSATION_ID), JoinClause.JoinType.LEFT));

        selectQuery.addSelectClause(List.of(
                trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.ORG_ID),
                quotationEmailConversationTable.getColumn(OMQNQUOTATIONEMAILCONVERSATION.QUOTATION_ID),
                quotationEmailConversationTable.getColumn(OMQNQUOTATIONEMAILCONVERSATION.USER_ID),
                invoiceEmailConversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID),
                invoiceEmailConversationTable.getColumn(OMIVINVOICEEMAILCONVERSATION.USER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TRACKING_ID), trackingId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Row trackerRow = dataContainer.getRow(trackerTable);

        if (Objects.isNull(trackerRow)) {
            return;
        }
        final Long orgId = trackerRow.getOrgId();
        final Row quotationEmailConversationRow = dataContainer.getRow(quotationEmailConversationTable);
        final Row invoiceEmailConversationRow = dataContainer.getRow(invoiceEmailConversationTable);

        if (Objects.nonNull(quotationEmailConversationRow)) {
            final Long quotationId = quotationEmailConversationRow.get(OMQNQUOTATIONEMAILCONVERSATION.QUOTATION_ID);
            final Long userId = quotationEmailConversationRow.get(OMQNQUOTATIONEMAILCONVERSATION.USER_ID);

            markViewedIfPublished(orgId, userId,
                    () -> quotationManager.getStatus(orgId, quotationId),
                    Quotation.Status.PUBLISHED,
                    targetContainer -> quotationManager.loadStatusUpdateIntoContainer(orgId, quotationId, Quotation.Status.VIEWED, targetContainer));
        } else if (Objects.nonNull(invoiceEmailConversationRow)) {
            final Long invoiceId = invoiceEmailConversationRow.get(OMIVINVOICEEMAILCONVERSATION.INVOICE_ID);
            final Long userId = invoiceEmailConversationRow.get(OMIVINVOICEEMAILCONVERSATION.USER_ID);

            markViewedIfPublished(orgId, userId,
                    () -> invoiceManager.getStatus(orgId, invoiceId),
                    Invoice.Status.PUBLISHED,
                    targetContainer -> invoiceManager.loadStatusUpdateIntoContainer(orgId, userId, invoiceId, Invoice.Status.VIEWED, targetContainer));
        }
    }

    /**
     * <p>
     * Shared status-transition logic: runs {@code secureProcess} for the given org/user,
     * checks whether the entity's current status equals {@code publishedStatus}, and if so,
     * builds a {@link DataContainer}, delegates the status-update population to
     * {@code loadIntoContainer}, and commits it — used by both the quotation and invoice
     * branches so the transition logic exists in exactly one place.
     * </p>
     *
     * @param orgId             the organization ID
     * @param userId            the user ID under whose context the update runs
     * @param statusSupplier    resolves the entity's current status
     * @param publishedStatus   the status value that must match for the update to proceed
     * @param loadIntoContainer populates the target {@link DataContainer} with the VIEWED status update
     * @param <S>               the status enum type (e.g. {@code Quotation.Status} or {@code Invoice.Status})
     */
    private <S> void markViewedIfPublished(final Long orgId, final Long userId, final Supplier<Optional<S>> statusSupplier,
                                           final S publishedStatus, final Consumer<DataContainer> loadIntoContainer) {
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);

        secureProcess(orgId, userId, () -> statusSupplier.get()
                .filter(status -> Objects.equals(status, publishedStatus))
                .ifPresent(status -> {
                    final DataContainer targetContainer = DataContainer.create();

                    loadIntoContainer.accept(targetContainer);

                    orgDataStore.commitChanges(targetContainer);
                }));
    }
}