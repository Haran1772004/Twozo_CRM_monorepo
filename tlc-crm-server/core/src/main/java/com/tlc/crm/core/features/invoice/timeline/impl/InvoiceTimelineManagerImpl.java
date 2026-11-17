package com.tlc.crm.core.features.invoice.timeline.impl;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.timeline.InvoiceTimelineManager;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.core.sql.resource.MTLINVOICETIMELINE;
import com.tlc.crm.core.sql.resource.OMIVINVOICE;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.InvoiceTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * Manages invoice-related activity timeline entries, posting events to all associated
 * contact, company, and deal timelines.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceTimelineManagerImpl implements InvoiceTimelineManager {

    private static final String INVOICE_NUMBER_FORMAT = "INV-%03d";
    private static final I18nKey INVOICE_STATUS_FIELD = new I18nKey("i18n.invoice.status");

    private final CommonTimelineManager commonTimelineManager;
    private final UserService userService;

    private InvoiceTimelineManagerImpl() {
        this.commonTimelineManager = CommonTimelineManager.getInstance();
        this.userService = UserService.getInstance();
    }

    private static final class InstanceHolder {
        private static final InvoiceTimelineManagerImpl INSTANCE = new InvoiceTimelineManagerImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link InvoiceTimelineManagerImpl}.
     * </p>
     *
     * @return {@link InvoiceTimelineManagerImpl} instance.
     */
    public static InvoiceTimelineManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    private Table getTable() {
        return Table.get(MTLINVOICETIMELINE.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInvoiceTimelineIntoContainer(final Invoice invoice, final boolean isCreateEvent,
                                                 final Invoice.Status previousStatus, final Long actorUserId,
                                                 final DataContainer targetContainer) {
        final Optional<Timeline> timelineOptional = buildTimeline(invoice, isCreateEvent, previousStatus, actorUserId);

        if (timelineOptional.isEmpty()) {
            return;
        }
        final Timeline timeline = timelineOptional.get();
        final Row timelineRow = new Row(commonTimelineManager.getTable());

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, timelineRow, targetContainer);
        final Long timelineId = timeline.id();
        final Row invoiceTimelineRow = new Row(getTable());

        invoiceTimelineRow.set(MTLINVOICETIMELINE.INVOICE_ID, invoice.id());
        invoiceTimelineRow.set(MTLINVOICETIMELINE.TIMELINE_ID, timelineId);
        targetContainer.addNewRow(invoiceTimelineRow);

        postToEntityTimelines(invoice, timelineId, targetContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, EntityTimelines> get(final long orgId, final Collection<Long> timelineIds) {
        if (timelineIds.isEmpty()) {
            return Map.of();
        }
        final Table invoiceTimelineTable = getTable();
        final Table invoiceTable = Table.get(OMIVINVOICE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(invoiceTimelineTable);

        selectQuery.addJoinClause(new JoinClause(invoiceTimelineTable.getColumn(MTLINVOICETIMELINE.INVOICE_ID),
                invoiceTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(
                invoiceTimelineTable.getColumn(MTLINVOICETIMELINE.TIMELINE_ID),
                invoiceTimelineTable.getColumn(MTLINVOICETIMELINE.INVOICE_ID),
                invoiceTable.getColumn(OMIVINVOICE.NUMBER)
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(invoiceTimelineTable.getColumn(MTLINVOICETIMELINE.TIMELINE_ID), timelineIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, EntityTimelines> entityTimelinesMap = new HashMap<>();

        dataContainer.getRows(invoiceTimelineTable).forEach(invoiceTimelineRow -> {
            final Long timelineId = invoiceTimelineRow.get(MTLINVOICETIMELINE.TIMELINE_ID);
            final Long invoiceId = invoiceTimelineRow.get(MTLINVOICETIMELINE.INVOICE_ID);
            final Row invoiceRow = dataContainer.getRow(invoiceTable, invoiceId);

            if (Objects.nonNull(invoiceRow)) {
                final InvoiceTimeline invoiceTimeline = new InvoiceTimeline();
                final EntityTimelines entityTimelines = new EntityTimelines();
                final Integer number = invoiceRow.get(OMIVINVOICE.NUMBER);

                invoiceTimeline.setId(invoiceId);

                if (Objects.nonNull(number)) {
                    invoiceTimeline.setNumber(String.format(INVOICE_NUMBER_FORMAT, number));
                }

                entityTimelines.set(invoiceTimeline);
                entityTimelinesMap.put(timelineId, entityTimelines);
            }
        });

        return entityTimelinesMap;
    }

    /**
     * <p>
     * Builds the {@link Timeline} to record for an invoice create or status-change event. Returns an
     * empty {@link Optional} when the event is not timeline-worthy, e.g. a create event for an invoice
     * that is not created in draft status, or a status change where the status has not actually changed.
     * </p>
     *
     * @param invoice        Holds the invoice attributes.
     * @param isCreateEvent  Whether this is a create event.
     * @param previousStatus The status before the change; {@code null} on create.
     * @param actorUserId    The user who performed the action.
     * @return {@link Optional} holding the {@link Timeline} to persist, or empty if no timeline entry is required.
     */
    private Optional<Timeline> buildTimeline(final Invoice invoice, final boolean isCreateEvent,
                                             final Invoice.Status previousStatus, final Long actorUserId) {
        final Invoice.Status newStatus = invoice.getStatus();
        final String invoiceNumber = invoice.getFullyQualifiedNumber();

        if (isCreateEvent) {
            if (Invoice.Status.DRAFT == newStatus) {
                return Optional.of(buildDraftTimeline(invoice, actorUserId, invoiceNumber));
            }

            return Optional.empty();
        }

        if (Objects.isNull(previousStatus) || previousStatus == newStatus) {
            return Optional.empty();
        }

        return Optional.of(buildStatusUpdatedTimeline(invoice, actorUserId, invoiceNumber, previousStatus, newStatus));
    }

    /**
     * <p>
     * Builds the {@link Timeline} recorded when an invoice is created directly in draft status.
     * </p>
     *
     * @param invoice       Holds the invoice attributes.
     * @param actorUserId   The user who performed the action.
     * @param invoiceNumber The fully qualified invoice number to record on the timeline.
     * @return {@link Timeline} for the draft-creation event.
     */
    private Timeline buildDraftTimeline(final Invoice invoice, final Long actorUserId, final String invoiceNumber) {
        final Timeline timeline = buildBaseTimeline(invoice, actorUserId);

        timeline.setAction(TimelineActionFields.DRAFT);
        timeline.setTimelineType(TimelineType.INVOICE_DRAFT);
        setInvoiceEntityTimeline(timeline, invoice.id(), invoiceNumber);

        return timeline;
    }

    /**
     * <p>
     * Builds the {@link Timeline} recorded when an invoice's status changes, capturing the
     * previous and new status as a {@link FieldTimeline}.
     * </p>
     *
     * @param invoice        Holds the invoice attributes.
     * @param actorUserId    The user who performed the action.
     * @param invoiceNumber  The fully qualified invoice number to record on the timeline.
     * @param previousStatus The status before the change.
     * @param newStatus      The status after the change.
     * @return {@link Timeline} for the status-change event.
     */
    private Timeline buildStatusUpdatedTimeline(final Invoice invoice, final Long actorUserId, final String invoiceNumber,
                                                final Invoice.Status previousStatus, final Invoice.Status newStatus) {
        final Timeline timeline = buildBaseTimeline(invoice, actorUserId);
        final I18nResolver i18nResolver = getI18nResolver(invoice, actorUserId);

        timeline.setAction(TimelineActionFields.STATUS_UPDATED);
        timeline.setTimelineType(TimelineType.INVOICE_STATUS_UPDATED);
        timeline.setFieldTimeline(new FieldTimeline(
                List.of(i18nResolver.get(previousStatus.getI18nKey())),
                i18nResolver.get(newStatus.getI18nKey()),
                INVOICE_STATUS_FIELD));
        setInvoiceEntityTimeline(timeline, invoice.id(), invoiceNumber);

        return timeline;
    }

    /**
     * <p>
     * Resolves an {@link I18nResolver} for the actor who performed the invoice timeline action.
     * </p>
     *
     * @param invoice     Holds the invoice attributes.
     * @param actorUserId Represents the user who performed the action.
     * @return {@link I18nResolver} configured with the actor's regional language.
     */
    private I18nResolver getI18nResolver(final Invoice invoice, final Long actorUserId) {
        final CrmUser actor = resolveActor(invoice, actorUserId);
        final UserRegionalPreference userPreference = actor.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return I18nServiceProvider.get().getResolver(Locale.forLanguageTag(userPreference.getLanguage().getName()));
    }

    /**
     * <p>
     * Resolves the actor from the invoice's already-loaded user references when possible,
     * falling back to a user lookup only when required.
     * </p>
     *
     * @param invoice     Holds the invoice attributes.
     * @param actorUserId Represents the user who performed the action.
     * @return {@link CrmUser} who performed the action.
     */
    private CrmUser resolveActor(final Invoice invoice, final Long actorUserId) {
        final CrmUser updatedBy = invoice.getUpdatedBy();

        if (Objects.nonNull(updatedBy) && Objects.equals(updatedBy.id(), actorUserId)) {
            return updatedBy;
        }
        final CrmUser createdBy = invoice.getCreatedBy();

        if (Objects.nonNull(createdBy) && Objects.equals(createdBy.id(), actorUserId)) {
            return createdBy;
        }
        final CrmUser owner = invoice.getOwner();

        if (Objects.nonNull(owner) && Objects.equals(owner.id(), actorUserId)) {
            return owner;
        }

        return userService.getUser(invoice.orgId(), actorUserId);
    }

    /**
     * <p>
     * Builds the common {@link Timeline} attributes shared by every invoice timeline event
     * (organization, owner, audited time, category), leaving the action-specific fields
     * to be set by the caller.
     * </p>
     *
     * @param invoice     Holds the invoice attributes.
     * @param actorUserId The user who performed the action.
     * @return {@link Timeline} populated with the shared, action-independent attributes.
     */
    private Timeline buildBaseTimeline(final Invoice invoice, final Long actorUserId) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(invoice.orgId());
        timeline.setOwnerId(actorUserId);
        timeline.setAuditedTime(Objects.nonNull(invoice.getModifiedAt()) ? invoice.getModifiedAt() : new DateTime(Instant.now().toEpochMilli()));
        timeline.setCategory(Timeline.Category.INVOICE);

        return timeline;
    }

    /**
     * <p>
     * Attaches the {@link InvoiceTimeline} entity reference to the given timeline.
     * </p>
     *
     * @param timeline      Holds the timeline being built.
     * @param invoiceId     Represents the invoice id.
     * @param invoiceNumber The fully qualified invoice number to record on the timeline.
     */
    private void setInvoiceEntityTimeline(final Timeline timeline, final Long invoiceId, final String invoiceNumber) {
        final InvoiceTimeline invoiceTimeline = new InvoiceTimeline();

        invoiceTimeline.setId(invoiceId);
        invoiceTimeline.setNumber(invoiceNumber);
        timeline.setEntityTimeline(invoiceTimeline);
    }

    /**
     * <p>
     * Posts the invoice timeline entry to every related contact, company and deal timeline. Each
     * related-entity type shares the same "collect ids, then write one junction row per id" shape,
     * so both steps are factored into single reusable helpers ({@link #collectEntityId} and
     * {@link #postEntityTimeline}) instead of being repeated per entity type.
     * </p>
     *
     * @param invoice         Holds the invoice attributes, including the related contact, company and deal.
     * @param timelineId      Represents the timeline id being posted.
     * @param targetContainer Holds the updated information to be persisted.
     */
    private void postToEntityTimelines(final Invoice invoice, final Long timelineId, final DataContainer targetContainer) {
        final ReadableTimelineManager readableTimelineManager = ReadableTimelineManager.getInstance();
        final Collection<Long> contactIds = new HashSet<>();
        final Collection<Long> companyIds = new HashSet<>();
        final Collection<Long> dealIds = new HashSet<>();

        collectEntityId(invoice.getContact(), Contact::id, contactIds);
        collectEntityId(invoice.getCompany(), Company::id, companyIds);
        collectEntityId(invoice.getDeal(), Deal::id, dealIds);

        postEntityTimeline(contactIds, timelineId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                readableTimelineManager.getContactTimelineTable(), targetContainer);
        postEntityTimeline(companyIds, timelineId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                readableTimelineManager.getCompanyTimelineTable(), targetContainer);
        postEntityTimeline(dealIds, timelineId, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                readableTimelineManager.getDealTimelineTable(), targetContainer);
    }

    /**
     * <p>
     * Records the id of a related entity for batched timeline posting, skipping {@code null} entities
     * and {@code null} ids. Replaces the previous per-type ({@code Contact}/{@code Company}/{@code Deal})
     * overloads with a single generic implementation.
     * </p>
     *
     * @param entity      Represents the related entity; may be {@code null}.
     * @param idExtractor Extracts the id from the entity.
     * @param ids         Holds the accumulated, deduplicated entity ids.
     */
    private <T> void collectEntityId(final T entity, final Function<T, Long> idExtractor, final Collection<Long> ids) {
        if (Objects.nonNull(entity)) {
            final Long id = idExtractor.apply(entity);

            if (Objects.nonNull(id)) {
                ids.add(id);
            }
        }
    }

    /**
     * <p>
     * Writes one junction row per related-entity id, linking it to the given timeline. Shared by
     * contact, company and deal timeline posting so the write loop only needs to exist once.
     * </p>
     *
     * @param entityIds        Represents the related entity ids to link.
     * @param timelineId       Represents the timeline id being posted.
     * @param entityIdColumn   Represents the junction table's entity id column.
     * @param timelineIdColumn Represents the junction table's timeline id column.
     * @param table            Represents the junction table for this entity type.
     * @param targetContainer  Holds the updated information to be persisted.
     */
    private void postEntityTimeline(final Collection<Long> entityIds, final Long timelineId, final String entityIdColumn,
                                    final String timelineIdColumn, final Table table, final DataContainer targetContainer) {
        for (final Long entityId : entityIds) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, entityIdColumn, timelineIdColumn,
                    new Row(table), targetContainer);
        }
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore} for the organization.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }
}