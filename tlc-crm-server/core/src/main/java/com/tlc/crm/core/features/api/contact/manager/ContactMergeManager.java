package com.tlc.crm.core.features.api.contact.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.features.api.merge.EntityMergeManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.sql.resource.OMKLMARKETINGLISTCONTACT;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.handler.ModelDescriptorLoader;
import com.tlc.crm.entity.internal.ModelDescriptorLoaderImpl;
import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.entity.sql.resource.OMTLTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nKey;

import com.tlc.l10n.temporal.model.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manages the duplicate and merge contact related DB actions
 * </p>
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class ContactMergeManager extends EntityMergeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactMergeManager.class);

    private static final Set<String> EXCLUDED_FIELDS = Set.of(ContactFields.ENTITY_TYPE, ContactFields.ID,
            ContactFields.SUBSCRIPTION_STATUS, ContactFields.SUBSCRIPTION_TYPES,
            ContactFields.UNSUBSCRIBE_REASON, ContactFields.OTHER_UNSUBSCRIBE_REASON,
            ContactFields.LIFECYCLE_STAGE, ContactFields.LIFECYCLE_STATUS, ContactFields.LOST_REASON);

    private final ContactManager contactManager;
    private final ParticipantManager participantManager;
    private final DealManager dealManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final CompanyManager companyManager;
    private final FieldService fieldService;
    private final CommonTimelineManager commonTimelineManager;
    private final ModificationHandler modificationHandler;
    private final Integer contactBatchFetchSize;
    private final DefaultFieldValuesLoader defaultFieldValuesLoader;
    private final MarketingListContactManager marketingListContactManager;

    private static class Instance {
        private static final ContactMergeManager INSTANCE = new ContactMergeManager();
    }

    /**
     * <p>
     * Initializes all required manager and service instances used across contact merge operations.
     * </p>
     */
    private ContactMergeManager() {
        fieldService = FieldService.getSecureInstance();
        contactManager = ContactManager.getInstance();
        participantManager = ParticipantManager.getInstance();
        dealManager = DealManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        companyManager = CompanyManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        modificationHandler = ModificationHandler.getInstance();
        contactBatchFetchSize = 10;
        defaultFieldValuesLoader = DefaultFieldValuesLoader.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactMergeManager} Instance
     * </p>
     *
     * @return {@link ContactMergeManager}
     */
    public static ContactMergeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the contact preview details based on the primary and secondary ids.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param primaryId   Represents the primary contact id
     * @param secondaryId Represents the secondary contact id
     * @return The {@link Contact} preview details
     */
    public Contact getContactPreviewDetails(final Long orgId, final Long primaryId, final Long secondaryId) {
        final Collection<Long> contactIds = Set.of(primaryId, secondaryId);
        final Contact primaryContact = getMergePreviewContact(orgId, primaryId);
        final Contact secondaryContact = getMergePreviewContact(orgId, secondaryId);

        if (Objects.isNull(primaryContact) || Objects.isNull(secondaryContact)) {
            return Objects.isNull(primaryContact) ? secondaryContact : primaryContact;
        }

        contactManager.loadSummaryDetails(orgId, primaryContact);
        contactManager.loadSummaryDetails(orgId, secondaryContact);

        loadMarketingListsForPreview(orgId, primaryContact, secondaryContact);

        mergeContactsForPreview(primaryContact, secondaryContact);
        primaryContact.setDealCount(getDealCount(orgId, contactIds));
        primaryContact.setActivityCount(sourceActivityManager.getContactActivityCount(orgId, contactIds));

        return primaryContact;
    }

    /**
     * Loads contact preview model for merge flow with full hydration from manager conversion.
     */
    private Contact getMergePreviewContact(final Long orgId, final Long contactId) {
        final Table contactTable = contactManager.getTable();
        final DataContainer dataContainer = contactManager.getContactDataContainer(orgId, List.of(contactId));
        final Row contactRow = dataContainer.getRow(contactTable, contactId);

        return Objects.isNull(contactRow) ? null : contactManager.convertRowToModel(orgId, contactRow, false, dataContainer);
    }

    /**
     * <p>
     * Gets the {@link Contact} details.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return The {@link Contact} details
     */
    public Contact get(final Long orgId, final Long contactId) {
        final Table contactTable = contactManager.getTable();
        final DataContainer dataContainer = contactManager.getContactDataContainer(orgId, List.of(contactId));
        final Contact contact = getContactDetails(orgId, contactId, contactTable, dataContainer);

        dataContainer.append(getContactMarketingListDataContainer(orgId, List.of(contactId)));

        if (Objects.nonNull(contact)) {
            contact.setMarketingLists(marketingListContactManager.buildMarketingLists(orgId, contactId, dataContainer));
        }

        return contact;
    }

    /**
     * <p>
     * Gets the contact details for the given contact id from the provided
     * {@link DataContainer}.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param contactId     Represents the contact id
     * @param contactTable  Represents the contact table
     * @param dataContainer Holds the contact data
     *
     * @return The contact details if found; otherwise {@code null}
     */
    private Contact getContactDetails(final Long orgId, final Long contactId, final Table contactTable, final DataContainer dataContainer) {
        final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)));

        return Objects.isNull(contactRow) ? null : contactManager.convertRowToModel(orgId, contactRow, false, dataContainer);
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Merging duplicate contact values with primary contact
     * </p>
     *
     * @param primaryContactId    Represents the primary contact id
     * @param orgId               Represents the organization id
     * @param userId              Represents the user id
     * @param duplicateContactIds {@link Collection<Long>} of duplicate contact ids
     * @param contactIds          Holds the both primary and secondary contact ids
     * @return response of Merged contacts
     */
    public Map<String, Object> mergeContacts(final Long orgId, final long userId, final Long primaryContactId,
                                             final Collection<Long> duplicateContactIds, final Collection<Long> contactIds) {
        final Map<String, Object> response = new HashMap<>();
        final Table contactTable = contactManager.getTable();
        final Table contactTimeLineTable = commonTimelineManager.getContactTimelineTable();
        final Table contactDropdownTable = contactManager.getContactDropdownTable();
        final Column contactIdColumn = contactTable.getPKColumn();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Collection<Long>> contactBatchedCollection = JavaUtil.splitCollection(contactIds, contactBatchFetchSize);
        final DataContainer dataContainer = DataContainer.create();

        for (final Collection<Long> contactIdBatch : contactBatchedCollection) {
            dataContainer.append(contactManager.getContactDataContainer(orgId, contactIdBatch));
            dataContainer.append(getContactTimelineDataContainer(orgId, contactIdBatch, contactTable, contactTimeLineTable));
        }
        final Row primaryContactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactIdColumn, primaryContactId)));
        final Collection<Row> duplicateContactRows = dataContainer.getRows(contactTable, new WhereClause(
                Criteria.in(contactIdColumn, duplicateContactIds))).sorted(Comparator.comparing((row) ->
                ((DateTime) row.get(MCOCONTACT.CREATED_TIME)).getTimeStamp())).toList();
        final String primaryContactName = Contact.getFullName(primaryContactRow.get(MCOCONTACT.FIRST_NAME),
                primaryContactRow.get(MCOCONTACT.LAST_NAME));
        final Set<String> columnNames = contactTable.getTableDefinition().getColumns();
        final Set<String> ignorableColumnNames = Set.of("OTHER_UNSUBSCRIBE_REASON");

        for (final String columnName : columnNames) {
            if (ignorableColumnNames.contains(columnName)) {
                continue;
            }
            if (Objects.isNull(primaryContactRow.get(columnName))) {
                final Optional<Row> duplicateContactRow = duplicateContactRows.stream().filter(row ->
                        Objects.nonNull(row.get(columnName))).findFirst();

                duplicateContactRow.ifPresent(row -> primaryContactRow.set(columnName, row.get(columnName)));
            }
        }
        final Contact contact = convertRowToModel(primaryContactRow);

        try {
            final DataContainer targetContainer = DataContainer.create();
            final Long contactId = contact.id();
            final String entityType = Contact.ENTITY_TYPE;
            final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(entityType).values();
            final Collection<FieldInfo> orgFields = fieldService.getOrgFields(orgId);

            for (final FieldInfo crmField : fields) {
                final StoreType storeType = crmField.storeType();
                final boolean multiSelectType = StoreType.MULTI_SELECT == storeType;

                if (crmField.isActive() && (StoreType.isModelStore(storeType) || multiSelectType) && !crmField.isPersistent()) {
                    final Long fieldId = crmField.id();
                    final boolean isEmail = StoreType.EMAILS.equals(storeType);

                    if (multiSelectType) {
                        loadMultiSelectFieldsIntoContainer(contactId, fieldId, Objects.nonNull(crmField.presetConfig().dependsOn()), contactIds, contactDropdownTable, dataContainer, targetContainer);
                    } else if (StoreType.DROPDOWN == storeType) {
                        loadDropdownFieldRowsIntoContainer(orgId, crmField, Objects.isNull(crmField.presetConfig().dependsOn()), primaryContactId, duplicateContactIds, dataContainer, targetContainer);
                    } else if (!isEmail && !StoreType.PHONES.equals(storeType)) {
                        loadDefaultFieldRowsIntoContainer(orgId, crmField, primaryContactId, duplicateContactIds, dataContainer, targetContainer);
                    } else if (isEmail) {
                        loadContactEmailRows(contact, contactIds, fieldId, dataContainer);
                        loadEmailRowsIntoContainer(orgId, contact, fieldId, dataContainer, targetContainer, contactIds);
                    } else {
                        loadContactPhoneRows(contact, contactIds, fieldId, dataContainer);
                        loadPhoneRowsIntoContainer(contact, fieldId, dataContainer, targetContainer);
                    }
                }
            }

            for (final FieldInfo crmField : orgFields) {

                if (StoreType.MULTI_SELECT == crmField.storeType() && !crmField.isPersistent()) {
                    loadMultiSelectFieldsIntoContainer(contactId, crmField.id(), Objects.nonNull(crmField.presetConfig().dependsOn()), contactIds, contactDropdownTable, dataContainer, targetContainer);
                }
            }
            loadCustomFieldRowsIntoContainer(contact, duplicateContactIds, dataContainer, targetContainer, Contact.ENTITY_TYPE);
            loadStandardMergeRowsIntoContainer(orgId, Source.CONTACT, contactId, duplicateContactIds, targetContainer, List.of(),
                    List.of(new MapperTableDedupedTransferSpec(marketingListContactManager.getMarketingListContactTable(),
                            OMKLMARKETINGLISTCONTACT.CONTACT_ID, OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID)));
            loadDealContactRowsIntoContainer(orgId, contactId, duplicateContactIds, contactIds, targetContainer);
            loadMappingCompanyRowsIntoContainer(orgId, duplicateContactIds, contactId, dataContainer, targetContainer);
            loadContactTimelineIntoContainer(orgId, userId, contactId, duplicateContactIds, primaryContactName, dataContainer, targetContainer);

            primaryContactRow.set(MCOCONTACT.MODIFIED_TIME, Instant.now().toEpochMilli());
            primaryContactRow.set(MCOCONTACT.UPDATED_BY, userId);

            targetContainer.updateRow(primaryContactRow);
            duplicateContactRows.forEach(targetContainer::deleteRow);

            orgDataStore.commitChanges(targetContainer);
        } catch (ErrorCode e) {
            e.printStackTrace();
            LOGGER.error("Contact merge failed for orgId: {}, primaryContactId: {}, duplicateContactIds: {}, reason: {}", orgId, primaryContactId, duplicateContactIds, e.getMessage());
            throw ErrorCode.get(ContactErrorCodes.MERGE_ACTION_FAILED);
        }
        response.put(ContactFields.MERGED_RECORDS, duplicateContactIds);
        response.put(ContactFields.CONTACT, contact);

        return response;
    }

    /**
     * <p>
     * Updates the mapping company rows based on provided contact id.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param duplicateContactIds Represents the {@link Collection} of duplicate contact ids
     * @param contactId           Represents the primary contact id
     * @param dataContainer       Holds the existing data for the respective duplicate and primary company
     * @param targetContainer     Holds the updated contact information into {@link DataContainer}
     */
    private void loadMappingCompanyRowsIntoContainer(final Long orgId, final Collection<Long> duplicateContactIds, final Long contactId,
                                                     final DataContainer dataContainer, final DataContainer targetContainer) {
        final String companyKey = ContactFields.COMPANY;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE,
                List.of(companyKey)).get(companyKey);
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Column entityIdColumn = contactCompanyTable.getColumn(OMETENTITY.ENTITY_ID);
        final Criteria fieldIdCriteria = Criteria.eq(contactCompanyTable.getColumn(OMETENTITY.FIELD_ID), fieldInfo.id());
        final Row primaryRow = dataContainer.getRow(contactCompanyTable, new WhereClause(fieldIdCriteria)
                .and(Criteria.eq(entityIdColumn, contactId)));

        if (Objects.isNull(primaryRow)) {
            final Collection<Row> rows = dataContainer.getRows(contactCompanyTable,
                    new WhereClause(Criteria.in(entityIdColumn, duplicateContactIds)).and(fieldIdCriteria)).toList();
            boolean isFirstRow = true; //To update the entity ID of only the first row from a collection of rows that contain company IDs and delete the remaining rows

            for (final Row row : rows) {

                if (isFirstRow) {
                    row.set(OMETENTITY.ENTITY_ID, contactId);

                    targetContainer.updateRow(row);
                    isFirstRow = false;
                } else {
                    targetContainer.deleteRow(row);
                }
            }
        }
    }

    /**
     * <p>
     * Updates the email rows into container
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param contact         Holds the contact attributes
     * @param fieldId         Represents the field id
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param targetContainer Holds the updated information of user defined and system field values
     * @param contactIds      {@link Collection<Long>} of contact ids which contains both primary and duplicate contact ids
     */
    private void loadEmailRowsIntoContainer(final Long orgId, final Contact contact, final Long fieldId, final DataContainer dataContainer,
                                            final DataContainer targetContainer, Collection<Long> contactIds) {
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Column emailFieldIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID);
        final Column emailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Long contactId = contact.id();
        final Collection<Email> emails = contact.getEmails();

        if (Objects.nonNull(emails) && !emails.isEmpty()) {
            final Collection<Long> emailIds = emails.stream().map(Email::id).toList();
            final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                    new WhereClause(Criteria.in(emailIdColumn, emailIds))
                            .and(new WhereClause(Criteria.eq(emailFieldIdColumn, fieldId))));

            contactEmailRows.map(contactEmailRow -> dataContainer.getRow(contactEmailTable, contactEmailRow.getPKValue()))
                    .forEach(row -> {
                        row.set(emailEntityIdColumn, contactId);
                        targetContainer.updateRow(row);
                    });
            deleteUnmappedEmailRows(orgId, contactIds, emailIds, fieldId, dataContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Updates the phone rows into container
     * </p>
     *
     * @param contact         Holds the contact attributes
     * @param fieldId         Represents the field id
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadPhoneRowsIntoContainer(final Contact contact, final Long fieldId, final DataContainer dataContainer,
                                            final DataContainer targetContainer) {
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Column phoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final Column phoneFieldIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID);
        final Column phoneEntityIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Long contactId = contact.id();
        final Collection<Phone> phones = contact.getPhones();

        if (Objects.nonNull(phones) && !phones.isEmpty()) {
            final Collection<Long> phoneIds = phones.stream().map(Phone::id).toList();
            final List<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                            new WhereClause(Criteria.in(phoneIdColumn, phoneIds))
                                    .and(new WhereClause(Criteria.eq(phoneFieldIdColumn, fieldId))))
                    .map(contactPhoneRow -> dataContainer.getRow(contactPhoneTable, contactPhoneRow.getPKValue())).toList();
            final Map<Long, List<Row>> rowsByPhoneId = contactPhoneRows.stream().collect(Collectors.groupingBy(row -> row.get(phoneIdColumn)));

            for (final Map.Entry<Long, List<Row>> entry : rowsByPhoneId.entrySet()) {
                final List<Row> phoneRows = entry.getValue();

                if (1 == phoneRows.size()) {
                    final Row row = phoneRows.get(0);

                    row.set(phoneEntityIdColumn, contactId);
                    targetContainer.updateRow(row);
                } else {
                    final Optional<Row> primaryPhoneRow = phoneRows.stream()
                            .filter(row -> Objects.equals(row.get(phoneEntityIdColumn), contactId))
                            .findFirst();

                    if (primaryPhoneRow.isPresent()) {
                        phoneRows.stream()
                                .filter(row -> !Objects.equals(row.get(phoneEntityIdColumn), contactId))
                                .forEach(targetContainer::deleteRow);
                    } else {
                        final Row phoneRow = phoneRows.remove(0);

                        phoneRow.set(phoneEntityIdColumn, contactId);
                        targetContainer.updateRow(phoneRow);

                        phoneRows.forEach(targetContainer::deleteRow);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Delete the emails that are not associated with the contact
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param contactIds      {@link Collection<Long>} of contact ids which contains both primary and duplicate contact ids
     * @param emailIds        {@link Collection<Long>} of email ids
     * @param fieldId         Represents the field id
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedEmailRows(final Long orgId, final Collection<Long> contactIds, final Collection<Long> emailIds,
                                         final Long fieldId, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Column emailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column emailFieldIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID);
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.in(emailEntityIdColumn, contactIds))
                        .and(new WhereClause(Criteria.eq(emailFieldIdColumn, fieldId))));
        final Collection<Long> ids = contactEmailRows.filter(Objects::nonNull).map(contactEmailRow ->
                (Long) contactEmailRow.get(emailIdColumn)).toList();
        final Set<Long> unMappedEmailIds = new HashSet<>();

        for (final Long id : ids) {

            if (!emailIds.contains(id)) {
                unMappedEmailIds.add(id);
            }
        }
        final Stream<Row> unMappedEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.in(emailIdColumn, unMappedEmailIds)));

        loadMapperTableDeletableRows(orgId, StoreType.EMAIL, getStoreTypeValueColumnName(StoreType.EMAIL), unMappedEmailRows, targetContainer);
    }

    /**
     * <p>
     * Fetches timeline mapping rows and created timeline rows for the given contacts.
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param contactIds           Represents contact ids
     * @param contactTable         Represents contact table
     * @param contactTimelineTable Represents contact timeline table
     * @return The timeline related {@link DataContainer}
     */
    private DataContainer getContactTimelineDataContainer(final Long orgId, final Collection<Long> contactIds,
                                                          final Table contactTable, final Table contactTimelineTable) {
        final Table timelineTable = commonTimelineManager.getTable();
        final SelectQuery timelineSelectQuery = SelectQuery.get(contactTable);

        timelineSelectQuery.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), contactTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID)),
                new JoinClause(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID), timelineTable.getPKColumn())));
        timelineSelectQuery.addSelectClause(contactTimelineTable, timelineTable);
        timelineSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.ACTION), TimelineActionFields.CREATED.toLowerCase()))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.TYPE), TimelineType.CONTACT_CREATED.getId()))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Timeline.Category.CONTACT.name())));

        return orgDataStore(orgId).get(timelineSelectQuery);
    }

    /**
     * <p>
     * Before merging need to update all duplicate contact deals with primary contact.
     * </p>
     *
     * @param orgId               Represents the organization id.
     * @param contactId           Represents the contact id.
     * @param targetContainer     Holds the updated information of user defined and system field values.
     * @param duplicateContactIds {@link Collection<Long>} of duplicate contact ids.
     */
    private void loadDealContactRowsIntoContainer(final Long orgId, final Long contactId, final Collection<Long> duplicateContactIds,
                                                  final Collection<Long> contactIds, final DataContainer targetContainer) {
        final Table dealTable = dealManager.getTable();
        final Table participantTable = participantManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable);
        selectQuery.addSelectClause(List.of(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID),
                participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID)));
        selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID),
                JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.CONTACT_ID), duplicateContactIds))
                .and(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        loadPrimaryContactRowIntoDealTable(contactId, duplicateContactIds, dealTable, dataContainer, targetContainer);
        loadParticipantTableRowsIntoContainer(contactId, duplicateContactIds, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Before merging updates all duplicate ids rows with primary id.
     * </p>
     *
     * @param primaryId       Represents the primary id
     * @param duplicateIds    {@link Collection<Long>} of duplicate ids
     * @param dealTable       Represents deal table.
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    protected void loadPrimaryContactRowIntoDealTable(final Long primaryId, final Collection<Long> duplicateIds, final Table dealTable,
                                                      final DataContainer dataContainer, final DataContainer targetContainer) {
        final Stream<Row> rows = dataContainer.getRows(dealTable,
                new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.CONTACT_ID), duplicateIds)));

        rows.filter(Objects::nonNull).forEach(row -> {
            row.set(MDODEAL.CONTACT_ID, primaryId);
            targetContainer.updateRow(row);
        });
    }

    /**
     * <p>
     * Updates the deal participants rows
     * </p>
     *
     * @param primaryId       Represents the primary id
     * @param duplicateIds    {@link Collection<Long>} of duplicate ids
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadParticipantTableRowsIntoContainer(final Long primaryId, final Collection<Long> duplicateIds, final DataContainer dataContainer,
                                                       final DataContainer targetContainer) {
        final Table participantTable = participantManager.getTable();
        final Column contactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column dealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Map<Long, List<Row>> rowsByDealId = dataContainer.getRows(participantTable, new WhereClause(Criteria.in(contactIdColumn, duplicateIds)))
                .filter(Objects::nonNull).collect(Collectors.groupingBy(row -> (Long) row.get(dealIdColumn)));

        if (rowsByDealId.isEmpty()) {
            return;
        }
        final Set<Long> dealsWithPrimaryContact = dataContainer.getRows(participantTable, new WhereClause(Criteria.eq(contactIdColumn, primaryId)).and(Criteria.in(dealIdColumn, rowsByDealId.keySet())))
                .filter(Objects::nonNull).map(row -> (Long) row.get(dealIdColumn)).collect(Collectors.toUnmodifiableSet());

        rowsByDealId.forEach((dealId, participantRows) -> {
            if (dealsWithPrimaryContact.contains(dealId)) {
                participantRows.forEach(targetContainer::deleteRow);
            } else {
                final Row participantRowToUpdate = participantRows.get(NumericalConstant.ZERO);

                participantRowToUpdate.set(MDOPARTICIPANT.CONTACT_ID, primaryId);
                targetContainer.updateRow(participantRowToUpdate);
                participantRows.stream().skip(1).forEach(targetContainer::deleteRow);
            }
        });
    }

    /**
     * Converts the row into {@link Contact}
     *
     * @param row Holds the attribute values of {@link Contact}
     * @return {@link Contact}
     */
    private Contact convertRowToModel(final Row row) {
        final Contact contact = new Contact();
        final Long orgId = row.getOrgId();
        final Long lifeCycleStageId = row.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);
        final Long lifeCycleStatusId = row.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID);
        final Long createdById = row.get(MCOCONTACT.CREATED_BY);
        final Long updatedById = row.get(MCOCONTACT.UPDATED_BY);
        final Long salesOwnerId = row.get(MCOCONTACT.OWNER_ID);
        final Collection<Long> userIds = collectUserIds(createdById, updatedById, salesOwnerId);

        contact.setId(row.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(row.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(row.get(MCOCONTACT.LAST_NAME));

        if (Objects.nonNull(lifeCycleStageId)) {
            final LifeCycleStage lifeCycleStage = new LifeCycleStage();

            lifeCycleStage.setId(lifeCycleStageId);
            contact.setLifeCycleStage(lifeCycleStage);
        }

        if (Objects.nonNull(lifeCycleStatusId)) {
            final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

            lifeCycleStatus.setId(lifeCycleStatusId);
            contact.setLifeCycleStatus(lifeCycleStatus);
        }
        contact.setModifiedTime(row.get(MCOCONTACT.MODIFIED_TIME));
        contact.setCreatedTime(row.get(MCOCONTACT.CREATED_TIME));
        contact.setLastAssignedAt(row.get(MCOCONTACT.LAST_ASSIGNED_AT));
        contact.setOwnerId(salesOwnerId);

        contact.setUserIds(userIds);
        setContactUsers(contact, createdById, updatedById, salesOwnerId, getUsersById(orgId, userIds));
        contact.setTerritory(buildPartialTerritory(row.get(MCOCONTACT.TERRITORY_ID)));

        return contact;
    }

    /**
     * <p>
     * Gets the primary contact values for given contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Contact} which contains primary contact value
     */
    public Contact getPrimaryContact(final Long orgId, final Long contactId) {
        final Table contactTable = contactManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.addSelectClause(getSelectedColumns());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(contactTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        } else {
            return convertRowToModel(row, dataContainer);
        }
    }

    /**
     * <p>
     * Gets the selected {@link Collection} of columns
     * </p>
     *
     * @return {@link Collection} of columns
     */
    private Collection<Column> getSelectedColumns() {
        final Table contactEmailTable = contactManager.getContactEmailDerivedTable();
        final Table contactPhoneTable = contactManager.getContactPhoneDerivedTable();
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();

        return List.of(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID),
                contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.CREATED_BY), contactTable.getColumn(MCOCONTACT.UPDATED_BY),
                emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE),
                phoneTable.getColumn(OMETPHONE.NUMBER), phoneTable.getColumn(OMETPHONE.COUNTRY_ID),
                phoneTable.getColumn(OMETPHONE.TYPE), contactTable.getColumn(MCOCONTACT.CREATED_TIME));
    }

    /**
     * <p>
     * Gets the collection of possible duplicate records by primary contact email and mobile values
     * </p>
     *
     * @param primaryContact An entity that holds the contact attributes
     * @param orgId          Represents an organization id
     * @param start          Represents the start value
     * @param limit          Represents the limit value
     * @return {@link Collection} of duplicate contacts by primary contact values
     */
    public Collection<Contact> getDuplicateContacts(final Long orgId, final Contact primaryContact, final int start,
                                                    final int limit) {
        final Collection<Long> duplicateContacts = getDuplicateContactIds(orgId, primaryContact, start, limit);

        if (duplicateContacts.isEmpty()) {
            return Collections.emptyList();
        }
        final Table contactTable = contactManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.addSelectClause(getSelectedColumns());
        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.CREATED_TIME), contactTable.getColumn(MCOCONTACT.OWNER_ID)));
        selectQuery.setWhereClause(getDuplicateContactCondition(primaryContact).and(Criteria.in(contactTable.getPKColumn(), duplicateContacts)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(contactTable).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Fetches the duplicate contact ids for the given primary {@link Contact} details
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param primaryContact Represents the primary {@link Contact}
     * @param start          Represents the start value
     * @param limit          Represents the limit value
     * @return the collection of contact ids
     */
    private Collection<Long> getDuplicateContactIds(final Long orgId, final Contact primaryContact, final int start,
                                                    final int limit) {
        final Table contactTable = contactManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final SelectQuery innerSelectQuery = SelectQuery.get(contactTable);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Table contactEmailTable = contactManager.getContactEmailDerivedTable();
        final Table contactPhoneTable = contactManager.getContactPhoneDerivedTable();
        final Column contactIdColumn = contactManager.getTable().getPKColumn();
        final Column createdTimeColumn = contactTable.getColumn(MCOCONTACT.CREATED_TIME);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(List.of(contactPkColumn, createdTimeColumn));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn));
        innerSelectQuery.addSelectClause(contactPkColumn);
        innerSelectQuery.addJoinClause(List.of(
                new JoinClause(contactIdColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactIdColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailManager.getTable().getPKColumn(), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneManager.getTable().getPKColumn(), leftJoin)));
        innerSelectQuery.setWhereClause(getDuplicateContactCondition(primaryContact));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, innerSelectQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(contactTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Retrieves the total duplicate contact count by using primary contact email and mobile values
     * </p>
     *
     * @param primaryContact An entity that holds the contact attributes
     * @param orgId          Represents the organization id
     * @return the {@link Long} of duplicate contact count
     */
    public long getDuplicateContactCount(final Long orgId, final Contact primaryContact) {
        final Table contactTable = contactManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column contactPkColumn = contactTable.getPKColumn();

        selectQuery.addJoinClause(getJoinClauses());
        selectQuery.addSelectClause(contactPkColumn);
        selectQuery.addGroupByClause(new GroupByClause(contactPkColumn));
        selectQuery.setWhereClause(getDuplicateContactCondition(primaryContact));

        return orgDataStore(orgId).get(selectQuery).size(contactTable);
    }

    /**
     * Gets the join clauses of contact
     *
     * @return {@link List} of JoinClauses of contact
     */
    private List<JoinClause> getJoinClauses() {
        final Table contactEmailTable = contactManager.getContactEmailDerivedTable();
        final Table contactPhoneTable = contactManager.getContactPhoneDerivedTable();
        final Column contactIdColumn = contactManager.getTable().getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(new JoinClause(contactIdColumn, contactManager.getContactCompanyTable().getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactIdColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactIdColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailManager.getTable().getPKColumn(), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneManager.getTable().getPKColumn(), leftJoin));
    }

    /**
     * <p>
     * Gets the {@link WhereClause} to filter the contacts by emails and phones
     * </p>
     *
     * @param contact {@link Contact} which holds the contact attributes
     * @return {@link WhereClause} which contains the {@link Criteria} to filter contacts
     */
    private WhereClause getDuplicateContactCondition(final Contact contact) {
        final Collection<Phone> phones = contact.getPhones();
        final Collection<Email> emails = contact.getEmails();
        final Collection<String> phoneNumbers = Objects.nonNull(phones) ? phones.stream().map(Phone::getNumber).toList() : null;
        final Collection<String> emailValues = Objects.nonNull(emails) ? emails.stream().map(Email::getValue).toList() : null;
        final boolean isPhoneNumbersEmpty = Objects.isNull(phoneNumbers) || phoneNumbers.isEmpty();
        final boolean isEmailValuesEmpty = Objects.isNull(emailValues) || emailValues.isEmpty();
        final WhereClause phoneCondition = new WhereClause(Criteria.in(phoneManager.getTable().getColumn(OMETPHONE.NUMBER), phoneNumbers));
        final WhereClause emailCondition = new WhereClause(Criteria.in(emailManager.getTable().getColumn(OMETEMAIL.VALUE), emailValues));

        return isPhoneNumbersEmpty ? emailCondition : isEmailValuesEmpty ? phoneCondition : phoneCondition.or(emailCondition);
    }

    /**
     * <p>
     * Gets the preview contact values by merging both primary and duplicate contacts values
     * </p>
     *
     * @param primaryContactId    Represents the primary contact id
     * @param orgId               Represents the organization id
     * @param duplicateContactIds {@link Collection<Long>} of duplicate contact ids
     * @return {@link Contact} which contains preview contact value
     */
    public Contact getPreviewContact(final Long orgId, final Long primaryContactId,
                                     final Collection<Long> duplicateContactIds, final Collection<Long> contactIds) {
        final String email = ContactFields.EMAILS;
        final String phone = ContactFields.PHONES;
        final Map<String, FieldInfo> fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, email, phone);
        final Contact contact = new Contact();
        final Table contactTable = contactManager.getTable();
        final Column contactIdColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        contactManager.loadSelectAndJoinClauses(selectQuery);
        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.CREATED_TIME), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactTable.getColumn(MCOCONTACT.CREATED_BY), contactTable.getColumn(MCOCONTACT.UPDATED_BY),
                contactTable.getColumn(MCOCONTACT.MODIFIED_TIME), contactTable.getColumn(MCOCONTACT.LAST_ASSIGNED_AT),
                contactTable.getColumn(MCOCONTACT.RECENT_NOTE)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactIdColumn, contactIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row primaryContactRow = dataContainer.getRow(contactTable,
                new WhereClause(Criteria.eq(contactIdColumn, primaryContactId)));
        final Long createdById = primaryContactRow.get(MCOCONTACT.CREATED_BY);
        final Long updatedById = primaryContactRow.get(MCOCONTACT.UPDATED_BY);
        final Long salesOwnerId = primaryContactRow.get(MCOCONTACT.OWNER_ID);
        final String primaryContactLastName = primaryContactRow.get(MCOCONTACT.LAST_NAME);

        if (Objects.isNull(primaryContactLastName)) {
            final Stream<Row> duplicateContactRows = dataContainer.getRows(contactTable, new WhereClause(
                    Criteria.in(contactIdColumn, duplicateContactIds))).sorted(Comparator.comparing((row) ->
                    ((DateTime) row.get(MCOCONTACT.CREATED_TIME)).getTimeStamp()));
            final Optional<Row> optional = duplicateContactRows.filter(row ->
                    Objects.nonNull(row.get(MCOCONTACT.LAST_NAME))).findFirst();

            optional.ifPresent(row -> contact.setLastName(row.get(MCOCONTACT.LAST_NAME)));
        } else {
            contact.setLastName(primaryContactLastName);
        }
        contact.setId(primaryContactRow.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(primaryContactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setCreatedTime(primaryContactRow.get(MCOCONTACT.CREATED_TIME));
        contact.setModifiedTime(primaryContactRow.get(MCOCONTACT.MODIFIED_TIME));
        contact.setLastAssignedAt(primaryContactRow.get(MCOCONTACT.LAST_ASSIGNED_AT));
        contact.setRecentNote(primaryContactRow.get(MCOCONTACT.RECENT_NOTE));
        contact.setOwnerId(salesOwnerId);
        contact.setDealCount(getDealCount(orgId, contactIds));
        loadContactEmailRows(contact, contactIds, fieldInfo.get(email).id(), dataContainer);
        loadContactPhoneRows(contact, contactIds, fieldInfo.get(phone).id(), dataContainer);
        loadContactCompanyRow(contact, duplicateContactIds, dataContainer);

        final Set<Long> userIds = collectUserIds(createdById, updatedById, salesOwnerId);

        contact.setUserIds(userIds);
        setContactUsers(contact, createdById, updatedById, salesOwnerId, getUsersById(orgId, userIds));

        return contact;
    }

    /**
     * Merges secondary contact values into primary for preview responses.
     */
    private void mergeContactsForPreview(final Contact primaryContact, final Contact secondaryContact) {
        primaryContact.setFirstName(preferPrimaryValue(primaryContact.getFirstName(), secondaryContact.getFirstName()));
        primaryContact.setLastName(preferPrimaryValue(primaryContact.getLastName(), secondaryContact.getLastName()));
        primaryContact.setCompany(preferPrimaryValue(primaryContact.getCompany(), secondaryContact.getCompany()));
        primaryContact.setDesignation(preferPrimaryValue(primaryContact.getDesignation(), secondaryContact.getDesignation()));
        primaryContact.setDepartment(preferPrimaryValue(primaryContact.getDepartment(), secondaryContact.getDepartment()));
        primaryContact.setFacebook(preferPrimaryValue(primaryContact.getFacebook(), secondaryContact.getFacebook()));
        primaryContact.setTwitter(preferPrimaryValue(primaryContact.getTwitter(), secondaryContact.getTwitter()));
        primaryContact.setLinkedIn(preferPrimaryValue(primaryContact.getLinkedIn(), secondaryContact.getLinkedIn()));
        primaryContact.setDateOfBirth(preferPrimaryValue(primaryContact.getDateOfBirth(), secondaryContact.getDateOfBirth()));
        primaryContact.setAddress(preferPrimaryValue(primaryContact.getAddress(), secondaryContact.getAddress()));
        primaryContact.setContactSource(preferPrimaryValue(primaryContact.getContactSource(), secondaryContact.getContactSource()));
        primaryContact.setLifeCycleStage(preferPrimaryValue(primaryContact.getLifeCycleStage(), secondaryContact.getLifeCycleStage()));
        primaryContact.setLifeCycleStatus(preferPrimaryValue(primaryContact.getLifeCycleStatus(), secondaryContact.getLifeCycleStatus()));
        primaryContact.setLostReason(preferPrimaryValue(primaryContact.getLostReason(), secondaryContact.getLostReason()));
        primaryContact.setSubscriptionStatus(preferPrimaryValue(primaryContact.getSubscriptionStatus(), secondaryContact.getSubscriptionStatus()));
        primaryContact.setSubscriptionTypes(mergeUniqueByKey(primaryContact.getSubscriptionTypes(), secondaryContact.getSubscriptionTypes(), Option::id));
        primaryContact.setTimeZone(preferPrimaryValue(primaryContact.getTimeZone(), secondaryContact.getTimeZone()));
        primaryContact.setUnsubscribeReason(preferPrimaryValue(primaryContact.getUnsubscribeReason(), secondaryContact.getUnsubscribeReason()));
        primaryContact.setOtherUnsubscribeReason(preferPrimaryValue(primaryContact.getOtherUnsubscribeReason(), secondaryContact.getOtherUnsubscribeReason()));
        primaryContact.setTerritory(preferPrimaryValue(primaryContact.getTerritory(), secondaryContact.getTerritory()));
        primaryContact.setPrimaryEmail(preferPrimaryValue(primaryContact.getPrimaryEmail(), secondaryContact.getPrimaryEmail()));
        primaryContact.setPrimaryPhone(preferPrimaryValue(primaryContact.getPrimaryPhone(), secondaryContact.getPrimaryPhone()));
        primaryContact.setRecentNote(preferPrimaryValue(primaryContact.getRecentNote(), secondaryContact.getRecentNote()));
        primaryContact.setSummaryInfo(mergeSummaryInfoForPreview(primaryContact.getSummaryInfo(), secondaryContact.getSummaryInfo()));
        primaryContact.setEmails(mergeUniqueByKey(primaryContact.getEmails(), secondaryContact.getEmails(), Email::getValue));
        primaryContact.setPhones(mergeUniqueByKey(primaryContact.getPhones(), secondaryContact.getPhones(), Phone::getNumber));
        primaryContact.setMarketingLists(mergeUniqueByKey(primaryContact.getMarketingLists(), secondaryContact.getMarketingLists(), MarketingList::id));
        mergeCustomFields(primaryContact, secondaryContact);
    }

    /**
     * <p>
     * Loads the contact phone rows
     * </p>
     *
     * @param contact       Holds the contact attributes
     * @param contactIds    {@link Collection<Long>} of contact ids which contains both primary and duplicate contact ids
     * @param fieldId       Represents the field id
     * @param dataContainer A container that holds the user defined field values and system values
     */
    private void loadContactPhoneRows(final Contact contact, final Collection<Long> contactIds, final Long fieldId, final DataContainer dataContainer) {
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table contactTable = contactManager.getTable();
        final Column phoneEntityIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Column phoneFieldIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID);
        final Column phoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final Long primaryPhoneId = dataContainer.getRow(contactTable, contact.id()).get(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE));
        final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                new WhereClause(Criteria.in(phoneEntityIdColumn, contactIds))
                        .and(new WhereClause(Criteria.eq(phoneFieldIdColumn, fieldId))));

        if (Objects.nonNull(contactPhoneRows)) {
            final Table phoneTable = phoneManager.getTable();
            final Stream<Row> phoneRows = dataContainer.getRows(phoneTable,
                    new WhereClause(Criteria.in(phoneTable.getPKColumn(), contactPhoneRows.map(contactPhoneRow ->
                            contactPhoneRow.get(phoneIdColumn)).collect(Collectors.toSet()))));

            if (Objects.nonNull(phoneRows)) {
                final Set<String> phones = new HashSet<>();
                final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);

                dataContainer.indexRows(phoneNumberColumn);

                phoneRows.forEach(phoneRow -> {
                    final String phoneNumber = phoneRow.get(phoneNumberColumn);
                    final Stream<Row> indexedPhoneRows = dataContainer.getIndexedRows(phoneNumberColumn, phoneNumber);

                    indexedPhoneRows.forEach(indexedPhoneRow -> {
                        final boolean containsPhone = phones.contains(phoneNumber);

                        if (containsPhone && Objects.equals(primaryPhoneId, indexedPhoneRow.getPKValue())) {
                            final Collection<Phone> contactPhones = contact.getPhones();

                            if (contactPhones.removeIf(phone -> Objects.equals(phone.getNumber(), phoneNumber) && !Objects.equals(phone.id(), primaryPhoneId))) {
                                addPhoneToContact(contact, indexedPhoneRow, phoneNumber, phones);
                            }
                        } else if (!containsPhone) {
                            addPhoneToContact(contact, indexedPhoneRow, phoneNumber, phones);
                        }

                    });
                });
            }
        }
    }


    /**
     * <p>
     * Adds phone to the provided contact
     * </p>
     *
     * @param contact     Represents the {@link Contact}
     * @param phoneRow    Represents the phone row
     * @param phoneNumber Represents the phone number
     * @param phones      Represents the collection of phones
     */
    private void addPhoneToContact(final Contact contact, final Row phoneRow, final String phoneNumber, final Collection<String> phones) {
        final Phone phone = phoneManager.convertRowToModel(phoneRow);

        contact.addPhone(phone);
        phones.add(phoneNumber);
    }

    /**
     * <p>
     * Loads the contact email rows
     * </p>
     *
     * @param contact       Holds the contact attributes
     * @param contactIds    {@link Collection<Long>} of contact ids which contains both primary and duplicate contact ids
     * @param fieldId       Represents the field id
     * @param dataContainer A container that holds the user defined field values and system values
     */
    private void loadContactEmailRows(final Contact contact, final Collection<Long> contactIds, final Long fieldId, final DataContainer dataContainer) {
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactTable = contactManager.getTable();
        final Column emailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column emailFieldIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID);
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Long primaryEmailId = dataContainer.getRow(contactTable, contact.id()).get(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL));
        final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.in(emailEntityIdColumn, contactIds))
                        .and(new WhereClause(Criteria.eq(emailFieldIdColumn, fieldId))));

        if (Objects.nonNull(contactEmailRows)) {
            final Table emailTable = emailManager.getTable();
            final Stream<Row> emailRows = dataContainer.getRows(emailTable,
                    new WhereClause(Criteria.in(emailTable.getPKColumn(), contactEmailRows.map(contactEmailRow ->
                            contactEmailRow.get(emailIdColumn)).collect(Collectors.toSet()))));

            if (Objects.nonNull(emailRows)) {
                final Set<String> emails = new HashSet<>();
                final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);

                dataContainer.indexRows(emailValueColumn);

                emailRows.forEach(emailRow -> {
                    final String emailValue = emailRow.get(emailValueColumn);
                    final Stream<Row> indexedEmailRows = dataContainer.getIndexedRows(emailValueColumn, emailValue);

                    indexedEmailRows.forEach(indexedEmailRow -> {
                        final boolean containsEmail = emails.contains(emailValue);

                        if (containsEmail && Objects.equals(primaryEmailId, indexedEmailRow.getPKValue())) {
                            final Collection<Email> contactEmails = contact.getEmails();

                            if (contactEmails.removeIf(email -> Objects.equals(email.getValue(), emailValue) && !Objects.equals(email.id(), primaryEmailId))) {
                                addEmailToContact(contact, indexedEmailRow, emailValue, emails);
                            }
                        } else if (!containsEmail) {
                            addEmailToContact(contact, indexedEmailRow, emailValue, emails);
                        }
                    });
                });
            }
        }
    }

    /**
     * <p>
     * Adds email to the provided contact
     * </p>
     *
     * @param contact    Represents the {@link Contact}
     * @param emailRow   Represents the email row
     * @param emailValue Represents the email value
     * @param emails     Represents the collection of emails
     */
    private void addEmailToContact(final Contact contact, final Row emailRow, final String emailValue, final Collection<String> emails) {
        final Email email = emailManager.convertRowToModel(emailRow);

        contact.addEmail(email);
        emails.add(emailValue);
    }

    /**
     * <p>
     * Loads the company row of contact if available or get the row from duplicate contact ids
     * </p>
     *
     * @param contact             Holds the contact attributes
     * @param dataContainer       A container that holds the user defined field values and system values
     * @param duplicateContactIds {@link Collection<Long>} of duplicate contact ids
     */
    private void loadContactCompanyRow(final Contact contact, final Collection<Long> duplicateContactIds,
                                       final DataContainer dataContainer) {
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Column contactIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
        final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria
                .eq(contactIdColumn, contact.id())));

        if (Objects.isNull(contactCompanyRow)) {
            final Stream<Row> contactCompanyRows = dataContainer.getRows(contactCompanyTable, new WhereClause(Criteria
                    .in(contactIdColumn, duplicateContactIds)));
            final Row row = contactCompanyRows.findFirst().orElse(null);

            contact.setCompany(Objects.isNull(row) ? null : convertRowToCompany(row, dataContainer));
        } else {
            contact.setCompany(convertRowToCompany(contactCompanyRow, dataContainer));
        }
    }

    /**
     * Gets the total deal count by contact ids
     *
     * @param orgId      Represents the organization id
     * @param contactIds {@link Collection<Long>} of contact ids
     * @return {@link Long} of deal count of contact ids
     */
    private Long getDealCount(final Long orgId, final Collection<Long> contactIds) {
        final Table dealTable = dealManager.getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.CONTACT_ID), contactIds)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the primary and secondary contact using email value
     *
     * @param orgId Represents the organization id
     * @param start Represents the start value
     * @param limit Represents the limit value
     * @return {@link List} of contacts by duplicate email value
     */
    public List<List<Contact>> getContactsByDuplicateEmail(final long orgId, final int limit, final int start) {
        final Set<String> contactSet = new HashSet<>();
        final List<List<Contact>> contactList = new ArrayList<>();
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final Column emailContactIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Column contactFirstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column contactLastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column createdTimeColumn = contactTable.getColumn(MCOCONTACT.CREATED_TIME);
        final Column contactIdColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final String fieldName = ContactFields.EMAILS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final Long fieldId = fieldInfo.id();
        final Collection<String> dismissedEmails = getDismissedValues(orgId, fieldInfo.sourceId(), fieldId);
        final List<JoinClause> joinClauses = List.of(new JoinClause(contactIdColumn, emailContactIdColumn, leftJoin),
                new JoinClause(emailIdColumn, emailTable.getPKColumn(), leftJoin));
        final List<Column> selectedColumns = List.of(emailContactIdColumn, emailIdColumn, contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactFirstNameColumn, contactLastNameColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID), createdTimeColumn);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId));

        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(selectedColumns);
        selectQuery.addSelectClause(emailTable);
        final SelectQuery innerSelectQuery = SelectQuery.get(emailTable);

        innerSelectQuery.addSelectClause(emailValueColumn);
        innerSelectQuery.addJoinClause(new JoinClause(emailTable.getPKColumn(), emailIdColumn));
        innerSelectQuery.addGroupByClause(new GroupByClause(emailValueColumn));
        innerSelectQuery.addHavingClause(new HavingClause(Criteria.gt(emailValueColumn.count(), 1)));
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.setWhereClause(dismissedEmails.isEmpty() ? whereClause :
                whereClause.and(Criteria.notIn(emailValueColumn, dismissedEmails)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailValueColumn, innerSelectQuery))
                .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId)));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            dataContainer.indexRows(emailValueColumn);
            final Stream<Row> emailRows = dataContainer.getRows(emailTable);

            emailRows.filter(Objects::nonNull).forEach(row -> {
                final String emailValue = row.get(emailValueColumn);

                if (!contactSet.contains(emailValue)) {
                    final Collection<Row> indexedEmailRows = dataContainer.getIndexedRows(emailValueColumn, emailValue)
                            .limit(2).toList();
                    final List<Contact> contacts = new ArrayList<>();

                    for (final Row emailRow : indexedEmailRows) {
                        final Row contactEmailRow = dataContainer.getRow(contactEmailTable,
                                new WhereClause(Criteria.eq(emailIdColumn, emailRow.getPKValue())));
                        final Row contactRow = dataContainer.getRow(contactTable,
                                (Long) contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID));
                        final Contact contact = new Contact();
                        final Email contactEmail = new Email();

                        contact.setId(contactRow.getPKValue());
                        contact.setOrgId(orgId);
                        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
                        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                        contactEmail.setValue(emailValue);
                        contact.addEmail(contactEmail);
                        contacts.add(contact);
                        contactSet.add(emailValue);
                    }
                    contactList.add(contacts);
                }
            });
        }

        return contactList;
    }

    /**
     * Gets the primary and secondary contact using phone number
     *
     * @param orgId Represents the organization id
     * @param limit Represents the limit value
     * @return {@link List} of contacts by duplicate phone number
     */
    public List<List<Contact>> getContactsByDuplicateMobile(final long orgId, final int start, final int limit) {
        final Set<String> contactSet = new HashSet<>();
        final List<List<Contact>> contactList = new ArrayList<>();
        final Table contactTable = contactManager.getTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final Column phoneContactIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Column phoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);
        final Column contactFirstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column contactLastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column createdTimeColumn = contactTable.getColumn(MCOCONTACT.CREATED_TIME);
        final Column contactIdColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final String fieldName = ContactFields.PHONES;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final Long fieldId = fieldInfo.id();
        final Collection<String> dismissedPhones = getDismissedValues(orgId, fieldInfo.sourceId(), fieldId);
        final List<JoinClause> joinClauses = List.of(new JoinClause(contactIdColumn, phoneContactIdColumn, leftJoin),
                new JoinClause(phoneIdColumn, phoneTable.getPKColumn(), leftJoin));
        final List<Column> selectedColumns = List.of(phoneContactIdColumn, phoneIdColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactFirstNameColumn, contactLastNameColumn, contactTable.getColumn(MCOCONTACT.ORG_ID), createdTimeColumn);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId));

        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(selectedColumns);
        selectQuery.addSelectClause(phoneTable);
        final SelectQuery innerSelectQuery = SelectQuery.get(phoneTable);

        innerSelectQuery.addSelectClause(phoneNumberColumn);
        innerSelectQuery.addJoinClause(new JoinClause(phoneTable.getPKColumn(), phoneIdColumn));
        innerSelectQuery.addGroupByClause(new GroupByClause(phoneNumberColumn));
        innerSelectQuery.addHavingClause(new HavingClause(Criteria.gt(phoneNumberColumn.count(), 1)));
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.setWhereClause(dismissedPhones.isEmpty() ? whereClause :
                whereClause.and(Criteria.notIn(phoneNumberColumn, dismissedPhones)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(phoneNumberColumn, innerSelectQuery))
                .and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId)));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            dataContainer.indexRows(phoneNumberColumn);
            final Stream<Row> phoneRows = dataContainer.getRows(phoneTable);

            phoneRows.filter(Objects::nonNull).forEach(row -> {
                final String phoneNumber = row.get(phoneNumberColumn);

                if (!contactSet.contains(phoneNumber)) {
                    final Row phoneRow = dataContainer.getIndexedRow(phoneNumberColumn, phoneNumber);
                    final List<Contact> contacts = new ArrayList<>();
                    final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                            new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneRow.getPKValue())));

                    contactPhoneRows.forEach(contactPhoneRow -> {
                        final Row contactRow = dataContainer.getRow(contactTable, (Long) contactPhoneRow.get(MCONPHONEFIELDS.ENTITY_ID));
                        final Contact contact = new Contact();
                        final Phone phone = new Phone();

                        phone.setNumber(phoneNumber);
                        contact.setId(contactRow.getPKValue());
                        contact.setOrgId(orgId);
                        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));

                        contact.addPhone(phone);
                        contacts.add(contact);
                        contactSet.add(phoneNumber);
                    });
                    contactList.add(contacts);
                }
            });
        }

        return contactList;
    }

    /**
     * Converts the row into {@link Contact}
     *
     * @param row           Holds the attribute values of {@link Contact}
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Contact}
     */
    private Contact convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Contact contact = new Contact();
        final Long orgId = row.getOrgId();
        final Long lifeCycleStageId = row.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);
        final Long lifeCycleStatusId = row.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID);
        final Long contactId = row.getPKValue();
        final Long createdById = row.get(MCOCONTACT.CREATED_BY);
        final Long updatedById = row.get(MCOCONTACT.UPDATED_BY);
        final Long salesOwnerId = row.get(MCOCONTACT.OWNER_ID);
        final Collection<Long> userIds = collectUserIds(createdById, updatedById, salesOwnerId);
        final Table contactEmailTable = contactManager.getContactEmailDerivedTable();
        final Table contactPhoneTable = contactManager.getContactPhoneDerivedTable();
        final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId)));
        final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId)));

        if (Objects.nonNull(contactEmailRows)) {
            final Table emailTable = emailManager.getTable();
            final Stream<Row> emailRows = dataContainer.getRows(emailTable,
                    new WhereClause(Criteria.in(emailTable.getPKColumn(), contactEmailRows.map(contactEmailRow ->
                            contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID)).collect(Collectors.toSet()))));

            if (Objects.nonNull(emailRows)) {
                contact.setEmails(emailRows.map(emailManager::convertRowToModel).toList());
            }
        }

        if (Objects.nonNull(contactPhoneRows)) {
            final Table phoneTable = phoneManager.getTable();
            final Stream<Row> phoneRows = dataContainer.getRows(phoneTable,
                    new WhereClause(Criteria.in(phoneTable.getPKColumn(), contactPhoneRows.map(contactPhoneRow ->
                            contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID)).collect(Collectors.toSet()))));

            if (Objects.nonNull(phoneRows)) {
                contact.setPhones(phoneRows.map(phoneManager::convertRowToModel).toList());
            }
        }
        contact.setId(contactId);
        contact.setId(row.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(row.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(row.get(MCOCONTACT.LAST_NAME));

        if (Objects.nonNull(lifeCycleStageId)) {
            final LifeCycleStage lifeCycleStage = new LifeCycleStage();

            lifeCycleStage.setId(lifeCycleStageId);
            contact.setLifeCycleStage(lifeCycleStage);
        }

        if (Objects.nonNull(lifeCycleStatusId)) {
            final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

            lifeCycleStatus.setId(lifeCycleStatusId);
            contact.setLifeCycleStatus(lifeCycleStatus);
        }
        contact.setModifiedTime(row.get(MCOCONTACT.MODIFIED_TIME));
        contact.setCreatedTime(row.get(MCOCONTACT.CREATED_TIME));
        contact.setLastAssignedAt(row.get(MCOCONTACT.LAST_ASSIGNED_AT));
        contact.setOwnerId(salesOwnerId);

        contact.setUserIds(userIds);
        setContactUsers(contact, createdById, updatedById, salesOwnerId, getUsersById(orgId, userIds));
        contact.setTerritory(buildPartialTerritory(row.get(MCOCONTACT.TERRITORY_ID)));

        return contact;
    }

    /**
     * Sets createdBy/updatedBy/salesOwner from a pre-fetched users map.
     */
    private void setContactUsers(final Contact contact, final Long createdById, final Long updatedById,
                                 final Long salesOwnerId, final Map<Long, CrmUser> usersById) {
        contact.setCreatedBy(usersById.get(createdById));
        contact.setUpdatedBy(usersById.get(updatedById));
        contact.setSalesOwner(usersById.get(salesOwnerId));
    }

    /**
     * Converts the row into {@link Company}
     *
     * @param row           Holds the attribute values of {@link Company}
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Company}
     */
    private Company convertRowToCompany(final Row row, final DataContainer dataContainer) {
        final Table companyTable = companyManager.getTable();
        final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                row.get(MCONCOMPANYFIELDS.COMPANY_ID))));
        final Company company = new Company();

        company.setName(companyRow.get(MCOCOMPANY.NAME));

        return company;
    }

    /**
     * Fetches the duplicate contact list by email and phone
     *
     * @param orgId   Represents the organization id
     * @param contact Holds the contact attributes
     * @param start   Represents the start value
     * @param limit   Represents the limit value
     * @return {@link List} of contact
     */
    public Collection<Contact> fetchDuplicateContacts(final Long orgId, final Contact contact, final int start, final int limit) {
        final Collection<Contact> contacts = new ArrayList<>();
        final Table contactTable = contactManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final List<JoinClause> joinClauses = getJoinClauses();
        final SelectQuery duplicateContactSelectQuery = SelectQuery.get(contactTable);

        duplicateContactSelectQuery.addJoinClause(joinClauses);
        duplicateContactSelectQuery.addSelectClause(contactPkColumn.distinct());
        duplicateContactSelectQuery.setLimitClause(new LimitClause(start, limit));
        duplicateContactSelectQuery.setWhereClause(getDuplicateContactCondition(contact));
        final DataContainer duplicateContactDataContainer = orgDataStore(orgId).get(duplicateContactSelectQuery);

        if (duplicateContactDataContainer.size(contactTable) > 0) {
            final Table contactEmailTable = contactManager.getContactEmailDerivedTable();
            final Table emailTable = emailManager.getTable();
            final Table phoneTable = phoneManager.getTable();
            final Table contactPhoneTable = contactManager.getContactPhoneDerivedTable();
            final Table contactCompanyTable = contactManager.getContactCompanyTable();
            final Table companyTable = companyManager.getTable();
            final Column companyPkColumn = companyTable.getPKColumn();
            final Column entityIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
            final Column companyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);
            final SelectQuery selectQuery = SelectQuery.get(contactTable);

            selectQuery.addJoinClause(joinClauses);
            selectQuery.addJoinClause(new JoinClause(companyIdColumn, companyPkColumn, JoinClause.JoinType.LEFT));
            selectQuery.addSelectClause(List.of(companyIdColumn, entityIdColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID),
                    contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID),
                    contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), contactTable.getColumn(MCOCONTACT.ORG_ID),
                    contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                    companyTable.getColumn(MCOCOMPANY.NAME), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                    contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE)));
            selectQuery.addSelectClause(emailTable, phoneTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn,
                    duplicateContactDataContainer.getRows(contactTable).map(Row::getPKValue).toList())));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            dataContainer.getRows(contactTable).filter(Objects::nonNull).forEach(contactRow -> {
                final Long contactId = contactRow.getPKValue();
                final Contact duplicateContact = new Contact();

                duplicateContact.setId(contactRow.getPKValue());
                duplicateContact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                duplicateContact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

                final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria.eq(entityIdColumn, contactId)));

                if (Objects.nonNull(contactCompanyRow)) {
                    final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyPkColumn, contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID))));

                    if (Objects.nonNull(companyRow)) {
                        duplicateContact.setCompany(companyManager.convertRowToPartialModel(companyRow));
                    }
                }
                final List<Long> emailIds = dataContainer.getRows(contactEmailTable,
                                new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId)))
                        .map(emailField -> (Long) emailField.get(MCONEMAILFIELDS.EMAIL_ID)).toList();
                final Stream<Row> emailRows = dataContainer.getRows(emailTable, new WhereClause(Criteria.in(emailTable.getPKColumn(), emailIds)));
                final Collection<Email> emails = emailRows.filter(Objects::nonNull).map(emailManager::convertRowToModel).toList();

                duplicateContact.setEmails(emails);

                for (final Email email : emails) {

                    if (Objects.equals(contactRow.get(MCOCONTACT.PRIMARY_EMAIL), email.id())) {
                        duplicateContact.setPrimaryEmail(email);
                        break;
                    }
                }

                final List<Long> phoneIds = dataContainer.getRows(contactPhoneTable,
                                new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId)))
                        .map(phoneField -> (Long) phoneField.get(MCONPHONEFIELDS.PHONE_ID)).toList();
                final Stream<Row> phoneRows = dataContainer.getRows(phoneTable, new WhereClause(Criteria.in(phoneTable.getPKColumn(), phoneIds)));
                final Collection<Phone> phones = phoneRows.filter(Objects::nonNull).map(phoneManager::convertRowToModel).toList();

                duplicateContact.setPhones(phones);

                for (final Phone phone : phones) {

                    if (Objects.equals(contactRow.get(MCOCONTACT.PRIMARY_PHONE), phone.id())) {
                        duplicateContact.setPrimaryPhone(phone);
                        break;
                    }
                }
                contacts.add(duplicateContact);
            });
        }
        return contacts;
    }

    /**
     * Merge the contact request payload with primary contact
     *
     * @param orgId            Represents the organization id
     * @param primaryContactId Represents the primary contact id
     * @param contact          Holds the contact attributes
     * @return {@link Map} which contains the merged records
     */
    public Map<String, Object> mergeContactDuplicates(final Long orgId, final Long primaryContactId, final Contact contact) {
        final Table contactTable = contactManager.getTable();
        final DataContainer dataContainer = contactManager.getContactDataContainer(orgId, List.of(primaryContactId));
        final Row primaryContactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactTable.getPKColumn(), primaryContactId)));
        final Contact primaryContact = Objects.nonNull(primaryContactRow)
                ? contactManager.convertRowToModel(orgId, primaryContactRow, false, dataContainer) : null;

        if (Objects.nonNull(primaryContact)) {
            final Collection<Email> primaryContactEmails = primaryContact.getEmails();
            final Collection<Phone> primaryContactPhones = primaryContact.getPhones();
            final Collection<Phone> phones = contact.getPhones();
            final Collection<Email> emails = contact.getEmails();
            final ValueHolder valueHolder = primaryContact.getValueHolder();
            final String emailKey = ContactFields.EMAILS;
            final String phoneKey = ContactFields.PHONES;
            final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, List.of(emailKey, phoneKey));
            final Set<Email> primaryEmails = new HashSet<>();
            final Set<Phone> primaryPhones = new HashSet<>();

            if (Objects.nonNull(primaryContactEmails)) {
                primaryEmails.addAll(primaryContactEmails);
            }

            if (Objects.nonNull(emails)) {
                primaryEmails.addAll(emails);
            }

            if (Objects.nonNull(primaryContactPhones)) {
                primaryPhones.addAll(primaryContactPhones);
            }

            if (Objects.nonNull(phones)) {
                primaryPhones.addAll(phones);
            }

            primaryContact.setEmails(primaryEmails);
            primaryContact.setPhones(primaryPhones);

            valueHolder.put(contactFields.get(emailKey).id(), primaryEmails);
            valueHolder.put(contactFields.get(phoneKey).id(), primaryPhones);
            primaryContact.setValueHolder(valueHolder);

            final ModelDescriptorLoader<Contact> descriptor = new ModelDescriptorLoaderImpl<>();
            final Set<String> fieldNames = descriptor.getFieldNames(primaryContact, EXCLUDED_FIELDS);

            if (!fieldNames.isEmpty()) {
                descriptor.mergeProperties(fieldNames, primaryContact, contact);
            }
            final DataContainer targetContainer = DataContainer.create();

            defaultFieldValuesLoader.load(primaryContact);
            contactManager.loadContactIntoContainer(false, contactManager.getContactFields(orgId), primaryContact, dataContainer, targetContainer);
            modificationHandler.loadModificationDetailsIntoContainer(orgId, contact.getUpdatedBy().id(), List.of(primaryContactId), Source.CONTACT, targetContainer);
            orgDataStore(orgId).commitChanges(targetContainer);
        }
        final Map<String, Object> response = new HashMap<>();

        response.put(ContactFields.MERGED_RECORDS, primaryContactId);
        response.put(ContactFields.CONTACT, primaryContact);

        return response;
    }

    /**
     * <p>
     * Fetches the total count of duplicate contacts based on duplicate emails
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the total email duplicate count
     */
    public int getDuplicateContactsCountByEmail(final long orgId) {
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column emailValueCountColumn = emailValueColumn.count();
        final String fieldName = ContactFields.EMAILS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final Collection<String> dismissedEmails = getDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id());

        selectQuery.addSelectClause(emailValueCountColumn);
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin)));
        selectQuery.addGroupByClause(new GroupByClause(emailValueColumn));
        selectQuery.addHavingClause(new HavingClause(Criteria.gt(emailValueCountColumn, 1)));

        if (!dismissedEmails.isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.notIn(emailValueColumn, dismissedEmails)));
        }
        return orgDataStore(orgId).getData(selectQuery).size();
//        final Counter duplicateCounter = new Counter(); TODO Replace with column receiver
//
//        orgDataStore(orgId).fetchData(selectQuery, (ColumnReceiver) value -> duplicateCounter.increment());
//
//        return duplicateCounter.getCount();
    }

    /**
     * <p>
     * Fetches the total count of duplicate contacts based on duplicate phones
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the total phone duplicate count
     */
    public int getDuplicateContactsCountByPhone(final long orgId) {
        final Table contactTable = contactManager.getTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column phoneNumberCountColumn = phoneNumberColumn.count();
        final String fieldName = ContactFields.PHONES;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final Collection<String> dismissedPhones = getDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id());

        selectQuery.addSelectClause(phoneNumberCountColumn);
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin)));
        selectQuery.addGroupByClause(new GroupByClause(phoneNumberColumn));
        selectQuery.addHavingClause(new HavingClause(Criteria.gt(phoneNumberCountColumn, 1)));

        if (!dismissedPhones.isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.notIn(phoneNumberColumn, dismissedPhones)));
        }
        return orgDataStore(orgId).getData(selectQuery).size();

//        final Counter duplicateCounter = new Counter(); TODO replace with column receiver
//
//        orgDataStore(orgId).fetchData(selectQuery, (ColumnReceiver) value -> duplicateCounter.increment());
//
//        return duplicateCounter.getCount();
    }

    /**
     * <p>
     * Updates the contact related timelines into the target {@link DataContainer}.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param contactId           Represents the primary contact id
     * @param duplicateContactIds Represents the secondary/duplicate contact ids
     * @param primaryContactName  Represents the primary contact full name before merge
     * @param dataContainer       Holds the information of existing contact attributes
     * @param targetContainer     Holds the updated information of contact attributes
     */
    private void loadContactTimelineIntoContainer(final long orgId, final long userId, final Long contactId, final Collection<Long> duplicateContactIds,
                                                  final String primaryContactName, final DataContainer dataContainer,
                                                  final DataContainer targetContainer) {
        final Table contactTable = contactManager.getTable();
        final Row primaryRow = dataContainer.getRow(contactTable, contactId);

        if (Objects.nonNull(primaryRow)) {
            final Collection<Row> secondaryRows = dataContainer.getRows(contactTable, new WhereClause(Criteria.in(contactTable.getPKColumn(), duplicateContactIds))).toList();
            final Collection<String> secondaryFullNames = new ArrayList<>();

            secondaryRows.stream().filter(Objects::nonNull).forEach(row ->
                    secondaryFullNames.add(Contact.getFullName(row.get(MCOCONTACT.FIRST_NAME), row.get(MCOCONTACT.LAST_NAME))));

            loadMergeTimelineIntoContainer(orgId, userId, Timeline.Category.CONTACT, duplicateContactIds, contactId,
                    new FieldTimeline(secondaryFullNames, primaryContactName, new I18nKey("i18n.contact.fields.firstName")),
                    dataContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Fetches marketing list contact rows with the associated marketing list id and name.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the collection of contact ids
     * @return a {@link DataContainer} containing marketing list contact rows
     */
    private DataContainer getContactMarketingListDataContainer(final Long orgId, final Collection<Long> contactIds) {
        final Table marketingListContactTable = marketingListContactManager.getMarketingListContactTable();
        final Table marketingListTable = marketingListContactManager.getMarketingListTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListContactTable);

        selectQuery.addJoinClause(new JoinClause(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListTable.getPKColumn()));
        selectQuery.addSelectClause(marketingListContactTable, marketingListTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the marketing lists associated with the given contacts for preview.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param primaryContact   Represents the primary contact for which marketing lists are loaded
     * @param secondaryContact Represents the secondary contact for which marketing lists are loaded
     */
    private void loadMarketingListsForPreview(final Long orgId, final Contact primaryContact, final Contact secondaryContact) {
        final DataContainer dataContainer = getContactMarketingListDataContainer(orgId, List.of(primaryContact.id(), secondaryContact.id()));

        primaryContact.setMarketingLists(marketingListContactManager.buildMarketingLists(orgId, primaryContact.id(), dataContainer));
        secondaryContact.setMarketingLists(marketingListContactManager.buildMarketingLists(orgId, secondaryContact.id(), dataContainer));
    }
}