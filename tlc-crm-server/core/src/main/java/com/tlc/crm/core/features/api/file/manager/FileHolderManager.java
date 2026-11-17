package com.tlc.crm.core.features.api.file.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.core.sql.resource.OMFICOMPANYFILE;
import com.tlc.crm.core.sql.resource.OMFICONTACTFILE;
import com.tlc.crm.core.sql.resource.OMFIDEALFILE;
import com.tlc.crm.entity.sql.resource.MTLATTACHMENTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.crm.entity.attachment.api.manager.AttachmentConfigManager;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Stores the entity attachment details into the database.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileHolderManager extends AttachmentConfigManager<FileHolder> {

    private final CommonTimelineManager commonTimelineManager;
    private final ReadableTimelineManager readableTimelineManager;
    private final ModificationHandler modificationHandler;

    private static class Instance {
        private static final FileHolderManager INSTANCE = new FileHolderManager();
    }

    private FileHolderManager() {
        commonTimelineManager = CommonTimelineManager.getInstance();
        readableTimelineManager = ReadableTimelineManager.getInstance();
        modificationHandler = ModificationHandler.getInstance();
    }

    /**
     * Gets the {@link Instance}.
     *
     * @return {@link Instance}.
     */
    public static FileHolderManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the {@link Table} of DealFile.
     *
     * @return {@link Table}.
     */
    public Table getDealFileTable() {
        return Table.get(OMFIDEALFILE.TABLE);
    }

    /**
     * Returns the {@link Table} of contact file.
     *
     * @return {@link Table}.
     */
    public Table getContactFileTable() {
        return Table.get(OMFICONTACTFILE.TABLE);
    }

    /**
     * Returns the {@link Table} of company file.
     *
     * @return {@link Table}.
     */
    public Table getCompanyFileTable() {
        return Table.get(OMFICOMPANYFILE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileHolder Represents the {@link FileHolder} details.
     */
    @Override
    public Map<String, Object> save(final FileHolder fileHolder) {
        final Map<String, Object> response = new HashMap<>();
        final List<Long> savedRecords = new ArrayList<>();
        final List<Map<String, Object>> failedEntries = new ArrayList<>();

        try {
            final DataContainer dataContainer = DataContainer.create();

            loadFileHolderDetailsIntoContainer(fileHolder, dataContainer);
            orgDataStore(fileHolder.orgId()).commitChanges(dataContainer);
            savedRecords.addAll(fileHolder.getAttachments().stream().map(Attachment::id).filter(Objects::nonNull).toList());
        } catch (ErrorCode errorCode) {
            final Map<String, Object> failedEntry = new HashMap<>();

            failedEntry.put(AttachmentFields.REASON.getName(), errorCode.getMessage());
            failedEntries.add(failedEntry);
        }

        response.put(AttachmentFields.UPLOADED_RECORDS.getName(), savedRecords);
        response.put(AttachmentFields.FAILED_RECORDS.getName(), failedEntries);

        return response;
    }

    /**
     * Loads the file upload details into the data container.
     *
     * @param fileHolder    An entity that holds the file upload attributes.
     * @param dataContainer Holds the updated information of user defined and system field values.
     */
    private void loadFileHolderDetailsIntoContainer(final FileHolder fileHolder, final DataContainer dataContainer) {
        final Map<String, Long> relatedIds = fileHolder.getRelatedEntityIds();
        final Long contactId = relatedIds.get(Contact.ENTITY_TYPE);
        final Long dealId = relatedIds.get(Deal.ENTITY_TYPE);
        final Long companyId = relatedIds.get(Company.ENTITY_TYPE);

        for (final EntityAttachment attachment : fileHolder.getAttachments()) {

            if (Objects.nonNull(contactId)) {
                loadContactFileDetailsIntoContainer(contactId, attachment, dataContainer);
            }

            if (Objects.nonNull(dealId)) {
                loadDealFileDetailsIntoContainer(dealId, attachment, dataContainer);
            }

            if (Objects.nonNull(companyId)) {
                loadCompanyFileDetailsIntoContainer(companyId, attachment, dataContainer);
            }

            loadAttachmentTimeline(buildAttachmentTimeline(attachment), fileHolder, attachment.id(), dataContainer);
        }
    }

    /**
     * Loads the contact file details into container.
     *
     * @param contactId     Refers the contact id.
     * @param attachment    Refers the {@link Attachment} attributes.
     * @param dataContainer Holds the updated information of file holder field values.
     */
    private void loadContactFileDetailsIntoContainer(final Long contactId, final EntityAttachment attachment,
                                                     final DataContainer dataContainer) {
        final Row contactFileRow = new Row(getContactFileTable());

        contactFileRow.set(OMFICONTACTFILE.CONTACT_ID, contactId);
        contactFileRow.set(OMFICONTACTFILE.ATTACHMENT_ID, attachment.id());
        contactFileRow.set(OMFICONTACTFILE.USER_ID, attachment.getOwnerId());

        dataContainer.addNewRow(contactFileRow);
    }

    /**
     * Loads the deal file details into container.
     *
     * @param dealId        Refers the deal id.
     * @param attachment    Refers the {@link Attachment} attributes.
     * @param dataContainer Holds the updated information of user defined and system field values.
     */
    private void loadDealFileDetailsIntoContainer(final Long dealId, final EntityAttachment attachment,
                                                  final DataContainer dataContainer) {
        final Row dealFileRow = new Row(getDealFileTable());
        final Long ownerId = attachment.getOwnerId();

        dealFileRow.set(OMFIDEALFILE.DEAL_ID, dealId);
        dealFileRow.set(OMFIDEALFILE.ATTACHMENT_ID, attachment.id());
        dealFileRow.set(OMFIDEALFILE.USER_ID, ownerId);

        dataContainer.addNewRow(dealFileRow);
        modificationHandler.loadModificationDetailsIntoContainer(attachment.orgId(), ownerId, List.of(dealId), Source.DEAL, dataContainer);
    }

    /**
     * Loads the company file details into container.
     *
     * @param companyId     Refers the company id.
     * @param attachment    Refers the {@link Attachment} attributes.
     * @param dataContainer Holds the updated information of file holder field values.
     */
    private void loadCompanyFileDetailsIntoContainer(final Long companyId, final EntityAttachment attachment,
                                                     final DataContainer dataContainer) {
        final Row companyFileRow = new Row(getCompanyFileTable());

        companyFileRow.set(OMFICOMPANYFILE.COMPANY_ID, companyId);
        companyFileRow.set(OMFICOMPANYFILE.ATTACHMENT_ID, attachment.id());
        companyFileRow.set(OMFICOMPANYFILE.USER_ID, attachment.getOwnerId());

        dataContainer.addNewRow(companyFileRow);
    }

    /**
     * Retrieves the {@link Collection} of contact file ids for given contact id.
     *
     * @param orgId     Organization Id.
     * @param contactId Contact Id.
     * @param start     Start value.
     * @param limit     Limit value.
     * @return Collection of files.
     */
    public final Collection<EntityAttachment> getContactEntityAttachments(final Long orgId, final long contactId, final int start, final int limit) {
        final Table contactFileTable = getContactFileTable();

        return getEntityAttachments(orgId, contactFileTable, getContactFileCondition(contactId, contactFileTable), start, limit);
    }

    /**
     * Retrieves the {@link Collection} of deal file ids for given deal id.
     *
     * @param orgId  Organization id.
     * @param dealId Deal id.
     * @param start  Start value.
     * @param limit  Limit value.
     * @return Collection of files.
     */
    public final Collection<EntityAttachment> getDealEntityAttachments(final Long orgId, final long dealId, final int start, final int limit) {
        final Table dealFileTable = getDealFileTable();

        return getEntityAttachments(orgId, dealFileTable, getDealFileCondition(dealId, dealFileTable), start, limit);
    }

    /**
     * Retrieves the {@link Collection} of company file ids for given company id.
     *
     * @param orgId     Refers the organization id.
     * @param companyId Refers the company id.
     * @param start     Refers the start value.
     * @param limit     Refers the limit value.
     * @return Collection of files.
     */
    public final Collection<EntityAttachment> getCompanyEntityAttachments(final Long orgId, final long companyId, final int start, final int limit) {
        final Table companyFileTable = getCompanyFileTable();

        return getEntityAttachments(orgId, companyFileTable, getCompanyFileCondition(companyId, companyFileTable), start, limit);

    }

    /**
     * Retrieves a collection of attachment IDs for a given organization, table, and where clause.
     *
     * @param orgId       The ID of the organization for which to retrieve attachment IDs
     * @param table       The table from which to retrieve the attachment IDs.
     * @param whereClause The where clause to filter the results.
     * @param start       The starting index for the results.
     * @param limit       The maximum number of results to retrieve.
     * @return The collection of attachment IDs that match the specified criteria.
     */
    private Collection<EntityAttachment> getEntityAttachments(final long orgId, final Table table,
                                                              final WhereClause whereClause, final int start, final int limit) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn(), OrderByClause.OrderType.DESCENDING));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final EntityAttachment entityAttachment = new EntityAttachment();

            entityAttachment.setId(row.get(OMFICONTACTFILE.ATTACHMENT_ID));
            entityAttachment.setOrgId(orgId);
            entityAttachment.setOwnerId(row.get(OMFICONTACTFILE.USER_ID));

            return entityAttachment;
        }).toList();
    }

    /**
     * Returns the {@link WhereClause} for the given contact id.
     *
     * @param contactId        Contact Id.
     * @param contactFileTable Contact file table.
     * @return {@link WhereClause} to retrieve contact files.
     */
    private WhereClause getContactFileCondition(final long contactId, final Table contactFileTable) {
        return new WhereClause(Criteria.eq(contactFileTable.getColumn(OMFICONTACTFILE.CONTACT_ID), contactId));
    }

    /**
     * Returns the {@link WhereClause} for the given deal id.
     *
     * @param dealId        Deal Id.
     * @param dealFileTable Deal file table.
     * @return {@link WhereClause} to retrieve deal files.
     */
    private WhereClause getDealFileCondition(final long dealId, final Table dealFileTable) {
        return new WhereClause(Criteria.eq(dealFileTable.getColumn(OMFIDEALFILE.DEAL_ID), dealId));
    }

    /**
     * Returns the {@link WhereClause} for the given company id.
     *
     * @param companyId        Refers the company id.
     * @param companyFileTable Refers the company file table.
     * @return {@link WhereClause} to retrieve company files.
     */
    private WhereClause getCompanyFileCondition(final long companyId, final Table companyFileTable) {
        return new WhereClause(Criteria.eq(companyFileTable.getColumn(OMFICOMPANYFILE.COMPANY_ID), companyId));
    }

    /**
     * Returns the count of contact file records that matches the given details.
     *
     * @param orgId     Organization Id.
     * @param contactId Contact Id.
     * @return Count of contact file records.
     */
    public final Long getContactFilesCount(final Long orgId, final long contactId) {
        final Table contactFileTable = getContactFileTable();
        final CountQuery countQuery = CountQuery.get(contactFileTable);

        countQuery.setWhereClause(getContactFileCondition(contactId, contactFileTable));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Returns the count of deal file records that matches the given details.
     *
     * @param orgId  Organization Id.
     * @param dealId Deal Id.
     * @return Count of deal file records.
     */
    public final Long getDealFilesCount(final Long orgId, final long dealId) {
        final Table dealFileTable = getDealFileTable();
        final CountQuery countQuery = CountQuery.get(dealFileTable);

        countQuery.setWhereClause(getDealFileCondition(dealId, dealFileTable));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Returns the count of company file records that matches the given details.
     *
     * @param orgId     Refers the organization id.
     * @param companyId Refers the company id.
     * @return Count of company file records.
     */
    public final Long getCompanyFilesCount(final Long orgId, final long companyId) {
        final Table companyFileTable = getCompanyFileTable();
        final CountQuery countQuery = CountQuery.get(companyFileTable);

        countQuery.setWhereClause(getCompanyFileCondition(companyId, companyFileTable));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Builds the attachment timeline when the attachment is uploaded.
     * </p>
     *
     * @param attachment An entity that holds the attachment attributes.
     * @return {@link Timeline}.
     */
    private Timeline buildAttachmentTimeline(final EntityAttachment attachment) {
        final Timeline timeline = new Timeline();

        timeline.setCategory(Timeline.Category.FILE);
        timeline.setAction(TimelineActionFields.UPLOADED);
        timeline.setOwnerId(attachment.getOwnerId());
        timeline.setOrgId(attachment.orgId());
        timeline.setTimelineType(TimelineType.FILE_UPLOADED);
        timeline.setAuditedTime(attachment.getCreatedTime());

        return timeline;
    }

    /**
     * <p>
     * Loads the attachment related timeline.
     * </p>
     *
     * @param timeline      An entity that holds the timeline attributes.
     * @param fileHolder    An entity that holds the file upload attributes.
     * @param attachmentId  Refers the attachment id.
     * @param dataContainer The container which holds the attachment details.
     */
    private void loadAttachmentTimeline(final Timeline timeline, final FileHolder fileHolder,
                                        final Long attachmentId, final DataContainer dataContainer) {
        final Row attachmentTimelineRow = new Row(Table.get(MTLATTACHMENTTIMELINE.TABLE));
        final Map<String, Long> relatedIds = fileHolder.getRelatedEntityIds();
        final Long contactId = relatedIds.get(Contact.ENTITY_TYPE);
        final Long dealId = relatedIds.get(Deal.ENTITY_TYPE);
        final Long companyId = relatedIds.get(Company.ENTITY_TYPE);

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), dataContainer);
        final Long timelineId = timeline.id();

        attachmentTimelineRow.set(MTLATTACHMENTTIMELINE.ATTACHMENT_ID, attachmentId);
        attachmentTimelineRow.set(MTLATTACHMENTTIMELINE.TIMELINE_ID, timelineId);
        dataContainer.addNewRow(attachmentTimelineRow);

        if (Objects.nonNull(contactId)) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contactId, MTLCONTACTTIMELINE.CONTACT_ID,
                    MTLCONTACTTIMELINE.TIMELINE_ID, new Row(readableTimelineManager.getContactTimelineTable()), dataContainer);
        }

        if (Objects.nonNull(dealId)) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, dealId, MTLDEALTIMELINE.DEAL_ID,
                    MTLDEALTIMELINE.TIMELINE_ID, new Row(readableTimelineManager.getDealTimelineTable()), dataContainer);
        }

        if (Objects.nonNull(companyId)) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID,
                    MTLCOMPANYTIMELINE.TIMELINE_ID, new Row(readableTimelineManager.getCompanyTimelineTable()), dataContainer);
        }
    }

    /**
     * <p>
     * Gets the entity id based on the attachment id
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param attachmentId Represents the attachment id
     * @return The map source as a key and entity id as a value
     */
    public Map<Source, Pair<Long, Long>> getEntityUserMap(final long orgId, final Long attachmentId) {
        final Map<Source, Pair<Long, Long>> entityUserIdMap = new HashMap<>();
        final Pair<Long, Long> contactUserIdPair = getEntityUserIdPair(orgId, attachmentId, getContactFileTable(), OMFICONTACTFILE.ATTACHMENT_ID, OMFICONTACTFILE.CONTACT_ID);
        final Pair<Long, Long> companyUserIdPair = getEntityUserIdPair(orgId, attachmentId, getCompanyFileTable(), OMFICOMPANYFILE.ATTACHMENT_ID, OMFICOMPANYFILE.COMPANY_ID);
        final Pair<Long, Long> dealUserIdPair = getEntityUserIdPair(orgId, attachmentId, getDealFileTable(), OMFIDEALFILE.ATTACHMENT_ID, OMFIDEALFILE.DEAL_ID);

        if (Objects.nonNull(contactUserIdPair)) {
            entityUserIdMap.put(Source.CONTACT, contactUserIdPair);
        }

        if (Objects.nonNull(companyUserIdPair)) {
            entityUserIdMap.put(Source.COMPANY, companyUserIdPair);
        }

        if (Objects.nonNull(dealUserIdPair)) {
            entityUserIdMap.put(Source.DEAL, dealUserIdPair);
        }

        return entityUserIdMap;
    }

    /**
     * <p>
     * Gets the entity id based on the attachment id
     * </p>
     *
     * @param orgId                  Represents an organization id
     * @param attachmentId           Represents the attachment id
     * @param table                  Represents the source table
     * @param attachmentIdColumnName Represents the attachment id column name
     * @param entityIdColumnName     Represents the entity id column name
     * @return The entity id
     */
    public Pair<Long, Long> getEntityUserIdPair(final long orgId, final long attachmentId, final Table table, final String attachmentIdColumnName,
                                                final String entityIdColumnName) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(attachmentIdColumnName), attachmentId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row entityRow = dataContainer.getRow(table);

        return Optional.ofNullable(entityRow).map(row -> Pair.of((Long) row.get(entityIdColumnName), (Long) row.get(OMFICONTACTFILE.USER_ID))).orElseGet(() -> null);
    }

    /**
     * <p>
     * Deletes the entity by orgId.
     * </p>
     *
     * @param orgId       Organization id.
     * @param attachments Collection of attachment.
     */
    @Override
    public final void delete(final Long orgId, final Collection<Attachment> attachments) {
        final Table timelineTable = commonTimelineManager.getTable();
        final Table attachmentTimelineTable = Table.get(MTLATTACHMENTTIMELINE.TABLE);
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final Collection<Long> attachmentIds = attachments.stream().map(Attachment::id).collect(Collectors.toSet());
        final SelectQuery selectQuery = SelectQuery.get(timelineTable);

        selectQuery.addJoinClause(new JoinClause(timelinePKColumn, attachmentTimelineTable.getColumn(MTLATTACHMENTTIMELINE.TIMELINE_ID)));
        selectQuery.addSelectClause(timelinePKColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(attachmentTimelineTable.getColumn(MTLATTACHMENTTIMELINE.ATTACHMENT_ID), attachmentIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final List<DeleteQuery> deleteQueries = new ArrayList<>();

            final Table contactFileTable = getContactFileTable();
            final Table companyFileTable = getCompanyFileTable();
            final Table dealFileTable = getDealFileTable();
            final Collection<Long> timelineIds = dataContainer.getRows(timelineTable).map(Row::getPKValue).collect(Collectors.toSet());

            final DeleteQuery contactFileDeleteQuery = DeleteQuery.get(contactFileTable);
            final DeleteQuery companyFileDeleteQuery = DeleteQuery.get(companyFileTable);
            final DeleteQuery dealFileDeleteQuery = DeleteQuery.get(dealFileTable);
            final DeleteQuery timelineDeleteQuery = DeleteQuery.get(timelineTable);

            contactFileDeleteQuery.setWhereClause(new WhereClause(Criteria.in(contactFileTable.getColumn(OMFICONTACTFILE.ATTACHMENT_ID), attachmentIds)));
            companyFileDeleteQuery.setWhereClause(new WhereClause(Criteria.in(companyFileTable.getColumn(OMFICOMPANYFILE.ATTACHMENT_ID), attachmentIds)));
            dealFileDeleteQuery.setWhereClause(new WhereClause(Criteria.in(dealFileTable.getColumn(OMFIDEALFILE.ATTACHMENT_ID), attachmentIds)));
            timelineDeleteQuery.setWhereClause(new WhereClause(Criteria.in(timelinePKColumn, timelineIds)));

            deleteQueries.add(contactFileDeleteQuery);
            deleteQueries.add(companyFileDeleteQuery);
            deleteQueries.add(dealFileDeleteQuery);
            deleteQueries.add(timelineDeleteQuery);

            orgDataStore(orgId).delete(deleteQueries);
        }
    }
}