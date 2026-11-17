package com.tlc.crm.core.api.contact.manager;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.action.service.EmailSummaryActionService;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.api.insights.service.EntityInsightsToSummaryInfoMapper;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.dropdown.contact.ContactSourceStaticOption;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.field.fieldAudit.resolver.ContactFieldAuditResolver;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.OMDPDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.MCONDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.MCONDROPDOWNFIELDS;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOMADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCONADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MWFCONTACTWEBFORM;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTAGE;

import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTATUS;
import com.tlc.crm.core.sql.resource.OMCONFOLLOWERS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.DefaultFieldsDataLoader;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.sql.resource.OMWFWEBFORM;

import com.tlc.i18n.I18nKey;

import com.tlc.l10n.model.country.Country;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Contact} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ContactManager extends EntityConfigManager<Contact> {

    private final CompanyManager companyManager;
    private final EntityLoader entityLoader;
    private final EntityHandler entityHandler;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final AddressManager addressManager;
    private final CommonTimelineManager commonTimelineManager;
    private final DefaultFieldsDataLoader defaultFieldsDataLoader;
    private final WebFormManager webFormManager;
    private final UserService userService;
    private final ContactSyncManager contactSyncManager;
    private final OptionService optionService;
    private final AuditLogger auditLogger;
    private final ModificationHandler modificationHandler;
    private final EmailSettingsService emailSettingsService;
    private final SourceFollowersManager sourceFollowersManager;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final TerritoryManager territoryManager;
    private final FieldAuditHandler fieldAuditHandler;
    private final ContactFieldAuditResolver fieldAuditResolver;
    private final MarketingListContactManager marketingListContactManager;
    private final EntityInsightsToSummaryInfoMapper entityInsightsToSummaryInfoMapper;

    private static class Instance {
        private static final ContactManager INSTANCE = new ContactManager();
    }

    private ContactManager() {
        companyManager = CompanyManager.getInstance();
        entityLoader = EntityLoader.getInstance();
        entityHandler = EntityHandler.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        addressManager = AddressManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        defaultFieldsDataLoader = DefaultFieldsDataLoader.getInstance();
        webFormManager = WebFormManager.getInstance();
        userService = UserService.getInstance();
        contactSyncManager = ContactSyncManager.getInstance();
        optionService = OptionService.getInstance();
        auditLogger = AuditLogger.getInstance();
        modificationHandler = ModificationHandler.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        territoryManager = TerritoryManager.getInstance();
        fieldAuditHandler = FieldAuditHandler.getInstance();
        fieldAuditResolver = ContactFieldAuditResolver.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
        entityInsightsToSummaryInfoMapper = EntityInsightsToSummaryInfoMapper.getInstance();
    }

    public static ContactManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public Table getTable() {
        return Table.get(MCOCONTACT.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return Contact follower table.
     */
    @Override
    protected Table getFollowersTable() {
        return Table.get(OMCONFOLLOWERS.TABLE);
    }

    /**
     * Creates a derived table for contact email fields based on the given field information.
     *
     * @return the derived table with the filtered contact email field data
     */
    public Table getContactEmailDerivedTable() {
        final String fieldName = ContactFields.EMAILS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final Table contactEmailTable = getContactEmailTable();
        final SelectQuery selectQuery = SelectQuery.get(contactEmailTable);

        selectQuery.addSelectClause(contactEmailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldInfo.id())));

        return DerivedTable.get(contactEmailTable.getName(), fieldInfo.idAsString(), selectQuery);
    }

    /**
     * Creates a derived table for contact phone fields based on the given field information.
     *
     * @return the derived table with the filtered contact phone field data
     */
    public Table getContactPhoneDerivedTable() {
        final Table contactPhoneTable = getContactPhoneTable();
        final String fieldName = ContactFields.PHONES;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, fieldName).get(fieldName);
        final SelectQuery selectQuery = SelectQuery.get(contactPhoneTable);

        selectQuery.addSelectClause(contactPhoneTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldInfo.id())));

        return DerivedTable.get(contactPhoneTable.getName(), fieldInfo.idAsString(), selectQuery);
    }

    /**
     * <p>
     * Gets the contact dropdown tables
     * </p>
     *
     * @return The contact dropdown table
     */
    public Table getContactDropdownTable() {
        return Table.get(MCONDROPDOWNFIELDS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EntityHandler}
     */
    @Override
    protected EntityHandler getEntityHandler() {
        return entityHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Column}
     */
    @Override
    protected Optional<Column> getTerritoryColumn() {
        return Optional.ofNullable(getTable().getColumn(MCOCONTACT.TERRITORY_ID));
    }

    /**
     * <p>
     * Fetches the {@link Contact} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Represents the organization id
     * @param ids   Collection of {@link Contact} ids
     * @return Collection of {@link Contact}
     */
    @Override
    public Collection<Contact> get(final Long orgId, final Collection<Long> ids) {
        final Table contactTable = getTable();
        final DataContainer dataContainer = fetchContactContainer(orgId, ids);
        final Stream<Row> rows = dataContainer.getRows(contactTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        } else {
            return rows.map(row -> convertRowToModel(orgId, row, false, dataContainer)).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @return A flag that indicates whether the user is the follower.
     */
    @Override
    public boolean isUserFollowingEntity(final long orgId, final long userId, final long entityId) {
        return sourceFollowersManager.isFollowerOfEntity(orgId, userId, entityId, Source.CONTACT);
    }

    /**
     * <p>
     * Using the entity IDs and user ID, checks if the user is the follower of the entities
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    @Override
    public Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds) {
        return sourceFollowersManager.isUserFollowingEntities(orgId, userId, entityIds, Source.CONTACT);
    }

    /**
     * <p>
     * Fetches the {@link Contact} that has the id value mentioned in the parameter
     * </p>
     *
     * @param orgId         Represents the organisation ID
     * @param contactId     Represents the contact ID
     * @param neededFields  Fields values which are needed
     * @return {@link Contact} linked with the id
     */
    public Contact getContactWithTemplateFields(final long orgId, final long contactId, final Collection<FieldInfo> neededFields) {
        final Table contactTable = getTable();
        final Optional<SelectQuery> optionalSelectQuery = buildContactBaseQuery(contactTable, List.of(contactId));

        if (optionalSelectQuery.isPresent()) {
            final SelectQuery selectQuery = optionalSelectQuery.get();
            final DataContainer dataContainer = getEntityContainer(orgId, List.of(contactId), selectQuery, neededFields);
            final Stream<Row> rows = dataContainer.getRows(getTable());

            if (Objects.nonNull(rows)) {
                return rows.map(row -> convertRowToModel(orgId, row, false, dataContainer)).findFirst().orElse(null);
            }
        }
        return null;
    }

    /**
     * <p>
     * Builds the contact base query
     * </p>
     *
     * @param contactTable  Represents the contact table
     * @param ids           Represents the collection of contact ids
     * @return The optional {@link SelectQuery}
     */
    public Optional<SelectQuery> buildContactBaseQuery(final Table contactTable, final Collection<Long> ids) {
        if (isEntityIdCollectionEmpty(ids)) {
            return Optional.empty();
        }
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(List.of(
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.OWNER_ID), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), contactTable.getColumn(MCOCONTACT.CREATED_BY), contactTable.getColumn(MCOCONTACT.UPDATED_BY))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), ids)));

        return Optional.of(selectQuery);
    }

    /**
     * Retrieves the total count of unique contact numbers for a given organization.
     *
     * @param orgId    The ID of the organization.
     * @param criteria Filtering criteria for phone numbers.
     * @return The count of distinct contact numbers associated with the organization.
     */
    public long getTotalContactNumbersCount(final long orgId, final DropdownCriteria criteria) {
        final Table contactTable = getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(phoneTable);
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);

        selectQuery.addSelectClause(contactPkColumn.countWithDistinct());
        selectQuery.addJoinClause(new JoinClause(phoneTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID)));
        selectQuery.addJoinClause(new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactPkColumn));
        selectQuery.setWhereClause(new WhereClause(criteria.build(phoneTable.getColumn(OMETPHONE.NUMBER))).or(criteria.build(firstNameColumn))
                .or(criteria.build(lastNameColumn)).or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim()))));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(contactTable);

        return Objects.isNull(row) ? NumericalConstant.ZERO : row.getPKValue();
    }

    /**
     * Retrieves a collection of contacts along with their associated phone numbers for a given organization.
     *
     * @param orgId         The ID of the organization.
     * @param criteria      Filtering criteria for phone numbers.
     * @param dropdownLimit Limits the number of results (pagination).
     * @return A collection of contacts with their associated phone numbers.
     */
    public Collection<Contact> getContactNumbers(final long orgId, final DropdownCriteria criteria, final DropdownLimit dropdownLimit) {
        final Table contactTable = getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column phonePkColumn = phoneTable.getPKColumn();
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);
        final Column phoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(firstNameColumn);
        selectQuery.addSelectClause(lastNameColumn);
        selectQuery.addSelectClause(phoneNumberColumn);
        selectQuery.addSelectClause(phoneTable.getColumn(OMETPHONE.COUNTRY_ID));
        selectQuery.addSelectClause(contactPhoneTable);
        selectQuery.addJoinClause(new JoinClause(contactPkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)));
        selectQuery.addJoinClause(new JoinClause(phoneIdColumn, phonePkColumn));
        selectQuery.setWhereClause(new WhereClause(criteria.build(phoneNumberColumn)).or(criteria.build(firstNameColumn))
                .or(criteria.build(lastNameColumn)).or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim()))));
        selectQuery.addOrderByClause(new OrderByClause(phonePkColumn));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Contact> contacts = new ArrayList<>();

        dataContainer.indexRows(contactPkColumn);

        dataContainer.getRows(phoneTable).forEach(phoneRow -> {
            final Phone phone = new Phone();
            final Integer countryId = phoneRow.get(OMETPHONE.COUNTRY_ID);
            final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                    new WhereClause(Criteria.eq(phoneIdColumn, phoneRow.getPKValue())));

            phone.setId(phoneRow.getPKValue());
            phone.setNumber(phoneRow.get(OMETPHONE.NUMBER));

            if (Objects.nonNull(countryId)) {
                phone.setCountry(Country.getCountryById(countryId));
            }

            contactPhoneRows.forEach(contactPhoneRow -> {
                final Row contactRow = dataContainer.getIndexedRow(contactPkColumn, contactPhoneRow.get(MCONPHONEFIELDS.ENTITY_ID));
                final Contact contact = new Contact();

                contact.setId(contactRow.getPKValue());
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                contact.setPrimaryPhone(phone);
                contacts.add(contact);
            });
        });
        return contacts;
    }

    /**
     * <p>
     * Updates the lifecycle stage of the contacts.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param oldStageIds Represents the old stage ids.
     * @param newStageId  Represents the new stage id.
     * @param newStatusId Represents the new status id.
     */
    public void updateLifecycleStage(final Long orgId, final Collection<Long> oldStageIds, final Long newStageId,
                                     final Long newStatusId, final Option lostReason) {
        updateLifecycle(orgId, newStageId, newStatusId, lostReason, new WhereClause(Criteria.in(getTable().getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID), oldStageIds)));
    }

    /**
     * <p>
     * Updates the lifecycle status of the contacts.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param oldStatusIds Represents the old status ids.
     * @param newStageId   Represents the new stage id.
     * @param newStatusId  Represents the new status id.
     */
    public void updateLifecycleStatus(final Long orgId, final Collection<Long> oldStatusIds, final Long newStageId,
                                      final Long newStatusId, final Option lostReason) {
        updateLifecycle(orgId, newStageId, newStatusId, lostReason, new WhereClause(Criteria.in(getTable().getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID), oldStatusIds)));
    }

    /**
     * <p>
     * Fetches the contacts where the given phone number is mapped.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param phoneNumber Represents the phone number.
     * @return Collection of {@link Contact}.
     */
    public Collection<Contact> getContactsByPhone(final Long orgId, final String phoneNumber) {
        final Table contactTable = getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.FIRST_NAME));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.LAST_NAME));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.OWNER_ID));
        selectQuery.addSelectClause(contactPhoneTable);
        selectQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)));
        selectQuery.addJoinClause(new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.contains(phoneTable.getColumn(OMETPHONE.NUMBER), phoneNumber)));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Stream<Row> contactRows = dataContainer.getRows(contactTable);

        return contactRows.map(contactRow -> {
            final Row contactPhoneRow = dataContainer.getRow(contactPhoneTable,
                    new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactRow.getPKValue())));
            final Contact contact = new Contact();
            final Phone phone = new Phone();

            phone.setId(contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID));
            phone.setNumber(phoneNumber);
            contact.setId(contactRow.getPKValue());
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setPrimaryPhone(phone);
            contact.setSalesOwner(userService.getUser(orgId, contactRow.get(MCOCONTACT.OWNER_ID)));

            return contact;
        }).toList();
    }

    /**
     * <p>
     * Updates the lifecycle stage & status of the contact.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param newStageId  Represents the new stage id.
     * @param newStatusId Represents the new status id.
     * @param lostReason  Represents the lost reason.
     * @param whereClause Represents the {@link WhereClause}.
     */
    private void updateLifecycle(final Long orgId, final Long newStageId, final Long newStatusId, final Option lostReason,
                                 final WhereClause whereClause) {
        final Table contactTable = getTable();
        final Table contactDropdownTable = getContactDropdownTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
        final FieldInfo lostReasonField = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, lostReasonKey).get(lostReasonKey);
        final Criteria fieldIdCriteria = Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), lostReasonField.id());
        final Column entityIdColumn = contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID);

        selectQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), entityIdColumn, JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final boolean isLostReasonNull = Objects.isNull(lostReason);

        dataContainer.getRows(contactTable).forEach(contactRow -> {
            final Long contactId = contactRow.getPKValue();
            final WhereClause criteria = new WhereClause(Criteria.eq(entityIdColumn, contactId)).and(fieldIdCriteria);
            final Optional<Row> oldLostReasonRow = Optional.ofNullable(dataContainer.getRow(contactDropdownTable, criteria));

            contactRow.set(MCOCONTACT.LIFE_CYCLE_STAGE_ID, newStageId);
            contactRow.set(MCOCONTACT.LIFE_CYCLE_STATUS_ID, newStatusId);

            if (isLostReasonNull) {
                oldLostReasonRow.ifPresent(targetContainer::deleteRow);
            } else {
                final Row lostReasonRow = oldLostReasonRow.orElseGet(() -> new Row(contactDropdownTable));

                if (lostReasonRow.isNewRow()) {
                    lostReasonRow.set(MCONDROPDOWNFIELDS.ENTITY_ID, contactId);
                    lostReasonRow.set(MCONDROPDOWNFIELDS.FIELD_ID, lostReason.getFieldId());
                }
                lostReasonRow.set(MCONDROPDOWNFIELDS.OPTION_ID, lostReason.id());
                targetContainer.updateRow(lostReasonRow);
            }
            targetContainer.updateRow(contactRow);
        });
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Gets the {@link SummaryInfo} of a contact.
     *
     * @param orgId   Refers the organization id.
     * @param contact Refers the {@link Contact} for which the summary details is to be fetched.
     */
    public final void loadSummaryDetails(final Long orgId, final Contact contact) {
        final Long contactId = contact.id();

        if (Objects.nonNull(contactId)) {
            final String entityType = Contact.ENTITY_TYPE;
            final SummaryInfo summaryInfo = new SummaryInfo();

            summaryInfo.setEmailSummary(EmailSummaryActionService.getInstance().get(orgId, contactId, Source.CONTACT));
            summaryInfo.setActivitySummary(SourceActivityManager.getInstance().getActivitySummary(orgId, contactId, entityType));
            summaryInfo.setDealStatistics(getDealStatistics(orgId, contactId));
            summaryInfo.setWebFormName(getWebFormName(orgId, contactId));
            summaryInfo.setTags(SourceTagManager.getInstance().getMappedTags(orgId, contactId, entityType));
            summaryInfo.setDealsByState(DealManager.getInstance().getContactDeals(orgId, contactId));

            contact.setSummaryInfo(summaryInfo);
        }
    }

    /**
     * <p>
     *   Loads the summary info data
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param contact Represents the contact whose summary info will be set.
     */
    public final void loadSummaryInfo(final Long orgId, final Contact contact) {
        final Long contactId = contact.id();

        if (Objects.nonNull(contactId)) {
            final SummaryInfo summaryInfo = entityInsightsToSummaryInfoMapper.buildSummaryInfo(orgId, contactId, Source.CONTACT);

            summaryInfo.setTags(SourceTagManager.getInstance().getMappedTags(orgId, contactId, Contact.ENTITY_TYPE));
            summaryInfo.setWebFormName(getWebFormName(orgId, contactId));
            summaryInfo.setDealsByState(DealManager.getInstance().getContactDeals(orgId, contactId));

            contact.setSummaryInfo(summaryInfo);
        }
    }

    /**
     * <p>
     * Loads the contact details into data container
     * </p>
     *
     * @param orgId Represents the organization id
     * @param ids   Collection of {@link Contact} ids
     * @return {@link DataContainer}
     */
    public DataContainer fetchContactContainer(final Long orgId, final Collection<Long> ids) {
        if (isEntityIdCollectionEmpty(ids)) {
            return DataContainer.create();
        }
        final Table contactTable = getTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final Collection<FieldInfo> contactFields = secureFieldService.getFieldsBySource(orgId, Contact.ENTITY_TYPE);
        final Table companyTable = Table.get(MCOCOMPANY.TABLE);
        final Map<Table, Collection<Column>> requiredColumns = new HashMap<>();
        final Map<Table, JoinClause.JoinType> joins = new HashMap<>();
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(contactTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), ids)));
        requiredColumns.put(companyTable, List.of(companyTable.getPKColumn(),
                companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.WEBSITE),
                companyTable.getColumn(MCOCOMPANY.ORG_ID)));
        requiredColumns.put(territoryTable, List.of(territoryTable.getPKColumn(),
                territoryTable.getColumn(OMCRMTERRITORY.NAME)));
        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.TERRITORY_ID), territoryPkColumn, JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(List.of(territoryPkColumn, territoryTable.getColumn(OMCRMTERRITORY.NAME)));
        SourceTagManager.getInstance().loadJoinsAndSelectClause(Source.CONTACT, contactTable, selectQuery);
        marketingListContactManager.loadJoinsAndSelectClause(contactTable, selectQuery);

        return getEntityContainer(orgId, selectQuery, ids, contactFields, requiredColumns, joins);
    }

    /**
     * Builds the {@link SelectQuery} to fetch contact related data
     *
     * @param orgId      Represents the organisation id
     * @param contactIds Collection of {@link Contact} ids.
     * @return {@link SelectQuery} to fetch contact related data
     */
    public DataContainer getContactDataContainer(final Long orgId, final Collection<Long> contactIds) {
        return getContactDataContainer(orgId, contactIds, secureFieldService.getFieldsBySource(orgId, Contact.ENTITY_TYPE));
    }

    /**
     * Builds the {@link SelectQuery} to fetch contact related data
     *
     * @param orgId      Represents the organisation id
     * @param contactIds Collection of {@link Contact} ids.
     * @return {@link SelectQuery} to fetch contact related data
     */
    private DataContainer getContactDataContainer(final Long orgId, final Collection<Long> contactIds, final Collection<FieldInfo> fields) {
        return getEntityContainer(orgId, contactIds, fields);
    }

    /**
     * Loads the select and join clauses into the {@link SelectQuery}
     *
     * @param selectQuery to be loaded with the select and join clauses
     */
    public void loadSelectAndJoinClauses(final SelectQuery selectQuery) {
        final Table contactCompanyTable = getContactCompanyTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table lifeCycleStageTable = lifeCycleStageManager.getTable();
        final Table contactTable = getTable();
        final Table addressTable = addressManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table contactAddressTable = getContactAddressTable();
        final Column contactIdColumn = contactTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final List<Column> selectedColumns = List.of(contactCompanyTable.getPKColumn(),
                contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID),
                contactEmailTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactPhoneTable.getPKColumn());
        final Collection<Table> selectedTables = selectQuery.getTables();

        loadEntityMapperTableSelectAndJoinClause(contactAddressTable, selectedTables, selectQuery, contactIdColumn);
        loadEntityMapperTableSelectAndJoinClause(contactPhoneTable, selectedTables, selectQuery, contactIdColumn);
        loadEntityMapperTableSelectAndJoinClause(contactEmailTable, selectedTables, selectQuery, contactIdColumn);
        loadEntityMapperTableSelectAndJoinClause(contactCompanyTable, selectedTables, selectQuery, contactIdColumn);

        final List<JoinClause> joinClauses = List.of(new JoinClause(contactTable.
                        getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID), lifeCycleStageTable.getPKColumn()),
                new JoinClause(contactAddressTable.getColumn(MCONADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn(), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin));

        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(selectedColumns);
        selectQuery.addSelectClause(lifeCycleStageTable, emailTable, phoneTable, addressTable, companyTable);
    }

    /**
     * <p>
     * Fetches the {@link Contact} for the given id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param id     Contact id.
     * @param fields Required fields.
     * @return {@link Contact} holds the details of the contact.
     */
    public Contact get(final Long orgId, final Long id, final Collection<FieldInfo> fields) {
        final Table table = getTable();
        final DataContainer dataContainer = getContactDataContainer(orgId, List.of(id), fields);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : convertRowToModel(orgId, row, true, dataContainer);
    }

    /**
     * <p>
     * Fetches the collection of {@link Contact} with partial data
     * </p>
     *
     * @param orgId                Represents the org id
     * @param dropdownLimit        Represents the dropdown limit
     * @param dropdownCriteria     Represents the dropdown criteria
     * @param includePhoneCriteria Represents the flag to indicate whether to include phone number criteria in where clause
     * @return the collection of {@link Contact} with partial data
     */
    public Collection<Contact> partialGet(final Long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria, final boolean includePhoneCriteria) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final Column primaryPhoneColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE);
        final Column emailTablePKColumn = emailTable.getPKColumn();
        final Column phoneTablePKColumn = phoneTable.getPKColumn();
        final WhereClause nameCriteria = new WhereClause(dropdownCriteria.build(firstNameColumn))
                .or(dropdownCriteria.build(lastNameColumn)).or(dropdownCriteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim())));
        final WhereClause whereClause = includePhoneCriteria
                ? WhereClauseUtil.connect(nameCriteria, new WhereClause(dropdownCriteria.build(phoneTable.getColumn(OMETPHONE.NUMBER))), Connector.OR)
                : nameCriteria;

        selectQuery.addSelectClause(List.of(firstNameColumn, lastNameColumn, primaryEmailColumn,
                primaryPhoneColumn, contactTable.getColumn(MCOCONTACT.ORG_ID)));
        selectQuery.addSelectClause(emailTable, phoneTable);
        selectQuery.addJoinClause(List.of(new JoinClause(primaryEmailColumn, emailTablePKColumn, leftJoin),
                new JoinClause(primaryPhoneColumn, phoneTablePKColumn, leftJoin)));

        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        selectQuery.setWhereClause(whereClause);

        selectQuery.addOrderByClause(new OrderByClause(firstNameColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(contactTable).map(contactRow -> {
            final Contact contact = new Contact();
            final Long primaryEmailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);
            final Long primaryPhoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

            contact.setId(contactRow.getPKValue());
            contact.setOrgId(orgId);
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));

            if (Objects.nonNull(primaryEmailId)) {
                final Email email = emailManager.convertRowToModel(dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTablePKColumn, primaryEmailId))));

                contact.setPrimaryEmail(email);
                contact.addEmail(email);
            }

            if (Objects.nonNull(primaryPhoneId)) {
                final Phone phone = phoneManager.convertRowToModel(dataContainer.getRow(phoneTable, new WhereClause(Criteria.eq(phoneTablePKColumn, primaryPhoneId))));

                contact.setPrimaryPhone(phone);
                contact.addPhone(phone);
            }

            return contact;
        }).toList();
    }

    /**
     * <p>
     * Fetches the user defined entity of an organization along with system entity
     * </p>
     *
     * @param orgId Represents the organization id
     * @return Collection of entity
     */
    public Collection<FieldInfo> getContactFields(final Long orgId) {
        final Collection<String> unwantedFields = Set.of(ContactFields.COMPANY);
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(orgId, Contact.ENTITY_TYPE);

        return fields.stream().filter(crmField -> {
            final String aliasName = crmField.aliasName();

            return !(Objects.nonNull(aliasName) && unwantedFields.contains(aliasName));
        }).toList();
    }

    /**
     * <p>
     * Saves the attributes of {@link Contact} into the database
     * </p>
     *
     * @param contact An entity that holds the attributes which have to be saved into the database
     */
    @Override
    public Contact create(final Contact contact) {
        final DataContainer targetContainer = DataContainer.create();
        final long orgId = contact.orgId();

        loadContactIntoContainer(true, contact, targetContainer);
        sourceFollowersManager.loadFollowerIfExists(orgId, contact.getSalesOwner(), contact.id(), Source.CONTACT, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        auditLogger.log(orgId, List.of(new AuditLogData(contact.getCreatedBy().id(), contact.id(), contact, AuditSource.CONTACT, AuditAction.ADDED)));

        return contact;
    }

    /**
     * <p>
     * Updates the given contacts
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param contacts Collection of contacts that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Contact> contacts) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<FieldInfo> fields = getContactFields(orgId);
        final Collection<Long> ids = contacts.stream().map(Contact::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = fetchContactContainer(orgId, ids);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditSource auditSource = AuditSource.CONTACT;
        final AuditAction auditAction = AuditAction.UPDATED;

        contacts.forEach(contact -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadContactIntoContainer(false, fields, contact, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(contact.id());

                loadCompleteContactInfo(orgId, contact, targetContainer);
                final Contact oldContact = convertRowToModel(orgId, entityContainer.getRow(getTable()), false, entityContainer);

                auditLogDataList.add(new AuditLogData(contact.getUpdatedBy().id(), contact.id(), oldContact, contact, auditSource, auditAction));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Contact update failed Org ID : {} ID : {} reason : {}", orgId, contact.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed", errorCode), ErrorKeys.UPDATE_ACTION_FAILED, contact.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * <p>
     * Updates the lifecycle stage/status of the contact.
     * </p>
     *
     * @param contact Holds the details of the contact.
     */
    public void updateLifeCycle(final Contact contact) {
        final DataContainer targetDataContainer = DataContainer.create();

        loadContactLifeCycleIntoTimeline(contact, targetDataContainer);
        updateLifeCycle(contact.orgId(), List.of(contact), targetDataContainer);
    }

    /**
     * <p>
     * Updates the lifecycle stage/status of the contacts.
     * </p>
     *
     * @param orgId           Represents the organisation id.
     * @param contacts        Holds the details of the contacts.
     * @param targetContainer Holds the updated information of contact timeline field values
     */
    public void updateLifeCycle(final long orgId, final Collection<Contact> contacts,
                                final DataContainer targetContainer) {
        final Table contactTable = getTable();
        final Column pkColumn = contactTable.getPKColumn();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table contactDropdownTable = getContactDropdownTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Set<Long> contactIds = contacts.stream().map(Contact::id).collect(Collectors.toSet());
        final WhereClause whereClause = new WhereClause(Criteria.in(pkColumn, contactIds));
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;

        selectQuery.addJoinClause(new JoinClause(pkColumn, contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = orgDataStore.get(selectQuery);

        for (final Contact contact : contacts) {
            final Long contactId = contact.id();
            final Option lostReason = contact.getLostReason();
            final Row contactRow = dataContainer.getRow(contactTable, contactId);

            if (Objects.nonNull(contactRow)) {
                contactRow.set(MCOCONTACT.LIFE_CYCLE_STAGE_ID, contact.getLifeCycleStage().id());
                contactRow.set(MCOCONTACT.LIFE_CYCLE_STATUS_ID, contact.getLifeCycleStatus().id());
                contactRow.set(MCOCONTACT.UPDATED_BY, contact.getUpdatedBy().id());
                contactRow.set(MCOCONTACT.MODIFIED_TIME, Instant.now().toEpochMilli());

                if (Objects.nonNull(lostReason)) {
                    loadLostReasonIntoContainer(contactId, lostReason, dataContainer, targetContainer);
                } else {
                    final Optional<FieldInfo> lostReasonField = secureFieldService.getDefaultFieldByAliasName(Source.CONTACT, lostReasonKey);

                    lostReasonField.ifPresent(fieldInfo -> {
                        final Row lostReasonRow = dataContainer.getRow(contactDropdownTable, new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                                .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldInfo.id())));

                        if (Objects.nonNull(lostReasonRow)) {
                            targetContainer.deleteRow(lostReasonRow);
                        }
                    });
                }
                targetContainer.updateRow(contactRow);
            }
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the lost reason into container.
     * </p>
     *
     * @param contactId       Represents the contact id.
     * @param lostReason      Represents the lost reason of the lifecycle.
     * @param dataContainer   Holds the user defined and system field values of existing contact
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadLostReasonIntoContainer(final long contactId, final Option lostReason, final DataContainer dataContainer,
                                             final DataContainer targetContainer) {
        final Table contactDropdownTable = getContactDropdownTable();
        final Row lostReasonRow;

        if (Objects.isNull(dataContainer) || 0 == dataContainer.size(contactDropdownTable)) {
            lostReasonRow = new Row(contactDropdownTable);
        } else {
            final WhereClause whereClause = new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                    .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), lostReason.getFieldId()));
            final Row oldLostReasonRow = dataContainer.getRow(contactDropdownTable, whereClause);

            lostReasonRow = Objects.isNull(oldLostReasonRow) ? new Row(contactDropdownTable) : oldLostReasonRow;
        }

        if (lostReasonRow.isNewRow()) {
            lostReasonRow.set(MCONDROPDOWNFIELDS.ENTITY_ID, contactId);
            lostReasonRow.set(MCONDROPDOWNFIELDS.FIELD_ID, lostReason.getFieldId());
        }
        lostReasonRow.set(MCONDROPDOWNFIELDS.OPTION_ID, lostReason.id());
        targetContainer.updateRow(lostReasonRow);
    }

    /**
     * <p>
     * Loads the complete contact info into the {@link Contact}.
     * </p>
     * <h4>
     * (FYI - example: When mapping a company to a contact, if the company is older, only the entry in the
     * MCONCompanyFields table is available. However, we require an entry in the MCOCompany table).
     * </h4>
     *
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    private void loadCompleteContactInfo(final Long orgId, final Contact contact, final DataContainer targetContainer) {
        final Table contactTable = getTable();
        final Table stageTable = lifeCycleStageManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table contactEmailTable = getContactEmailTable();
        final Collection<Long> contactIds = new HashSet<>();
        final Collection<Long> unavailableStageIds = new HashSet<>();
        final Collection<Long> unavailableCompanyIds = new HashSet<>();
        final Collection<Long> unavailablePhoneIds = new HashSet<>();
        final Collection<Long> unavailableEmailIds = new HashSet<>();

        loadEntityIds(contactIds, unavailableStageIds, unavailableCompanyIds, unavailablePhoneIds, unavailableEmailIds, targetContainer);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final boolean isStageNeeded = !unavailableStageIds.isEmpty();
        final boolean isCompanyNeeded = !unavailableCompanyIds.isEmpty();
        final boolean isPhoneNeeded = !unavailablePhoneIds.isEmpty();
        final boolean isEmailNeeded = !unavailableEmailIds.isEmpty();

        if (isStageNeeded) {
            final SelectQuery stageQuery = SelectQuery.get(stageTable);
            final Column stagePkColumn = stageTable.getPKColumn();

            stageQuery.setWhereClause(new WhereClause(Criteria.in(stagePkColumn, unavailableStageIds)));
            stageQuery.addSelectClause(List.of(stagePkColumn, stageTable.getColumn(OMCOLIFECYCLESTAGE.ORG_ID),
                    stageTable.getColumn(OMCOLIFECYCLESTAGE.NAME)));
            final Table stageDerivedTable = DerivedTable.get(stageTable.getName(), stageTable.getAlias(), stageQuery);
            final Column pkColumn = stageDerivedTable.getPKColumn();

            selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID), pkColumn));
            selectQuery.addSelectClause(List.of(pkColumn, stageDerivedTable.getColumn(OMCOLIFECYCLESTAGE.ORG_ID),
                    stageDerivedTable.getColumn(OMCOLIFECYCLESTAGE.NAME)));
        }

        if (isCompanyNeeded) {
            final SelectQuery companyQuery = SelectQuery.get(companyTable);
            final Column companyPkColumn = companyTable.getPKColumn();

            companyQuery.addSelectClause(List.of(companyPkColumn, companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
            companyQuery.setWhereClause(new WhereClause(Criteria.in(companyPkColumn, unavailableCompanyIds)));
            final Table companyDerivedTable = DerivedTable.get(companyTable.getName(), companyTable.getAlias(), companyQuery);
            final Column pkColumn = companyDerivedTable.getPKColumn();

            selectQuery.addSelectClause(List.of(pkColumn, companyDerivedTable.getColumn(MCOCOMPANY.NAME),
                    companyDerivedTable.getColumn(MCOCOMPANY.ORG_ID)));
            selectQuery.addJoinClause(List.of(
                    new JoinClause(contactTable.getPKColumn(), contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), pkColumn, leftJoin)));
        }

        if (isPhoneNeeded) {
            final SelectQuery phoneQuery = SelectQuery.get(phoneTable);
            final Column phonePkColumn = phoneTable.getPKColumn();

            phoneQuery.addSelectClause(List.of(phonePkColumn, phoneTable.getColumn(OMETPHONE.NUMBER)));
            phoneQuery.setWhereClause(new WhereClause(Criteria.in(phonePkColumn, unavailablePhoneIds)));
            final Table phoneDerivedTable = DerivedTable.get(phoneTable.getName(), phoneTable.getAlias(), phoneQuery);
            final Column pkColumn = phoneDerivedTable.getPKColumn();

            selectQuery.addSelectClause(List.of(pkColumn, phoneDerivedTable.getColumn(OMETPHONE.NUMBER)));
            selectQuery.addJoinClause(List.of(
                    new JoinClause(contactTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), pkColumn, leftJoin)));
        }

        if (isEmailNeeded) {
            final SelectQuery emailQuery = SelectQuery.get(emailTable);
            final Column emailPkColumn = emailTable.getPKColumn();

            emailQuery.addSelectClause(List.of(emailPkColumn, emailTable.getColumn(OMETEMAIL.VALUE)));
            emailQuery.setWhereClause(new WhereClause(Criteria.in(emailPkColumn, unavailableEmailIds)));
            final Table emailDerivedTable = DerivedTable.get(emailTable.getName(), emailTable.getAlias(), emailQuery);
            final Column pkColumn = emailDerivedTable.getPKColumn();

            selectQuery.addSelectClause(List.of(pkColumn, emailDerivedTable.getColumn(OMETEMAIL.VALUE)));
            selectQuery.addJoinClause(List.of(
                    new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), pkColumn, leftJoin)));
        }

        if (!selectQuery.getJoinClause().isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (isStageNeeded) {
                final Row stageRow = dataContainer.getRow(stageTable);

                if (Objects.nonNull(stageRow)) {
                    contact.setLifeCycleStage(lifeCycleStageManager.convertRowToModel(stageRow));
                }
            }

            if (isCompanyNeeded) {
                final Row companyRow = dataContainer.getRow(companyTable);

                if (Objects.nonNull(companyRow)) {
                    contact.setCompany(companyManager.convertRowToPartialModel(companyRow));
                }
            }

            if (isPhoneNeeded) {
                final Collection<Phone> phones = dataContainer.getRows(phoneTable).map(phoneManager::convertRowToModel).toList();

                if (!phones.isEmpty()) {
                    contact.setPhones(phones);
                }
            }

            if (isEmailNeeded) {
                final Collection<Email> emails = dataContainer.getRows(emailTable).map(emailManager::convertRowToModel).toList();

                if (!emails.isEmpty()) {
                    contact.setEmails(emails);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the unavailable entity ids.
     * </p>
     *
     * @param unavailableStageIds   Holds the unavailable stage ids.
     * @param unavailableCompanyIds Holds the unavailable company ids.
     * @param unavailablePhoneIds   Holds the unavailable phone ids.
     * @param unavailableEmailIds   Holds the unavailable email ids.
     * @param targetContainer       Holds the updated information of user defined and system field values.
     */
    private void loadEntityIds(final Collection<Long> contactIds, final Collection<Long> unavailableStageIds,
                               final Collection<Long> unavailableCompanyIds, final Collection<Long> unavailablePhoneIds,
                               final Collection<Long> unavailableEmailIds, final DataContainer targetContainer) {
        final Table stageTable = lifeCycleStageManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table contactEmailTable = getContactEmailTable();
        final String companyKey = ContactFields.COMPANY;
        final String phoneKey = ContactFields.PHONES;
        final String emailKey = ContactFields.EMAILS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey, phoneKey, emailKey);

        targetContainer.getRows(getTable()).forEach(contactRow -> {
            final Long stageId = contactRow.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);
            final WhereClause companyCriteria = new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id()))
                    .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactRow.getPKValue()));
            final WhereClause phoneCriteria = new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fields.get(phoneKey).id()))
                    .and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactRow.getPKValue()));
            final WhereClause emailCriteria = new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fields.get(emailKey).id()))
                    .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactRow.getPKValue()));

            if (Objects.nonNull(stageId)) {
                final Row stageRow = targetContainer.getRow(stageTable, stageId);

                if (Objects.isNull(stageRow)) {
                    unavailableStageIds.add(stageId);
                }
            }
            contactIds.add(contactRow.getPKValue());
            loadEntityIds(MCONCOMPANYFIELDS.COMPANY_ID, contactCompanyTable, companyTable, companyCriteria, targetContainer, unavailableCompanyIds);
            loadEntityIds(MCONPHONEFIELDS.PHONE_ID, contactPhoneTable, phoneTable, phoneCriteria, targetContainer, unavailablePhoneIds);
            loadEntityIds(MCONEMAILFIELDS.EMAIL_ID, contactEmailTable, emailTable, emailCriteria, targetContainer, unavailableEmailIds);
        });
    }

    /**
     * <p>
     * Loads the unavailable entity ids.
     * </p>
     *
     * @param entityIdColumnName   Represents the column name of the entity id(COMPANY_ID, PHONE_ID, EMAIL_ID).
     * @param mappingTable         Represents the mapping table of the entity.
     * @param entityTable          Represents the entity table.
     * @param entityCriteria       Represents the criteria of the entity.
     * @param targetContainer      Holds the updated information of user defined and system field values.
     * @param unavailableEntityIds Holds the unavailable entity ids.
     */
    private void loadEntityIds(final String entityIdColumnName, final Table mappingTable, final Table entityTable,
                               final WhereClause entityCriteria, final DataContainer targetContainer,
                               final Collection<Long> unavailableEntityIds) {
        final Stream<Row> mappingRows = targetContainer.getRows(mappingTable, entityCriteria);

        mappingRows.forEach(mappingRow -> {
            final Long entityId = mappingRow.get(entityIdColumnName);
            final Row entityRow = targetContainer.getRow(entityTable, entityId);

            if (Objects.isNull(entityRow)) {
                unavailableEntityIds.add(entityId);
            }
        });
    }

    /**
     * <p>
     * Loads the contact details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param contact         An entity that holds the contact attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadContactIntoContainer(final boolean isCreateEvent, final Contact contact, final DataContainer targetContainer) {
        final Long orgId = contact.orgId();
        final Long contactId = contact.id();
        final Collection<FieldInfo> fields = getContactFields(orgId);
        final DataContainer entityContainer = isCreateEvent ? null
                : entityLoader.getEntityContainerByIds(orgId, getTable(), fields, List.of(contactId));

        loadContactIntoContainer(isCreateEvent, fields, contact, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the contact details into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param fields          Contact entity that has to be loaded into the container
     * @param contact         An entity that holds the contact attributes
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadContactIntoContainer(final boolean isCreateEvent, final Collection<FieldInfo> fields, final Contact contact,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final CoreMailManager coreMailManagerInstance = CoreMailManager.getInstance();
        final Table contactTable = getTable();
        final Long contactId = contact.id();
        final Long ownerId = contact.getOwnerId();
        final Long orgId = contact.orgId();
        final String sourceName = Contact.ENTITY_TYPE;
        final Row contactRow = isCreateEvent ? new Row(contactTable)
                : Optional.ofNullable(sourceContainer.getRow(contactTable, contactId)).map(Row::new).orElse(null);

        final String emailsKey = ContactFields.EMAILS;
        final String phonesKey = ContactFields.PHONES;
        final Map<String, FieldInfo> contactFields = secureFieldService.groupDefaultFieldsByAliasName(sourceName, emailsKey,
                phonesKey, ContactFields.COMPANY);
        final SourceTagManager sourceTagManager = SourceTagManager.getInstance();
        final Source source = Source.CONTACT;
        final Long currentTime = Instant.now().toEpochMilli();
        final DateTime createdTime = contact.getCreatedTime();
        final Map<String, String> oldContactValues = new HashMap<>();

        if (Objects.nonNull(contactRow)) {

            if (contactRow.isNewRow()) {
                contactRow.setOrgId(contact.orgId());
                contactRow.set(MCOCONTACT.CREATED_TIME, Objects.isNull(createdTime) ? currentTime : createdTime);
                contactRow.set(MCOCONTACT.CREATED_BY, contact.getCreatedBy().id());
                contactRow.set(MCOCONTACT.LAST_ASSIGNED_AT, currentTime);
                contact.setId(contactRow.getPKValue());
                contact.setEntityState(Entity.State.NEW);

                if (Objects.isNull(contact.getLifeCycleStage())) {
                    final LifeCycleStage defaultStage = lifeCycleStageManager.getDefaultStage(orgId);

                    contact.setLifeCycleStage(defaultStage);

                    if (Objects.isNull(contact.getLifeCycleStatus())) {
                        contact.setLifeCycleStatus(lifeCycleStatusManager.getDefaultStatus(orgId, defaultStage.id()));
                    }
                }
            } else {
                contact.setEntityState(Entity.State.EXISTING);

                if (Objects.nonNull(createdTime)) {
                    contactRow.setIfModified(MCOCONTACT.CREATED_TIME, createdTime);
                }
            }

            if (!isCreateEvent) {
                loadFieldAudit(contact, targetContainer, sourceContainer);
            }
            loadContactRowIntoContainer(isCreateEvent, contact, contactRow, oldContactValues, targetContainer, sourceContainer);
            defaultFieldsDataLoader.loadData(new DataLoaderValues<>(isCreateEvent, contactTable, contact, fields,
                    sourceContainer, targetContainer));
            entityLoader.buildEntity(isCreateEvent, contact, fields, contactTable, sourceContainer, targetContainer);

            final Email primaryEmail = contact.getPrimaryEmail();
            final Phone primaryPhone = contact.getPrimaryPhone();
            final SummaryInfo summaryInfo = contact.getSummaryInfo();

            if (Objects.nonNull(primaryEmail) && contactFields.containsKey(emailsKey)) {
                final FieldInfo fieldInfo = contactFields.get(emailsKey);

                contactRow.set(MCOCONTACT.PRIMARY_EMAIL, primaryEmail.id());
                deleteDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id(), primaryEmail.getValue(), targetContainer);
            }

            if (Objects.nonNull(primaryPhone) && contactFields.containsKey(phonesKey)) {
                final FieldInfo fieldInfo = contactFields.get(phonesKey);

                contactRow.set(MCOCONTACT.PRIMARY_PHONE, primaryPhone.id());
                deleteDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id(), primaryPhone.getNumber(), targetContainer);
            }
            final Long contactPkValue = contactRow.getPKValue();
            final Collection<Email> emails = contact.getEmails();

            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                final Set<String> emailValues = emails.stream().map(Email::getValue).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                if (emailSettingsService.getEmailLinkerSettings(orgId, ownerId).isContactLinkingEnabled()) {
                    final Collection<String> emailRecipients = coreMailManagerInstance.fetchEmailRecipientsByAddress(orgId, emailValues);

                    if (!emailRecipients.isEmpty()) {
                        final Table emailTable = emailManager.getTable();
                        final Table contactEmailTable = getContactEmailTable();
                        final Column emailPkColumn = emailTable.getPKColumn();
                        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
                        final Map<String, Long> contactEmails = targetContainer.getRows(contactEmailTable,
                                        new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactPkValue)))
                                .map(row -> {
                                    final Row emailRow = targetContainer.getRow(emailTable,
                                            new WhereClause(Criteria.eq(emailPkColumn, row.get(MCONEMAILFIELDS.EMAIL_ID))));
                                    final String emailValue = emailRow.get(emailValueColumn);

                                    if (!emailRecipients.contains(emailValue)) {
                                        return Map.entry(emailValue, (Long) row.get(MCONEMAILFIELDS.EMAIL_ID));
                                    }

                                    return null;
                                }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        if (!contactEmails.isEmpty()) {
                            coreMailManagerInstance.updateConversationBasedOnContact(orgId, ownerId, contactPkValue, contactEmails, targetContainer);
                        }
                    }
                }
            }
            final Collection<Tag> tags = Objects.nonNull(summaryInfo) ? summaryInfo.getTags() : Collections.emptyList();

            if (!tags.isEmpty()) {
                sourceTagManager.mapTagsWithSource(isCreateEvent, orgId, contactPkValue, tags, source,
                        sourceContainer, targetContainer);
            }
            final Collection<MarketingList> marketingLists = contact.getMarketingLists();

            if (Objects.nonNull(marketingLists) && !marketingLists.isEmpty()) {
                marketingListContactManager.mapContactsWithMarketingList(orgId, contact.getUpdatedBy().id(), isCreateEvent, contactPkValue, marketingLists, sourceContainer, targetContainer);
            }

            if (!isCreateEvent) {
                sourceTagManager.deleteMappedTags(contactId, tags, source, sourceContainer, targetContainer);

                if (!Objects.equals(contactRow.get(MCOCONTACT.OWNER_ID), contact.getSalesOwner().id())) {
                    contactRow.set(MCOCONTACT.LAST_ASSIGNED_AT, currentTime);
                }
                final SourceFollowers sourceFollowers = new SourceFollowers();

                sourceFollowers.setOrgId(orgId);
                sourceFollowers.setSource(Source.CONTACT);
                sourceFollowers.setEntityId(contactId);
                sourceFollowersManager.removeSourceFollower(sourceFollowers, contact.getOwnerId(), targetContainer);
                marketingListContactManager.deleteUnmappedMarketingLists(contactId, Objects.nonNull(marketingLists)
                        ? marketingLists : Collections.emptyList(), sourceContainer, targetContainer);
            }
            loadCompanyIntoContainer(orgId, contact, isCreateEvent, contactRow, contactFields, sourceContainer, targetContainer);

            if (Objects.nonNull(contact.getExternalPersonId())) {
                final Long syncId = contactSyncManager.getSyncId(orgId, ownerId);

                if (Objects.nonNull(syncId)) {
                    contactSyncManager.loadSyncedContactRowIntoContainer(contact, syncId, targetContainer);
                }
            }
            targetContainer.updateRow(contactRow);
        }
    }

    /**
     * <p>
     * Loads the contact field edit history
     * </p>
     *
     * @param contact          An entity that holds the contact attributes
     * @param targetContainer  Holds the updated information of user defined and system field values
     * @param sourceContainer  Holds the existing contact sub-entity data
     */
    private void loadFieldAudit(final Contact contact, final DataContainer targetContainer, final DataContainer sourceContainer) {
        try {
            fieldAuditResolver.loadFieldAudit(contact, sourceContainer.getRow(getTable(), contact.id()), sourceContainer, targetContainer);
        } catch (Exception exception) {
            LOGGER.error("Field edit history : Error while loading history for contact id: {}", contact.id(), exception);
        }
    }

    /**
     * Loads the company into container.
     *
     * @param orgId           Represents the organization id.
     * @param contact         Entity of {@link Contact}.
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param row             Represents the contact row.
     * @param contactFields   Represents the fields of contact.
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param dataContainer   Holds the updated information of user defined and system field values
     */
    public void loadCompanyIntoContainer(final Long orgId, final Contact contact, final boolean isCreateEvent,
                                         final Row row, final Map<String, FieldInfo> contactFields,
                                         final DataContainer sourceContainer, final DataContainer dataContainer) {
        final String companyKey = ContactFields.COMPANY;

        if (contactFields.containsKey(companyKey)) {
            final Long id = contact.id();
            final Long companyFieldId = contactFields.get(companyKey).id();
            Company company = contact.getCompany();

            if (Objects.nonNull(company) && Objects.isNull(company.id())) {
                final Company contactCompany = companyManager.getExistingCompany(orgId, null, company.getName());

                if (Objects.isNull(contactCompany)) {
                    companyManager.loadCompanyIntoContainer(true, company, dataContainer);
                } else {
                    company.setId(contactCompany.id());
                    company = contactCompany;
                }
            }
            loadContactCompanyIntoContainer(id, Objects.isNull(company) ? null : company.id(), isCreateEvent, companyFieldId,
                    sourceContainer, dataContainer);

            if (Objects.nonNull(row) && !row.isNewRow()) {
                deleteUnmappedCompany(id, company, companyFieldId, sourceContainer, dataContainer);
            }
        }
    }

    /**
     * Loads the contact and company mapping into the data container
     *
     * @param contactId       Represents the {@link Contact} id.
     * @param companyId       Represents the {@link Company} id.
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param companyFieldId  Represents the id of the company field.
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param dataContainer   Holds the updated information of user defined and system field values
     */
    private void loadContactCompanyIntoContainer(final Long contactId, final Long companyId, final boolean isCreateEvent,
                                                 final Long companyFieldId, final DataContainer sourceContainer,
                                                 final DataContainer dataContainer) {
        if (Objects.nonNull(contactId) && Objects.nonNull(companyId)) {
            final Table contactCompanyTable = getContactCompanyTable();
            final Row row;

            if (isCreateEvent) {
                row = new Row(contactCompanyTable);
            } else {
                final Row contactCompanyRow = sourceContainer.getRow(contactCompanyTable,
                        new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId))
                                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));
                row = Objects.isNull(contactCompanyRow) ? new Row(contactCompanyTable) : new Row(contactCompanyRow);
            }

            if (row.isNewRow()) {
                row.set(MCONCOMPANYFIELDS.ENTITY_ID, contactId);
                row.set(MCONCOMPANYFIELDS.FIELD_ID, companyFieldId);
            }
            row.set(MCONCOMPANYFIELDS.COMPANY_ID, companyId);

            dataContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Deletes the unmapped company details of the contact in data container
     * </p>
     *
     * @param contactId       Represents the contact id
     * @param company         Entity of {@link Company}
     * @param companyFieldId  Represents the id of the company field.
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedCompany(final Long contactId, final Company company, final Long companyFieldId,
                                       final DataContainer sourceContainer, final DataContainer targetContainer) {
        if (Objects.isNull(company)) {
            final Table contactCompanyTable = getContactCompanyTable();
            final Row contactCompanyRow = sourceContainer.getRow(contactCompanyTable, new WhereClause
                    (Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId)).and(Criteria
                    .eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));

            if (Objects.nonNull(contactCompanyRow)) {
                targetContainer.deleteRow(contactCompanyRow);
            }
        }
    }

    /**
     * <p>
     * Loads the related entities of contact table into the data container
     * </p>
     *
     * @param isCreateEvent     A flag which indicates whether the action is related to create or update event
     * @param contact           An entity that holds the contact attributes
     * @param row               Represents the row of a contact details
     * @param dataContainer     Holds the updated information of user defined and system field values
     * @param existingContainer Holds the existing information of contact related field values
     */
    private void loadContactRowIntoContainer(final boolean isCreateEvent, final Contact contact, final Row row, final Map<String, String> oldContactValues,
                                             final DataContainer dataContainer, final DataContainer existingContainer) {
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final CrmUser salesOwner = contact.getSalesOwner();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final Territory territory = contact.getTerritory();

        row.set(MCOCONTACT.MODIFIED_TIME, Instant.now().toEpochMilli());
        row.set(MCOCONTACT.FIRST_NAME, contact.getFirstName());
        row.set(MCOCONTACT.LAST_NAME, contact.getLastName());
        row.set(MCOCONTACT.DESIGNATION, contact.getDesignation());
        row.set(MCOCONTACT.DEPARTMENT, contact.getDepartment());
        row.set(MCOCONTACT.FACEBOOK, contact.getFacebook());
        row.set(MCOCONTACT.TWITTER, contact.getTwitter());
        row.set(MCOCONTACT.LINKEDIN, contact.getLinkedIn());
        row.set(MCOCONTACT.DATE_OF_BIRTH, contact.getDateOfBirth());
        row.set(MCOCONTACT.LIFE_CYCLE_STAGE_ID, Objects.isNull(lifeCycleStage) ? null : lifeCycleStage.id());
        row.set(MCOCONTACT.LIFE_CYCLE_STATUS_ID, Objects.isNull(lifeCycleStatus) ? null : lifeCycleStatus.id());
        row.set(MCOCONTACT.UPDATED_BY, updatedBy.id());
        row.set(MCOCONTACT.TERRITORY_ID, Objects.isNull(territory) ? null : territory.id());
        row.set(MCOCONTACT.OTHER_UNSUBSCRIBE_REASON, contact.getOtherUnsubscribeReason());

        if (!isCreateEvent) {
            loadOldContactValues(contact, oldContactValues, existingContainer);

            if (contact.hasSalesOwner() && !Objects.equals(row.get(MCOCONTACT.OWNER_ID), contact.getSalesOwner().id())) {
                row.set(MCOCONTACT.LAST_ASSIGNED_AT, Instant.now().toEpochMilli());
            }
        }
        row.set(MCOCONTACT.OWNER_ID, salesOwner.id());
        loadContactTimeline(contact, isCreateEvent, oldContactValues, row, dataContainer);
        dataContainer.updateRow(row);
    }

    /**
     * Updates the contact owner and (optionally) its territory, and timeline.
     *
     * @param orgId     The organization in which the update is applied.
     * @param id        The identifier of the record being updated.
     * @param timeline  The timeline entry representing the assignment update.
     * @param ownerId   The newly assigned owner for the record.
     * @param territory The resolved territory to assign; may be {@code null} if no territory update is required.
     */
    public void updateOwnerAndTerritory(final long orgId, final Long id, final Timeline timeline,
                                        final long ownerId, final Territory territory) {
        final Table table = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        Optional.ofNullable(orgDataStore.get(table, id)).ifPresent(row -> {
            final DataContainer targetContainer = DataContainer.create();
            final Long existingOwnerId = row.get(MCOCONTACT.OWNER_ID);
            final Long existingTerritoryId = row.get(MCOCONTACT.TERRITORY_ID);

            row.set(MCOCONTACT.OWNER_ID, ownerId);

            if (Objects.nonNull(territory)) {
                row.set(MCOCONTACT.TERRITORY_ID, territory.id());
            }
            targetContainer.updateRow(row);

            if (Objects.nonNull(timeline)) {
                loadContactTimelineRowIntoContainer(timeline, id, targetContainer);
                final FieldAuditInfo editContext = timeline.getFieldAuditInfo();

                fieldAuditHandler.loadFieldAuditEntry(orgId, Source.CONTACT, id, null, editContext, ContactFields.SALES_OWNER,
                        Objects.isNull(existingOwnerId) ? null : String.valueOf(existingOwnerId), String.valueOf(ownerId), targetContainer);

                if (Objects.nonNull(territory)) {
                    fieldAuditHandler.loadFieldAuditEntry(orgId, Source.CONTACT, id, null, editContext, ContactFields.TERRITORY,
                            Objects.isNull(existingTerritoryId) ? null : territoryManager.getName(orgId, existingTerritoryId),
                            territory.getName(), targetContainer);
                }
            }
            orgDataStore.commitChanges(targetContainer);
        });
    }

    /**
     * <p>
     * Gets the contact address mapping table
     * </p>
     *
     * @return {@link Table} of custom address mapping
     */
    public Table getContactAddressTable() {
        return Table.get(MCONADDRESSFIELDS.TABLE);
    }

    /**
     * Gets the contact company mapping table
     *
     * @return {@link Table} of contact company mapping
     */
    public Table getContactCompanyTable() {
        return Table.get(MCONCOMPANYFIELDS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation ID
     * @param contacts Collection of {@link Contact} ids
     */
    @Override
    public final void delete(final Long orgId, final Collection<Contact> contacts) {
        if (Objects.nonNull(contacts) && !contacts.isEmpty()) {
            final Map<Integer, Collection<FieldInfo>> groupedFields = fieldService.groupAllFieldsByType(orgId, Contact.ENTITY_TYPE);
            final Table contactTable = getTable();
            final Set<Long> entityIds = new HashSet<>();
            final Set<Long> emailIds = new HashSet<>();
            final Set<Long> phoneIds = new HashSet<>();
            final Set<Long> addressIds = new HashSet<>();
            final Collection<ValueHolder> valueHolders = new ArrayList<>();
            final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
            final AuditSource auditSource = AuditSource.CONTACT;
            final AuditAction auditAction = AuditAction.DELETED;

            for (final Contact contact : contacts) {
                final Address address = contact.getAddress();
                final Collection<Email> emails = contact.getEmails();
                final Collection<Phone> phones = contact.getPhones();

                entityIds.add(contact.id());
                valueHolders.add(contact.getValueHolder());

                if (Objects.nonNull(emails) && !emails.isEmpty()) {
                    emailIds.addAll(emails.stream().map(Email::id).collect(Collectors.toSet()));
                }

                if (Objects.nonNull(phones) && !phones.isEmpty()) {
                    phoneIds.addAll(phones.stream().map(Phone::id).collect(Collectors.toSet()));
                }

                if (Objects.nonNull(address)) {
                    addressIds.add(address.id());
                }
                auditLogDataList.add(new AuditLogData(contact.getUpdatedBy().id(), contact.id(), contact, auditSource, auditAction));
            }

            final Map<Long, String> personIds = contactSyncManager.fetchExternalPersonIds(orgId, entityIds);

            for (final Contact contact : contacts) {
                contact.setExternalPersonId(personIds.get(contact.id()));
            }

            final List<DeleteQuery> deleteQueries = entityHandler.buildEntityDeleteQueries(contactTable, groupedFields,
                    new WhereClause(Criteria.in(contactTable.getPKColumn(), entityIds)), valueHolders);

            if (!emailIds.isEmpty()) {
                final Table emailTable = emailManager.getTable();

                deleteQueries.add(entityHandler.buildEntityDeleteQuery(emailTable, new WhereClause(Criteria
                        .in(emailTable.getPKColumn(), emailIds))));
            }

            if (!addressIds.isEmpty()) {
                final Table addressTable = addressManager.getTable();

                deleteQueries.add(entityHandler.buildEntityDeleteQuery(addressTable, new WhereClause(Criteria
                        .in(addressTable.getPKColumn(), addressIds))));
            }
            orgDataStore(orgId).delete(deleteQueries);

            if (!auditLogDataList.isEmpty()) {
                auditLogger.log(orgId, auditLogDataList);
            }
        }
    }

    /**
     * Converts the row into {@link Contact}
     *
     * @param orgId         Represents an organization id
     * @param row           Holds the attribute values of {@link Contact}
     * @param isPartial     A flag that represents the row should be converted as partial or not
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Contact} which contains contact related data.
     */
    public Contact convertRowToModel(final Long orgId, final Row row, final boolean isPartial, final DataContainer dataContainer) {
        final Contact contact = new Contact();
        final Long contactId = row.getPKValue();
        final Table contactTable = getTable();
        final Table contactDropdownTable = getContactDropdownTable();
        final String contactSourceKey = ContactFields.CONTACT_SOURCE;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
        final String timezoneKey = ContactFields.TIMEZONE;
        final String unsubscribeReasonKey = ContactFields.UNSUBSCRIBE_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, lostReasonKey,
                subscriptionTypesKey, subscriptionStatusKey, contactSourceKey, ContactFields.PHONES, timezoneKey,
                ContactFields.EMAILS, ContactFields.COMPANY, ContactFields.ADDRESS, unsubscribeReasonKey);

        contact.setId(contactId);
        contact.setOrgId(orgId);
        contact.setFirstName(row.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(row.get(MCOCONTACT.LAST_NAME));
        contact.setDealCount(getDealCountByState(orgId, contactId, Deal.State.OPEN.getId()));
        contact.setOwnerId(row.get(MCOCONTACT.OWNER_ID));

        if (!isPartial) {
            final Row lifeCycleStageRow = dataContainer.getRow(lifeCycleStageManager.getTable(),
                    (Long) row.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID));
            final Row lifeCycleStatusRow = dataContainer.getRow(lifeCycleStatusManager.getTable(),
                    (Long) row.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID));
            final Map<Long, BiConsumer<Contact, Option>> fieldActions = new HashMap<>();

            final Long contactSourceFieldId = fields.get(contactSourceKey).id();
            final Long subscriptionTypeFieldId = fields.get(subscriptionTypesKey).id();
            final Long subscriptionStatusFieldId = fields.get(subscriptionStatusKey).id();
            final Long unsubscribeReasonFieldId = fields.get(unsubscribeReasonKey).id();
            final Long lostReasonFieldId = fields.get(lostReasonKey).id();
            final Long timeZoneFieldId = fields.get(timezoneKey).id();

            fieldActions.put(contactSourceFieldId, Contact::setContactSource);
            fieldActions.put(subscriptionTypeFieldId, Contact::addSubscriptionType);
            fieldActions.put(subscriptionStatusFieldId, Contact::setSubscriptionStatus);
            fieldActions.put(timeZoneFieldId, Contact::setTimeZone);
            fieldActions.put(lostReasonFieldId, Contact::setLostReason);
            fieldActions.put(unsubscribeReasonFieldId, Contact::setUnsubscribeReason);

            dataContainer.getRows(contactDropdownTable,
                            new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId)))
                    .forEach(optionRow -> {
                        final Long fieldId = optionRow.get(MCONDROPDOWNFIELDS.FIELD_ID);
                        final Option option = optionService.get(orgId, fieldId, optionRow.get(MCONDROPDOWNFIELDS.OPTION_ID));
                        final BiConsumer<Contact, Option> setContactOption = fieldActions.get(fieldId);

                        if (Objects.nonNull(setContactOption)) {
                            setContactOption.accept(contact, option);
                        }
                    });

            if (Objects.nonNull(lifeCycleStageRow)) {
                contact.setLifeCycleStage(lifeCycleStageManager.convertRowToModel(lifeCycleStageRow));
            }

            if (Objects.nonNull(lifeCycleStatusRow)) {
                contact.setLifeCycleStatus(lifeCycleStatusManager.convertRowToModel(lifeCycleStatusRow));
            }
            contact.setModifiedTime(row.get(MCOCONTACT.MODIFIED_TIME));
            contact.setCreatedTime(row.get(MCOCONTACT.CREATED_TIME));
            contact.setLastAssignedAt(row.get(MCOCONTACT.LAST_ASSIGNED_AT));

            entityLoader.setEntityValues(contact, contactTable, dataContainer);
        }
        contact.setDesignation(row.get(MCOCONTACT.DESIGNATION));
        contact.setDepartment(row.get(MCOCONTACT.DEPARTMENT));
        contact.setFacebook(row.get(MCOCONTACT.FACEBOOK));
        contact.setTwitter(row.get(MCOCONTACT.TWITTER));
        contact.setLinkedIn(row.get(MCOCONTACT.LINKEDIN));
        contact.setDateOfBirth(row.get(MCOCONTACT.DATE_OF_BIRTH));
        contact.setOtherUnsubscribeReason(row.get(MCOCONTACT.OTHER_UNSUBSCRIBE_REASON));
        contact.setCompany(buildCompany(contactId, fields.get(ContactFields.COMPANY).id(), dataContainer));
        contact.setAddress(buildAddress(contactId, fields.get(ContactFields.ADDRESS).id(), dataContainer));
        contact.setRecentNote(row.get(MCOCONTACT.RECENT_NOTE));
        buildPhones(contactId, contact, row, fields.get(ContactFields.PHONES).id(), dataContainer);
        buildEmails(contactId, contact, row, fields.get(ContactFields.EMAILS).id(), dataContainer);

        final Long territoryId = row.get(MCOCONTACT.TERRITORY_ID);
        final Long createdById = row.get(MCOCONTACT.CREATED_BY);
        final Long updatedById = row.get(MCOCONTACT.UPDATED_BY);
        final Long salesOwnerId = row.get(MCOCONTACT.OWNER_ID);
        final Collection<Long> userIds = new HashSet<>();
        final ValueHolder valueHolder = contact.getValueHolder();

        if (Objects.nonNull(valueHolder)) {
            userIds.addAll(valueHolder.getUserValues().stream().map(CrmUser::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(createdById)) {
            contact.setCreatedBy(userService.getUser(orgId, createdById));
            userIds.add(createdById);
        }

        if (Objects.nonNull(updatedById)) {
            contact.setUpdatedBy(userService.getUser(orgId, updatedById));
            userIds.add(updatedById);
        }

        if (Objects.nonNull(salesOwnerId)) {
            contact.setOwnerId(salesOwnerId);
            contact.setSalesOwner(userService.getUser(orgId, salesOwnerId));
            userIds.add(salesOwnerId);
        }
        contact.setUserIds(userIds);

        if (Objects.nonNull(territoryId)) {
            contact.setTerritory(buildPartialTerritory(territoryId, dataContainer));
        }
        final Collection<Tag> tags = SourceTagManager.getInstance().buildTags(contactId, Source.CONTACT, dataContainer);

        if (Objects.nonNull(tags) && !tags.isEmpty()) {
            final SummaryInfo summaryInfo = new SummaryInfo();

            summaryInfo.setTags(tags);
            contact.setSummaryInfo(summaryInfo);
        }
        final Collection<MarketingList> marketingLists = marketingListContactManager.buildMarketingLists(orgId, contactId, dataContainer);

        if (!marketingLists.isEmpty()) {
            contact.setMarketingLists(marketingLists);
        }
        loadSummaryDetails(orgId,contact);
        return contact;
    }

    /**
     * Converts the row into {@link Email}
     *
     * @param contactId     Represents the id of the {@link Contact}
     * @param dataContainer A container that holds the user defined field values and system values
     */
    public void buildEmails(final Long contactId, final Contact contact, final Row row,
                            final Long fieldId, final DataContainer dataContainer) {
        final Table contactEmailTable = getContactEmailTable();
        final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId))
                        .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId)));
        final Table emailTable = emailManager.getTable();
        final Long primaryEmailId = row.get(MCOCONTACT.PRIMARY_EMAIL);
        final Stream<Row> emailRows = dataContainer.getRows(emailTable,
                new WhereClause(Criteria.in(emailTable.getPKColumn(), contactEmailRows.map(contactEmailRow ->
                        contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID)).collect(Collectors.toSet()))));

        emailRows.forEach(emailRow -> {
            final Email email = emailManager.convertRowToModel(emailRow);

            if (Objects.nonNull(primaryEmailId) && Objects.equals(primaryEmailId, email.id())) {
                contact.setPrimaryEmail(email);
            }
            contact.addEmail(email);
        });
    }

    /**
     * Converts the row into {@link Phone}
     *
     * @param contactId     Represents the id of the {@link Contact}
     * @param dataContainer A container that holds the user defined field values and system values
     */
    public void buildPhones(final Long contactId, final Contact contact, final Row row,
                            final Long fieldId, final DataContainer dataContainer) {
        final Table contactPhoneTable = getContactPhoneTable();
        final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId))
                        .and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId)));
        final Table phoneTable = phoneManager.getTable();
        final Long primaryPhoneId = row.get(MCOCONTACT.PRIMARY_PHONE);
        final Stream<Row> phoneRows = dataContainer.getRows(phoneTable,
                new WhereClause(Criteria.in(phoneTable.getPKColumn(), contactPhoneRows.map(contactPhoneRow ->
                        contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID)).collect(Collectors.toSet()))));

        phoneRows.forEach(phoneRow -> {
            final Phone phone = phoneManager.convertRowToModel(phoneRow);

            if (Objects.nonNull(primaryPhoneId) && Objects.equals(primaryPhoneId, phone.id())) {
                contact.setPrimaryPhone(phone);
            }
            contact.addPhone(phone);
        });
    }

    /**
     * Converts the row into {@link Company}
     *
     * @param contactId     Represents the id of the {@link Contact}
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Company}
     */
    private Company buildCompany(final Long contactId, final Long fieldId, final DataContainer dataContainer) {
        final Table contactCompanyTable = getContactCompanyTable();
        final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria
                .eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId)).
                and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), fieldId)));

        if (Objects.nonNull(contactCompanyRow)) {
            final Table companyTable = companyManager.getTable();
            final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                    contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID))));

            if (Objects.nonNull(companyRow)) {
                return companyManager.convertRowToPartialModel(companyRow);
            }
        }

        return null;
    }

    /**
     * Converts the row into {@link Address}
     *
     * @param contactId     Represents the id of the {@link Contact}
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Address}
     */
    public Address buildAddress(final Long contactId, final Long fieldId, final DataContainer dataContainer) {
        final Table contactAddressTable = getContactAddressTable();
        final Row contactAddressRow = dataContainer.getRow(contactAddressTable,
                new WhereClause(Criteria.eq(contactAddressTable.getColumn(MCONADDRESSFIELDS.ENTITY_ID), contactId))
                        .and(Criteria.eq(contactAddressTable.getColumn(MCONADDRESSFIELDS.FIELD_ID), fieldId)));

        if (Objects.nonNull(contactAddressRow)) {
            final Table addressTable = addressManager.getTable();
            final Row addressRow = dataContainer.getRow(addressTable, new WhereClause(Criteria.eq(addressTable.getPKColumn(),
                    contactAddressRow.get(contactAddressTable.getColumn(MCONADDRESSFIELDS.ADDRESS_ID)))));

            if (Objects.nonNull(addressRow)) {
                return addressManager.convertRowToModel(addressRow);
            }
        }

        return null;
    }

    /**
     * Gets the total open deal count by contact
     *
     * @param orgId     Represents the organisation id
     * @param contactId Represents the contact id
     * @param state     Represents the state
     * @return {@link Long} of deal count of contact
     */
    public Long getDealCountByState(final Long orgId, final Long contactId, final int state) {
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.CONTACT_ID), contactId))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), state)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the email table definitions
     * </p>
     *
     * @return Contact email table
     */
    public Table getContactEmailTable() {
        return Table.get(MCONEMAILFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the contact phone table definition
     * </p>
     *
     * @return Contact phone table
     */
    public Table getContactPhoneTable() {
        return Table.get(MCONPHONEFIELDS.TABLE);
    }

    /**
     * Fetches the {@link Contact} for the dropdown actions.
     *
     * @param orgId    Represents the organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of contacts.
     */
    public Collection<Contact> getContactDropdownList(final Long orgId, final DropdownLimit limit, final boolean includeEmails,
                                                      final DropdownCriteria criteria) {
        return getContactDropdownList(orgId, limit, includeEmails, false, criteria);
    }

    /**
     * Fetches the {@link Contact} for the dropdown actions.
     *
     * @param orgId         Represents the organization id.
     * @param limit         Which holds the limit configuration.
     * @param includeEmails Whether to include emails in the dropdown.
     * @param includePhones Whether to include phone in the search criteria.
     * @param criteria      Which configure the criteria.
     * @return Collection of contacts.
     */
    public Collection<Contact> getContactDropdownList(final Long orgId, final DropdownLimit limit, final boolean includeEmails,
                                                      final boolean includePhones, final DropdownCriteria criteria) {
        return getContactDropdownList(orgId, limit, includeEmails, includePhones, false, criteria);
    }

    /**
     * Fetches the {@link Contact} for the dropdown actions.
     *
     * @param orgId                 Represents the organization id.
     * @param limit                 Which holds the limit configuration.
     * @param includeEmails         Whether to include emails in the dropdown.
     * @param includePhones         Whether to include phone in the search criteria.
     * @param includeCompanyAddress Whether to include the contact company address in the result.
     * @param criteria              Which configure the criteria.
     * @return Collection of contacts.
     */
    public Collection<Contact> getContactDropdownList(final Long orgId, final DropdownLimit limit, final boolean includeEmails,
                                                      final boolean includePhones, final boolean includeCompanyAddress,
                                                      final DropdownCriteria criteria) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);
        WhereClause whereClause = new WhereClause(criteria.build(firstNameColumn)).or(criteria.build(lastNameColumn))
                .or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim())));
        final OrderByClause contactPkOrderByClause = new OrderByClause(contactPkColumn);

        if (includeEmails) {
            final Table contactEmailTable = getContactEmailTable();
            final Column emailColumn = emailTable.getColumn(OMETEMAIL.VALUE);

            contactSelectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
            contactSelectQuery.addSelectClause(List.of(emailColumn, contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL)));
            contactSelectQuery.addSelectClause(contactEmailTable);
            whereClause = whereClause.or(new WhereClause(criteria.build(emailColumn)));
            contactSelectQuery.addOrderByClause(List.of(contactPkOrderByClause, new OrderByClause(contactEmailTable.getPKColumn())));

            if (includeCompanyAddress) {
                addContactCompanyJoinsAndSelectClause(contactSelectQuery, contactPkColumn, contactCompanyTable, companyTable, true);
            }
        } else {
            contactSelectQuery.addJoinClause(List.of(
                    new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), JoinClause.JoinType.LEFT),
                    new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), JoinClause.JoinType.LEFT)));
            contactSelectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.VALUE), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL)));
            contactSelectQuery.addSelectClause(List.of(companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.NAME)));
            contactSelectQuery.addSelectClause(contactCompanyTable);

            if (includeCompanyAddress) {
                addCompanyAddressJoins(contactSelectQuery, contactCompanyTable);
            }
            contactSelectQuery.addOrderByClause(contactPkOrderByClause);
        }

        if (includePhones || !includeEmails) {
            final Table phoneTable = phoneManager.getTable();
            final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);

            contactSelectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
            contactSelectQuery.addSelectClause(List.of(phoneNumberColumn, contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE)));
            contactSelectQuery.addSelectClause(phoneTable);

            if (includePhones) {
                whereClause = whereClause.or(new WhereClause(criteria.build(phoneNumberColumn)));
            }
        }

        contactSelectQuery.setWhereClause(whereClause);
        contactSelectQuery.addSelectClause(List.of(firstNameColumn, lastNameColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID)));
        contactSelectQuery.setLimitClause(limit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(contactSelectQuery);
        final List<Contact> contactList = new ArrayList<>();
        final Table phoneTable = phoneManager.getTable();

        if (includeEmails) {
            final Stream<Row> contactEmailRows = dataContainer.getRows(getContactEmailTable());

            contactEmailRows.forEach(contactEmailRow -> {
                final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPkColumn, contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID))));
                final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(), contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID))));
                final Contact contact = convertRowToContact(orgId, contactRow);
                final Email email = new Email();
                final Long emailPkValue = emailRow.getPKValue();

                email.setId(emailPkValue);
                email.setValue(emailRow.get(OMETEMAIL.VALUE));

                if (Objects.equals(contactRow.get(MCOCONTACT.PRIMARY_EMAIL), emailPkValue)) {
                    contact.setPrimaryEmail(email);
                }
                contact.addEmail(email);

                if (includePhones) {
                    addPrimaryPhoneToContact(contact, contactRow, dataContainer, phoneTable);
                }

                if (includeCompanyAddress) {
                    addCompanyToContact(contact, contactRow, dataContainer, companyTable, contactCompanyTable, true);
                }
                contactList.add(contact);
            });

            return contactList;
        } else {
            final Stream<Row> rows = dataContainer.getRows(contactTable);

            rows.forEach(row -> {
                final Contact contact = convertRowToContact(orgId, row);

                addPrimaryEmailToContact(contact, row, dataContainer, emailTable);
                addPrimaryPhoneToContact(contact, row, dataContainer, phoneTable);
                addCompanyToContact(contact, row, dataContainer, companyTable, contactCompanyTable, includeCompanyAddress);
                contactList.add(contact);
            });

            return contactList;
        }
    }

    /**
     * <p>
     *   Fetches the primary email details from the given data container
     * </p>
     *
     * @param contact       The contact entity to update
     * @param contactRow    The contact row containing the primary email reference
     * @param dataContainer The container holding related email data
     * @param emailTable    The email table used to fetch email details
     */
    private void addPrimaryEmailToContact(final Contact contact, final Row contactRow, final DataContainer dataContainer, final Table emailTable) {
        final Long primaryEmailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);

        if (Objects.nonNull(primaryEmailId)) {
            final Row emailRow = dataContainer.getRow(emailTable, primaryEmailId);

            if (Objects.nonNull(emailRow)) {
                final Email email = new Email();

                email.setId(primaryEmailId);
                email.setValue(emailRow.get(OMETEMAIL.VALUE));
                contact.setPrimaryEmail(email);
                contact.addEmail(email);
            }
        }
    }

    /**
     * Adds the associated company to the given contact.
     *
     * @param contact               The contact to update
     * @param contactRow            The contact row
     * @param dataContainer         The joined data container
     * @param companyTable          The company table
     * @param contactCompanyTable   The contact-company mapping table
     * @param includeCompanyAddress {@code true} to also resolve and set the company's address;
     *                              {@code false} to set only the company's basic details
     */
    private void addCompanyToContact(final Contact contact, final Row contactRow, final DataContainer dataContainer,
                                     final Table companyTable, final Table contactCompanyTable,
                                     final boolean includeCompanyAddress) {
        final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria.eq(
                contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactRow.getPKValue())));

        if (Objects.isNull(contactCompanyRow)) {
            return;
        }
        final Long companyId = contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID);

        if (Objects.isNull(companyId)) {
            return;
        }

        final Row companyRow = dataContainer.getRow(companyTable, companyId);

        if (Objects.nonNull(companyRow)) {
            final Company company = companyManager.convertRowToPartialModel(companyRow);

            if (includeCompanyAddress) {
                final Table companyAddressTable = companyManager.getCompanyAddressTable();
                final Table addressTable = addressManager.getTable();
                final Column addressPkColumn = addressTable.getPKColumn();
                final Row addressCompanyRow = dataContainer.getRow(companyAddressTable,
                        new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), companyId)));

                if (Objects.nonNull(addressCompanyRow)) {
                    final Row addressRow = dataContainer.getRow(addressTable,
                            new WhereClause(Criteria.eq(addressPkColumn, addressCompanyRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

                    if (Objects.nonNull(addressRow)) {
                        company.setAddress(addressManager.convertRowToModel(addressRow));
                    }
                }
            }
            contact.setCompany(company);
        }
    }

    /**
     *<p>
     * Adds the joins and select clauses needed to fetch a contact's company (and optionally the
     * company's address) in the given query.
     * </p>
     *
     * @param selectQuery           The query to add the joins and select clauses to
     * @param contactPkColumn       The contact table's primary key column, used to join to the mapping table
     * @param contactCompanyTable   The contact-company mapping table
     * @param companyTable          The company table
     * @param includeCompanyAddress {@code true} to also join and select the company's address
     */
    private void addContactCompanyJoinsAndSelectClause(final SelectQuery selectQuery, final Column contactPkColumn,
                                                       final Table contactCompanyTable, final Table companyTable,
                                                       final boolean includeCompanyAddress) {
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.NAME)));
        selectQuery.addSelectClause(contactCompanyTable);

        if (includeCompanyAddress) {
            addCompanyAddressJoins(selectQuery, contactCompanyTable);
        }
    }

    /**
     * <p>
     * Joins the company's address using the contact-company link ({@code COMPANY_ID}) directly,
     * without an extra hop via the company table.
     * </p>
     *
     * @param selectQuery         The query to add the address joins and select clauses to
     * @param contactCompanyTable The contact-company mapping table providing the {@code COMPANY_ID} to join on
     */
    private void addCompanyAddressJoins(final SelectQuery selectQuery, final Table contactCompanyTable) {
        final Table companyAddressTable = companyManager.getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final Column addressPkColumn = addressTable.getPKColumn();
        final Column companyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);

        selectQuery.addSelectClause(addressTable, companyAddressTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(companyIdColumn, companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressPkColumn, JoinClause.JoinType.LEFT)));
    }

    /**
     * <p>
     *   Fetches the primary phone details from the given data container
     * </p>
     *
     * @param contact        The contact entity to update
     * @param contactRow     The contact row containing the primary phone reference
     * @param dataContainer  The container holding related phone data
     * @param phoneTable     The phone table used to fetch phone details
     */
    private void addPrimaryPhoneToContact(final Contact contact, final Row contactRow, final DataContainer dataContainer, final Table phoneTable) {
        final Long primaryPhoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

        if (Objects.nonNull(primaryPhoneId)) {
            final Row phoneRow = dataContainer.getRow(phoneTable, primaryPhoneId);

            if (Objects.nonNull(phoneRow)) {
                final Phone phone = new Phone();

                phone.setId(primaryPhoneId);
                phone.setNumber(phoneRow.get(OMETPHONE.NUMBER));
                contact.setPrimaryPhone(phone);
                contact.addPhone(phone);
            }
        }
    }

    /**
     * <p>
     * Retrieves the corresponding email address for collection of contact email ids
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param contactEmailIds Represents the contact Email Ids
     * @return Email id from contact email table
     */
    public Map<Long, String> getContactEmailIds(final Long orgId, final Collection<Long> contactEmailIds) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(emailTable);
        selectQuery.addJoinClause(getJoinClauseForContactEmail());
        selectQuery.setWhereClause(new WhereClause(Criteria.in(getContactEmailTable().getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailIds)));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(emailTable);

        return rows.collect(Collectors.toMap(Row::getPKValue, row -> row.get(OMETEMAIL.VALUE)));
    }

    /**
     * <p>
     * Gets contact ids for given contact email ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactEmailIds Represents the contact email Ids
     * @return Collection of contact ids
     */
    public Collection<Long> getContactIds(final Long orgId, final Collection<Long> contactEmailIds) {
        if (!contactEmailIds.isEmpty()) {
            final Table contactTable = getTable();
            final SelectQuery selectQuery = SelectQuery.get(contactTable);

            selectQuery.addJoinClause(getJoinClauseForContactEmail());
            selectQuery.addSelectClause(contactTable.getPKColumn());
            selectQuery.setWhereClause(new WhereClause(Criteria.in(getContactEmailTable().getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailIds)));

            final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(contactTable);

            return rows.filter(Objects::nonNull).map(Row::getPKValue).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * <p>
     * Gets contact related company ids for given contact email id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param contactIds Represents the contact email Ids
     * @return Collection of company ids
     */
    public Collection<Long> getContactRelatedCompanyIds(final Long orgId, final Collection<Long> contactIds) {
        final Table contactCompanyTable = getContactCompanyTable();
        final Stream<Row> rows = orgDataStore(orgId).get(contactCompanyTable, new WhereClause(Criteria.in
                        (contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactIds)),
                List.of(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID))).getRows(contactCompanyTable);

        return rows.filter(Objects::nonNull).map(row -> (Long) row.get(MCONCOMPANYFIELDS.COMPANY_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Checks whether the given contact emails present
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactEmailIds Represents contact email ids
     * @return true, if contact email id exists
     */
    public boolean isContactEmailIdsExists(final Long orgId, final Collection<Long> contactEmailIds) {
        if (!contactEmailIds.isEmpty()) {
            final Table contactEmailTable = getContactEmailTable();
            final CountQuery countQuery = CountQuery.get(contactEmailTable);

            countQuery.setWhereClause(new WhereClause(Criteria.in(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailIds)));

            return contactEmailIds.size() == orgDataStore(orgId).get(countQuery);
        }

        return false;
    }

    /**
     * <p>
     * Gets the join clause for doing DB function related to contact email
     * </p>
     *
     * @return Collection of {@link JoinClause}
     */
    private List<JoinClause> getJoinClauseForContactEmail() {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();

        return List.of(new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param ids   Represents the collection of contact ID
     * @return {@link Contact} which contains the partial data
     */
    @Override
    public Collection<Contact> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table contactTable = getTable();
        final SelectQuery query = SelectQuery.get(contactTable);

        loadSelectAndJoinClauses(query);
        query.addSelectClause(contactTable);
        query.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(query);
        final Stream<Row> rows = dataContainer.getRows(contactTable);

        return Objects.nonNull(rows) ? rows.map(row -> convertRowToModel(orgId, row, true, dataContainer)).toList()
                : Collections.emptyList();
    }

    /**
     * <p>
     * Fetches the collection contact IDs and the respective email values in a collection of {@link Map} representation
     * where column names are used as keys and their corresponding values are stored as values
     * </p>
     *
     * @param orgId              Represents the organisation ID
     * @param emails             Which contains the collection of emails
     * @param excludedContactIds Which contains the collection contact IDs that are excluded
     * @return the collection of {@link Map} that contains the emails and contact IDs
     */
    public Map<String, Long> fetchContactIdsByEmails(final Long orgId, final Collection<String> emails, final Collection<Long> excludedContactIds) {
        final String emailsAliasName = ContactFields.EMAILS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailsAliasName).get(emailsAliasName);

        final Table emailTable = emailManager.getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Column contactIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Column emailColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final SelectQuery selectQuery = SelectQuery.get(contactEmailTable);
        final SelectQuery minimumEntityIdsQuery = SelectQuery.get(contactEmailTable);
        final Criteria fieldCriteria = Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldInfo.id());
        final Criteria emailCriteria = Criteria.in(emailColumn, emails);

        minimumEntityIdsQuery.addSelectClause(contactIdColumn.min());
        minimumEntityIdsQuery.addJoinClause(new JoinClause(emailIdColumn, emailTable.getPKColumn()));
        minimumEntityIdsQuery.addGroupByClause(new GroupByClause(emailColumn));
        minimumEntityIdsQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMETEMAIL.ORG_ID), orgId))
                .and(fieldCriteria).and(emailCriteria));
        final WhereClause whereClause = new WhereClause(emailCriteria).and(Criteria.in(contactIdColumn, minimumEntityIdsQuery))
                .and(fieldCriteria);

        selectQuery.addSelectClause(List.of(contactIdColumn, emailColumn.distinct()));
        selectQuery.addJoinClause(new JoinClause(emailIdColumn, emailTable.getPKColumn()));

        if (excludedContactIds.isEmpty()) {
            selectQuery.setWhereClause(whereClause);
        } else {
            selectQuery.setWhereClause(whereClause.and(Criteria.notIn(contactIdColumn, excludedContactIds)));
        }
        return groupContactIdsByValues(orgDataStore(orgId).getData(selectQuery), OMETEMAIL.VALUE);
    }

    /**
     * <p>
     * Fetches the collection contact IDs and the respective phone number values in a collection of {@link Map} representation
     * where column names are used as keys and their corresponding values are stored as values
     * </p>
     *
     * @param orgId              Represents the organisation ID
     * @param phoneNumbers       Which contains the collection of phone numbers
     * @param excludedContactIds Which contains the collection contact IDs that are excluded
     * @return the collection of {@link Map} that contains the phone numbers and contact IDs
     */
    public Map<String, Long> fetchContactIdsByPhoneNumbers(final Long orgId, final Collection<String> phoneNumbers, final Collection<Long> excludedContactIds) {
        final String phonesAliasName = ContactFields.PHONES;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, phonesAliasName).get(phonesAliasName);

        final Table phoneNumberTable = phoneManager.getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Column contactIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Column phoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final Column phoneNumberColumn = phoneNumberTable.getColumn(OMETPHONE.NUMBER);
        final SelectQuery selectQuery = SelectQuery.get(contactPhoneTable);
        final SelectQuery minimumEntityIdsQuery = SelectQuery.get(contactPhoneTable);
        final Criteria fieldCriteria = Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldInfo.id());
        final Criteria phoneNumberCriteria = Criteria.in(phoneNumberColumn, phoneNumbers);

        minimumEntityIdsQuery.addSelectClause(contactIdColumn.min());
        minimumEntityIdsQuery.addJoinClause(new JoinClause(phoneIdColumn, phoneNumberTable.getPKColumn()));
        minimumEntityIdsQuery.addGroupByClause(new GroupByClause(phoneNumberColumn));
        minimumEntityIdsQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMETPHONE.ORG_ID), orgId))
                .and(phoneNumberCriteria).and(fieldCriteria));
        final WhereClause whereClause = new WhereClause(phoneNumberCriteria).and(Criteria.in(contactIdColumn, minimumEntityIdsQuery))
                .and(fieldCriteria);

        selectQuery.addSelectClause(List.of(contactIdColumn, phoneNumberColumn.distinct()));
        selectQuery.addJoinClause(new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID),
                phoneNumberTable.getPKColumn()));

        if (excludedContactIds.isEmpty()) {
            selectQuery.setWhereClause(whereClause);
        } else {
            selectQuery.setWhereClause(whereClause.and(Criteria.notIn(contactIdColumn, excludedContactIds)));
        }
        return groupContactIdsByValues(orgDataStore(orgId).getData(selectQuery), OMETPHONE.NUMBER);
    }

    /**
     * Groups the contact IDs that have been mapped based on the values in the specified column from the contactValueRows
     *
     * @param contactValueRows Which contains the collection of rows in the form of Map representations, where the keys are column names and the values are corresponding values
     * @param columnName       Represents the column name by which the contact IDs are to be grouped
     * @return A {@link Map} that holds the contact IDs, organized according to the values in the specified column
     */
    private Map<String, Long> groupContactIdsByValues(final List<Map<String, Object>> contactValueRows, final String columnName) {
        final Map<String, Long> groupedContactIdsByValues = new HashMap<>();

        for (final Map<String, Object> contactValueRow : contactValueRows) {
            groupedContactIdsByValues.put((String) contactValueRow.get(columnName), (Long) contactValueRow.get(OMETENTITY.ENTITY_ID));
        }
        return groupedContactIdsByValues;
    }

    /**
     * <p>
     * Loads the timeline for the specified contact. If it's a create event, it retrieves the contact creation timeline.
     * In the case of an update event, it compares the old values and new values from the request
     * and appropriately stores the container in the timeline container.
     * </p>
     *
     * @param contact         Represents the {@link Contact}
     * @param isCreateEvent   A flag which represents the event is created or not
     * @param oldValues       Represents the old values of existing timeline field values
     * @param contactRow      Represents the contact row
     * @param targetContainer Holds the updated contact field information into the container
     */
    private void loadContactTimeline(final Contact contact, final boolean isCreateEvent, final Map<String, String> oldValues,
                                     final Row contactRow, final DataContainer targetContainer) {
        final Long orgId = contact.orgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final String firstNameKey = ContactFields.FIRST_NAME;

        if (isCreateEvent) {
            final Option contactSource = contact.getContactSource();

            if (Objects.nonNull(contactSource)) {
                final String contactSourceKey = ContactFields.CONTACT_SOURCE;
                final String webFormKey = ContactSourceStaticOption.WEB_FORM.getName();
                final FieldInfo contactSourceFieldInfo = fields.get(contactSourceKey);

                if (contactSource.id().equals(optionService.getOption(orgId, contactSourceFieldInfo, webFormKey).id())) {
                    loadEventCreationTimeline(contactRow, contact, TimelineType.CONTACT_FORM_SUBMISSION, contactSourceFieldInfo.i18nName(), targetContainer);
                }
            }

            loadEventCreationTimeline(contactRow, contact, contact.isCreatedViaCall() ? TimelineType.CONTACT_CREATE_UPON_CALL : TimelineType.CONTACT_CREATED,
                    fields.get(firstNameKey).i18nName(), targetContainer);

        } else {
            final String lifecycleStageKey = ContactFields.LIFECYCLE_STAGE;
            final String lifecycleStatusKey = ContactFields.LIFECYCLE_STATUS;
            final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
            final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
            final String createdAtKey = ContactFields.CREATED_AT;
            final String oldLifecycleStage = oldValues.get(lifecycleStageKey);
            final String oldLifecycleStatus = oldValues.get(lifecycleStatusKey);
            final String oldLostReason = oldValues.get(lostReasonKey);
            final String oldSubscriptionStatus = oldValues.get(subscriptionStatusKey);
            final String oldName = oldValues.get(firstNameKey);
            final String newName = contact.getFullName();
            final String salesOwnerKey = ContactFields.SALES_OWNER;
            final String oldSalesOwner = oldValues.get(salesOwnerKey);
            final String oldCreatedAtTime = oldValues.get(createdAtKey);
            final DateTime createdTime = contact.getCreatedTime();
            final String newCreatedTime = Objects.isNull(createdTime) ? null : createdTime.getTimeStamp().toString();

            if (Objects.nonNull(oldLifecycleStage)) {
                final LifeCycleStage lifeCycleStage = lifeCycleStageManager.partialGet(orgId, contact.getLifeCycleStage().id());

                buildAndLoadTimeline(contactRow, contact, oldLifecycleStage, lifeCycleStage.getName(),
                        fields.get(lifecycleStageKey).i18nName(), lifecycleStageKey, targetContainer);
            }
            final LifeCycleStatus contactLifeCycleStatus = contact.getLifeCycleStatus();

            LifeCycleStatus existingLifeCycleStatus = null;
            boolean isLifeCycleStatusNonNull = false;

            if (Objects.nonNull(oldLifecycleStatus)) {
                existingLifeCycleStatus = lifeCycleStatusManager.partialGet(orgId, contactLifeCycleStatus.id());
                isLifeCycleStatusNonNull = Objects.nonNull(existingLifeCycleStatus);

                if (isLifeCycleStatusNonNull) {
                    buildAndLoadTimeline(contactRow, contact, oldLifecycleStatus, existingLifeCycleStatus.getName(),
                            fields.get(lifecycleStatusKey).i18nName(), lifecycleStatusKey, targetContainer);
                }
            }

            if (Objects.nonNull(contactLifeCycleStatus)) {
                final LifeCycleStatus lifeCycleStatus = isLifeCycleStatusNonNull ? existingLifeCycleStatus
                        : lifeCycleStatusManager.partialGet(orgId, contactLifeCycleStatus.id());
                final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

                if (Objects.nonNull(label) && Objects.equals(LifeCycleStatus.Label.CLOSED_LOST.getId(), label.getId())) {
                    final Option contactLostReason = contact.getLostReason();

                    if (Objects.nonNull(contactLostReason)) {
                        final Option option = optionService.get(orgId, contactLostReason.getFieldId(), contactLostReason.id());

                        if (!Objects.equals(oldLostReason, option.getOption())) {
                            buildAndLoadTimeline(contactRow, contact, oldLostReason, option.getOption(),
                                    fields.get(lostReasonKey).i18nName(), lostReasonKey, targetContainer);
                        }
                    }
                }
            }

            if (Objects.nonNull(oldSubscriptionStatus)) {
                final Option subscriptionStatus = contact.getSubscriptionStatus();
                final Option option = optionService.get(orgId, subscriptionStatus.getFieldId(), subscriptionStatus.id());

                if (!Objects.equals(oldSubscriptionStatus, option.getOption())) {
                    buildAndLoadTimeline(contactRow, contact, oldSubscriptionStatus, option.getOption(),
                            fields.get(subscriptionStatusKey).i18nName(), subscriptionStatusKey, targetContainer);
                }
            }

            if (Objects.nonNull(oldName)) {
                buildAndLoadTimeline(contactRow, contact, oldName, newName,
                        fields.get(firstNameKey).i18nName(), firstNameKey, targetContainer);
            }

            if (Objects.nonNull(oldSalesOwner)) {
                buildAndLoadTimeline(contactRow, contact, oldSalesOwner, String.valueOf(contact.getSalesOwner().id()),
                        fields.get(salesOwnerKey).i18nName(), salesOwnerKey, targetContainer);
            }

            if (Objects.nonNull(newCreatedTime) && !Objects.equals(oldCreatedAtTime, newCreatedTime)) {
                buildAndLoadTimeline(contactRow, contact, oldCreatedAtTime, newCreatedTime,
                        fields.get(createdAtKey).i18nName(), createdAtKey, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the event creation timeline based on {@link TimelineType}.
     * </p>
     *
     * @param contactRow      Holds  the row of a contact table for the given contact id
     * @param contact         An entity that holds the contact attributes
     * @param type            Represents the {@link TimelineType}
     * @param key             Represents the {@link I18nKey}
     * @param targetContainer Holds the updated information in container
     */
    private void loadEventCreationTimeline(final Row contactRow, final Contact contact,
                                           final TimelineType type, final I18nKey key, final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(true, contactRow, contact);

        switch (type) {
            case CONTACT_FORM_SUBMISSION -> {
                timeline.setAction(TimelineActionFields.SUBMITTED);
                timeline.setTimelineType(TimelineType.CONTACT_FORM_SUBMISSION);
            }
            case CONTACT_CREATED -> {
                final FieldTimeline fieldTimeline = new FieldTimeline(Collections.emptyList(), contact.getFullName(), key);

                timeline.setFieldTimeline(fieldTimeline);
            }
            case CONTACT_CREATE_UPON_CALL -> {
                final FieldTimeline fieldTimeline = new FieldTimeline(Collections.emptyList(), contact.getFullName(), key);

                timeline.setFieldTimeline(fieldTimeline);
                timeline.setTimelineType(TimelineType.CONTACT_CREATE_UPON_CALL);
            }
        }

        loadContactTimelineRowIntoContainer(timeline, contact.id(), targetContainer);
    }

    /**
     * <p>
     * Builds and loads the timeline based on existing and new values.
     * </p>
     *
     * @param contactRow      Represents the contact {@link Row}
     * @param contact         Represents the {@link Contact}
     * @param oldValue        Represents the old value
     * @param newValue        Represents the new value
     * @param fieldName       Represents the field name
     * @param contactField    Represents the contact field
     * @param targetContainer Holds the updated information of contact timeline field values
     */
    private void buildAndLoadTimeline(final Row contactRow, final Contact contact, final String oldValue,
                                      final String newValue, final I18nKey fieldName, final String contactField,
                                      final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(false, contactRow, contact);

        switch (contactField) {
            case "lifeCycleStage" -> timeline.setTimelineType(TimelineType.CONTACT_LIFE_CYCLE_STAGE_UPDATED);
            case "lifeCycleStatus" -> timeline.setTimelineType(TimelineType.CONTACT_LIFE_CYCLE_STATUS_UPDATED);
            case "contactLostReason" -> {

                if (Objects.isNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.CONTACT_CLOSED_LOST_REASON_SET);
                    timeline.setAction(TimelineActionFields.SET);
                } else {
                    timeline.setTimelineType(TimelineType.CONTACT_CLOSED_LOST_REASON_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "subscriptionStatus" -> {
                timeline.setTimelineType(TimelineType.SUBSCRIPTION_STATUS_CHANGED);
                timeline.setAction(TimelineActionFields.CHANGED);
            }
            case "firstName" -> {

                if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.CONTACT_NAME_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "salesOwner" -> {
                timeline.setTimelineType(TimelineType.CONTACT_SALES_OWNER_REASSIGNED);
                timeline.setAction(TimelineActionFields.REASSIGNED);
            }
            case "createdAt" -> {
                timeline.setTimelineType(TimelineType.CONTACT_CREATED_TIME_CHANGED);
                timeline.setAction(TimelineActionFields.UPDATED);
            }
        }

        if (!Objects.equals(oldValue, newValue)) {
            final FieldTimeline fieldTimeline = new FieldTimeline(Objects.isNull(oldValue)
                    ? Collections.emptyList() : List.of(oldValue), newValue, fieldName);

            timeline.setFieldTimeline(fieldTimeline);
        }

        loadContactTimelineRowIntoContainer(timeline, contact.id(), targetContainer);
    }

    /**
     * <p>
     * Loads the old contact values from the existing {@link DataContainer} and updates the values into the existing {@link Map}.
     * </p>
     *
     * @param contact          Represents the {@link Contact}
     * @param oldContactValues Represents the old contact values
     * @param dataContainer    Holds the existing container of contact related field values
     */
    private void loadOldContactValues(final Contact contact, final Map<String, String> oldContactValues, final DataContainer dataContainer) {
        final Row oldContactRow = dataContainer.getRow(getTable(), contact.id());
        final Long oldLifeCycleStageId = oldContactRow.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);
        final Long oldLifeCycleStatusId = oldContactRow.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID);
        final Long orgId = oldContactRow.getOrgId();
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final Option lostReason = contact.getLostReason();
        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final String fullName = contact.getFullName();
        final CrmUser salesOwner = contact.getSalesOwner();
        final String oldContactName = Contact.getFullName(oldContactRow.get(MCOCONTACT.FIRST_NAME), oldContactRow.get(MCOCONTACT.LAST_NAME));
        final Long oldSalesOwnerId = oldContactRow.get(MCOCONTACT.OWNER_ID);
        final DateTime createdTime = oldContactRow.get(MCOCONTACT.CREATED_TIME);

        if (Objects.nonNull(subscriptionStatus)) {
            loadOldOptionValue(orgId, oldContactRow.getPKValue(), ContactFields.SUBSCRIPTION_STATUS, subscriptionStatus, dataContainer, oldContactValues);
        }

        if (Objects.nonNull(lifeCycleStage) && !Objects.equals(oldLifeCycleStageId, lifeCycleStage.id())) {
            final String oldLifeCycleStageName = lifeCycleStageManager.get(orgId, oldLifeCycleStageId).getName();

            oldContactValues.put(ContactFields.LIFECYCLE_STAGE, oldLifeCycleStageName);
        }

        if (Objects.nonNull(lifeCycleStatus) && !Objects.equals(oldLifeCycleStatusId, lifeCycleStatus.id())) {
            final String oldLifeCycleStatusName = lifeCycleStatusManager.get(orgId, oldLifeCycleStatusId).getName();

            oldContactValues.put(ContactFields.LIFECYCLE_STATUS, oldLifeCycleStatusName);
        }

        if (Objects.nonNull(lostReason)) {
            loadOldOptionValue(orgId, oldContactRow.getPKValue(), ContactFields.CONTACT_LOST_REASON, lostReason, dataContainer, oldContactValues);
        }

        if (Objects.nonNull(fullName) && !Objects.equals(oldContactName, fullName)) {
            oldContactValues.put(ContactFields.FIRST_NAME, oldContactName);
        }

        if (Objects.nonNull(createdTime)) {
            oldContactValues.put(ContactFields.CREATED_AT, String.valueOf(createdTime.getTimeStamp()));
        }

        if (Objects.nonNull(salesOwner) && !Objects.equals(salesOwner.id(), oldSalesOwnerId)) {
            oldContactValues.put(ContactFields.SALES_OWNER, Objects.isNull(oldSalesOwnerId) ? null : String.valueOf(oldSalesOwnerId));
        }
    }

    /**
     * <p>
     * Loads the old option values of the contact.
     * </p>
     *
     * @param orgId            Represents the organisation id.
     * @param contactId        Represents the contact id.
     * @param fieldName        Represents the name of the field.
     * @param option           Represents the option details.
     * @param dataContainer    Holds the old value of the given field.
     * @param oldContactValues Holds the old values of the contacts.
     */
    private void loadOldOptionValue(final long orgId, final long contactId, final String fieldName, final Option option,
                                    final DataContainer dataContainer, final Map<String, String> oldContactValues) {
        final Table contactDropdownTable = getContactDropdownTable();
        final Optional<FieldInfo> optionalField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, fieldName);

        optionalField.ifPresent(field -> {
            final Long fieldId = field.id();
            final Row contactDropdownRow = dataContainer.getRow(contactDropdownTable,
                    new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(getContactDropdownTable().getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldId)));

            if (Objects.nonNull(contactDropdownRow)) {
                final Long optionId = contactDropdownRow.get(MCONDROPDOWNFIELDS.OPTION_ID);
                final String oldOptionName = optionService.get(orgId, fieldId, optionId).getOption();

                oldContactValues.put(fieldName, oldOptionName);
            }
        });
    }

    /**
     * <p>
     * Builds the timeline when any action performed in contact.
     * </p>
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event
     * @param contactRow    Row which holds the values of the contact table
     * @param contact       An entity that holds the contact attributes
     * @return {@link Timeline}
     */
    private Timeline buildTimeline(final boolean isCreateEvent, final Row contactRow, final Contact contact) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(contact.orgId());
        timeline.setCategory(Timeline.Category.CONTACT);
        timeline.setAuditedTime(contactRow.get(MCOCONTACT.MODIFIED_TIME));

        if (isCreateEvent) {
            timeline.setAction(TimelineActionFields.CREATED);
            timeline.setTimelineType(TimelineType.CONTACT_CREATED);
            timeline.setOwnerId(contact.getCreatedBy().id());
        } else {
            timeline.setAction(TimelineActionFields.UPDATED);
            timeline.setOwnerId(contact.getUpdatedBy().id());
        }

        return timeline;
    }

    /**
     * <p>
     * Loads the contact timeline row into the container.
     * </p>
     *
     * @param contactId     Represent contact id
     * @param timeline      An entity that holds the timeline attributes
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    public void loadContactTimelineRowIntoContainer(final Timeline timeline, final Long contactId,
                                                    final DataContainer dataContainer) {
        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), dataContainer);
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), contactId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getContactTimelineTable()), dataContainer);
    }

    /**
     * Converts partial row into {@link Contact}
     *
     * @param contactRow Represents the contact row
     * @return {@link Contact}
     */
    public Contact convertRowToPartialModel(final Row contactRow) {
        final Contact contact = new Contact();
        final Long orgId = contactRow.getOrgId();

        contact.setId(contactRow.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
        final Long emailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);

        if (Objects.nonNull(emailId)) {
            final Email email = new Email();

            email.setId(emailId);
            contact.setPrimaryEmail(email);
        }

        return contact;
    }

    /**
     * Gets the company id for the contact id provided.
     *
     * @param contactId Refers the contact id.
     * @param orgId     Refers the organization id.
     * @return id of the contact company.
     */
    public Long getContactCompanyId(final Long contactId, final Long orgId) {
        final Table table = getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column contactPkColumn = table.getPKColumn();
        final Column contactCompanyColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);

        selectQuery.addSelectClause(contactCompanyColumn);
        selectQuery.addJoinClause(new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(contactCompanyTable);

        return Objects.isNull(row) ? null : row.get(contactCompanyColumn);
    }

    /**
     * <p>
     * Gets the contact full name by given contact id
     * </p>
     *
     * @param orgId     represents the organization id
     * @param contactId represents the contact id
     * @return by contact full name
     */
    public String getContactFullName(final Long orgId, final Long contactId) {
        final Table contactTable = getTable();

        return exists(orgId, contactId) ? convertRowToPartialModel(orgDataStore(orgId)
                                                                   .get(contactTable, new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)),
                                                                           List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)))
                                                                   .getRow(contactTable)).getFullName() : null;
    }

    /**
     * Gets the deal counts details for summary.
     *
     * @param orgId Refers the organization id.
     * @param id    Refers the contact id.
     * @return {@link DealStatistics} details.
     */
    public DealStatistics getDealStatistics(final Long orgId, final Long id) {
        final DealStatistics dealStatistics = new DealStatistics();

        dealStatistics.setOpenDealsCount(getDealCountByState(orgId, id, Deal.State.OPEN.getId()));
        dealStatistics.setWonDealsCount(getDealCountByState(orgId, id, Deal.State.WON.getId()));
        dealStatistics.setLostDealsCount(getDealCountByState(orgId, id, Deal.State.LOST.getId()));

        return dealStatistics;
    }

    /**
     * Fetches the web form name related to the contact.
     *
     * @param orgId Represents the organisation id.
     * @param id    Represents the contact id.
     * @return The name of the web form.
     */
    public String getWebFormName(final Long orgId, final Long id) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Table contactWebFormTable = Table.get(MWFCONTACTWEBFORM.TABLE);
        final Table webFormTable = webFormManager.getTable();
        final Column contactPKColumn = contactTable.getPKColumn();

        selectQuery.addJoinClause(List.of(
                new JoinClause(contactPKColumn, contactWebFormTable.getColumn(MWFCONTACTWEBFORM.CONTACT_ID), JoinClause.JoinType.LEFT),
                new JoinClause(contactWebFormTable.getColumn(MWFCONTACTWEBFORM.WEB_FORM_ID), webFormTable.getPKColumn())));
        selectQuery.addSelectClause(webFormTable.getColumn(OMWFWEBFORM.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPKColumn, id)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(webFormTable);

        return Objects.isNull(row) ? null : (String) row.get(OMWFWEBFORM.NAME);
    }

    /**
     * <p>
     * Checks the existence of email address in email entity, returns true if it exists just once
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address
     * @return {@link Boolean} status of single existence of email address
     */
    private boolean ensureSingleExistence(final Long orgId, final String emailAddress) {
        final Table emailTable = emailManager.getTable();
        final CountQuery countQuery = CountQuery.get(emailTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMETEMAIL.VALUE), emailAddress)));

        return 1 == orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves a contact that mapped with the given email address
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address
     * @return {@link Contact} that is mapped with the given email address
     */
    public Contact getSenderInfo(final Long orgId, final String emailAddress) {
        if (ensureSingleExistence(orgId, emailAddress)) {
            final Table contactEmailTable = getContactEmailTable();
            final Table emailTable = emailManager.getTable();
            final Table contactTable = getTable();
            final SelectQuery selectQuery = SelectQuery.get(contactTable);
            final String emailField = ContactFields.EMAILS;

            selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                    contactTable.getColumn(MCOCONTACT.OWNER_ID), contactTable.getColumn(MCOCONTACT.ORG_ID), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL)));
            selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMETEMAIL.VALUE), emailAddress))
                    .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID),
                            fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id())));
            selectQuery.setLimitClause(new LimitClause(1));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : convertRowToPartialModel(dataContainer.getRow(contactTable));
        }

        return null;
    }

    /**
     * <p>
     * Fetches the related contacts of the given email address.
     * </p>
     *
     * @param orgId          Represents an organization id.
     * @param emailAddresses Represents the collection of email address.
     * @return A map of email addresses to their associated contact names.
     */
    public Map<String, Contact> fetchContactsByEmails(final Long orgId, final Collection<String> emailAddresses) {
        final Map<String, Contact> contactMap = new HashMap<>();

        if (!emailAddresses.isEmpty()) {
            final Table contactEmailTable = getContactEmailTable();
            final Table emailTable = emailManager.getTable();
            final Table contactTable = getTable();

            final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
            final Column contactPkColumn = contactTable.getPKColumn();
            final Column contactEmailEntityColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
            final Column contactEmailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
            final SelectQuery selectQuery = SelectQuery.get(contactTable);

            selectQuery.addJoinClause(List.of(
                    new JoinClause(contactPkColumn, contactEmailEntityColumn),
                    new JoinClause(contactEmailIdColumn, emailTable.getPKColumn())));
            selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                    contactTable.getColumn(MCOCONTACT.OWNER_ID), emailValueColumn, contactEmailEntityColumn, contactEmailIdColumn));
            selectQuery.addOrderByClause(new OrderByClause(contactTable.getColumn(MCOCONTACT.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(emailValueColumn, emailAddresses)));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {

                for (final String emailAddress : emailAddresses) {
                    final Row contactEmailAddressRow = dataContainer.getRow(emailTable,
                            new WhereClause(Criteria.eq(emailValueColumn, emailAddress)));

                    if (Objects.nonNull(contactEmailAddressRow)) {
                        final Row contactEmailMappingRow = dataContainer.getRow(contactEmailTable,
                                new WhereClause(Criteria.eq(contactEmailIdColumn, contactEmailAddressRow.getPKValue())));

                        if (Objects.nonNull(contactEmailMappingRow)) {
                            final Row contactRow = dataContainer.getRow(contactTable,
                                    new WhereClause(Criteria.eq(contactPkColumn, contactEmailMappingRow.get(MCONEMAILFIELDS.ENTITY_ID))));

                            if (Objects.nonNull(contactRow)) {
                                final Contact contact = convertRowToPartialModel(contactRow);
                                final Email email = new Email();

                                email.setValue(emailAddress);
                                email.setId(contactEmailMappingRow.get(contactEmailIdColumn));
                                contact.setPrimaryEmail(email);

                                contactMap.put(emailAddress, contact);
                            }
                        }
                    }
                }
            }
        }

        return contactMap;
    }

    /**
     * <p>
     * Gets the IDs of the contacts newly added into the given container.
     * </p>
     *
     * @param dataContainer Holds the records added into the container
     * @return The collection of newly added contact IDs
     */
    public Collection<Long> getContactIds(final DataContainer dataContainer) {
        return dataContainer.getRows(getTable()).filter(Row::isNewRow).map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * Creates new contacts using email and life cycle stage id.
     * </p>
     *
     * @param contactRecipients Represents the {@link ContactRecipients}
     * @param targetContainer   Holds the updated information of data to stored ito the target container
     */
    public void createContactsUsingEmailAndLifeCycleStage(final ContactRecipients contactRecipients, final DataContainer targetContainer) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final Table contactDropdownTable = getContactDropdownTable();
        final String emailKey = ContactFields.EMAILS;
        final Long currentTime = Instant.now().toEpochMilli();
        final String subscriptionTypeKey = ContactFields.SUBSCRIPTION_TYPES;
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailKey, subscriptionStatusKey,
                subscriptionTypeKey);
        final Long emailFieldId = fields.get(emailKey).id();
        final Consumer<Collection<Contact>> setContactsByAddressType = (contacts -> {

            if (Objects.nonNull(contacts) && !contacts.isEmpty()) {

                for (final Contact contact : contacts) {
                    final Row contactRow = new Row(contactTable);
                    final Row contactEmailRow = new Row(contactEmailTable);
                    final Row emailRow = new Row(emailTable);
                    final Row optionRow = new Row(contactDropdownTable);
                    final Email email = contact.getEmails().stream().findFirst().orElse(null);
                    final Option subscriptionStatus = contact.getSubscriptionStatus();
                    final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
                    final Long contactPkValue = contactRow.getPKValue();
                    final Long ownerId = contact.getOwnerId();
                    final Option lostReason = contact.getLostReason();

                    contact.setId(contactPkValue);
                    contactRow.set(MCOCONTACT.FIRST_NAME, contact.getFirstName());
                    contactRow.set(MCOCONTACT.LIFE_CYCLE_STAGE_ID, contact.getLifeCycleStage().id());
                    contactRow.set(MCOCONTACT.LIFE_CYCLE_STATUS_ID, contact.getLifeCycleStatus().id());
                    contactRow.set(MCOCONTACT.CREATED_TIME, currentTime);
                    contactRow.set(MCOCONTACT.MODIFIED_TIME, currentTime);
                    contactRow.set(MCOCONTACT.LAST_ASSIGNED_AT, currentTime);
                    contactRow.set(MCOCONTACT.PRIMARY_EMAIL, emailRow.getPKValue());
                    contactRow.set(MCOCONTACT.OWNER_ID, ownerId);
                    contactRow.set(MCOCONTACT.UPDATED_BY, ownerId);
                    contactRow.set(MCOCONTACT.CREATED_BY, ownerId);

                    if (Objects.nonNull(subscriptionStatus)) {
                        optionRow.set(MCONDROPDOWNFIELDS.OPTION_ID, subscriptionStatus.id());
                        optionRow.set(MCONDROPDOWNFIELDS.ENTITY_ID, contactPkValue);
                        optionRow.set(MCONDROPDOWNFIELDS.FIELD_ID, fields.get(subscriptionStatusKey).id());

                        targetContainer.addNewRow(optionRow);
                    }

                    if (Objects.nonNull(subscriptionTypes)) {
                        final Long subscriptionTypeId = fields.get(subscriptionTypeKey).id();

                        subscriptionTypes.forEach(option -> {
                            final Row subscriptionTypeRow = new Row(contactDropdownTable);

                            subscriptionTypeRow.set(MCONDROPDOWNFIELDS.OPTION_ID, option.id());
                            subscriptionTypeRow.set(MCONDROPDOWNFIELDS.ENTITY_ID, contactPkValue);
                            subscriptionTypeRow.set(MCONDROPDOWNFIELDS.FIELD_ID, subscriptionTypeId);

                            targetContainer.addNewRow(subscriptionTypeRow);
                        });
                    }

                    if (Objects.nonNull(email)) {
                        emailRow.set(OMETEMAIL.VALUE, email.getValue());
                        emailRow.set(OMETEMAIL.TYPE, email.getType().getId());
                        final Email primaryEmail = new Email();

                        primaryEmail.setId(emailRow.getPKValue());
                        primaryEmail.setValue(email.getValue());
                        primaryEmail.setType(email.getType());
                        contact.setPrimaryEmail(primaryEmail);
                    }
                    contactEmailRow.set(MCONEMAILFIELDS.EMAIL_ID, emailRow.getPKValue());
                    contactEmailRow.set(MCONEMAILFIELDS.ENTITY_ID, contactPkValue);
                    contactEmailRow.set(MCONEMAILFIELDS.FIELD_ID, emailFieldId);

                    targetContainer.addNewRow(contactRow);
                    targetContainer.addNewRow(emailRow);
                    targetContainer.addNewRow(contactEmailRow);

                    loadContactTimeline(contact, true, Map.of(), contactRow, targetContainer);

                    if (Objects.nonNull(lostReason)) {
                        loadLostReasonIntoContainer(contactPkValue, lostReason, null, targetContainer);
                    }
                }
            }
        });

        final Map<AddressType, Collection<Contact>> contacts = contactRecipients.getContacts();

        if (Objects.nonNull(contacts)) {
            setContactsByAddressType.accept(contacts.get(AddressType.TO));
            setContactsByAddressType.accept(contacts.get(AddressType.BCC));
            setContactsByAddressType.accept(contacts.get(AddressType.CC));
            setContactsByAddressType.accept(contacts.get(AddressType.FROM));
        }
    }

    /**
     * <p>
     * Gets the related contact of the mail by using the email address.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param emailIds Represents the collection of email address ids.
     * @return Collection of {@link Contact} with its related email address.
     */
    public Map<String, Contact> retrieveContactsByEmailAddress(final long orgId, final Collection<Long> emailIds) {
        final String emailKey = ContactFields.EMAILS;
        final Table emailTable = emailManager.getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table contactTable = getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final Column contactPKColumn = contactTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column companyEntityIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
        final Column contactCompanyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);
        final Column phoneEntityIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Column contactPhoneIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID);
        final Column contactCreatedTimeColumn = contactTable.getColumn(MCOCONTACT.CREATED_TIME);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addJoinClause(List.of(new JoinClause(contactPKColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn()),
                new JoinClause(contactPKColumn, companyEntityIdColumn, leftJoin),
                new JoinClause(contactCompanyIdColumn, companyTable.getPKColumn(), leftJoin), new JoinClause(contactPKColumn, phoneEntityIdColumn, leftJoin),
                new JoinClause(contactPhoneIdColumn, phoneTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(emailTable, phoneTable, contactEmailTable);
        selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN), contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), contactTable.getColumn(MCOCONTACT.DESIGNATION),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                contactCreatedTimeColumn, contactPhoneIdColumn, phoneEntityIdColumn, companyEntityIdColumn,
                contactCompanyIdColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailTable.getPKColumn(), emailIds))
                .and(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID),
                        fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailKey).get(emailKey).id()))));
        selectQuery.addOrderByClause(new OrderByClause(contactCreatedTimeColumn));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        } else {
            return buildContacts(emailIds, dataContainer);
        }
    }

    /**
     * <p>
     * Links the given email address to an existing contact
     * </p>
     *
     * @param orgId     Represents an organization id.
     * @param userId    Represents the user id.
     * @param email     Represents the email.
     * @param contactId Represents the id of the {@link Contact}
     */
    public void linkEmailToContact(final long orgId, final long userId, final Long contactId,
                                   final Email email, final DataContainer targetContainer) {
        final String emailKey = ContactFields.EMAILS;
        final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailKey);
        final Row contactEmailRow = new Row(getContactEmailTable());
        final Row emailRow = new Row(emailManager.getTable());

        emailRow.set(OMETEMAIL.TYPE, Email.Type.WORK.getId());
        emailRow.set(OMETEMAIL.VALUE, email.getValue());

        contactEmailRow.set(MCONEMAILFIELDS.ENTITY_ID, contactId);
        contactEmailRow.set(MCONEMAILFIELDS.EMAIL_ID, emailRow.getPKValue());
        contactEmailRow.set(MCONEMAILFIELDS.FIELD_ID, contactFields.get(emailKey).id());

        email.setId(emailRow.getPKValue());
        targetContainer.addNewRow(emailRow);
        targetContainer.addNewRow(contactEmailRow);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(contactId), Source.CONTACT, targetContainer);
    }

    /**
     * <p>
     * Checks if the provided email address exists to the given organization id and contact id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactId  Represents the contact id
     * @param emailValue Represents the email value
     * @return true if the email exists for the contact, otherwise false
     */
    public boolean isEmailExistsInContact(final long orgId, final long contactId, final String emailValue) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final CountQuery countQuery = CountQuery.get(contactTable);
        final Column contactPkColumn = contactTable.getPKColumn();

        countQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId))
                .and(Criteria.eq(emailTable.getColumn(OMETEMAIL.VALUE), emailValue)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the emails that are not associated with any contact.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param emails Represents the collection of emails.
     * @return Collection of the emails that are not associated with any contact.
     */
    public Map<AddressType, Collection<String>> getUnassociatedEmails(final long orgId, final Map<AddressType, Collection<String>> emails) {

        if (Objects.nonNull(emails) && !emails.isEmpty()) {
            final Table emailTable = emailManager.getTable();
            final Table contactEmailTable = getContactEmailTable();
            final Table contactTable = getTable();
            final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
            final SelectQuery selectQuery = SelectQuery.get(emailTable);
            final Collection<String> emailValues = new HashSet<>();

            for (final Collection<String> value : emails.values()) {
                emailValues.addAll(value);
            }
            selectQuery.addJoinClause(DmlUtil.getJoins(emailTable, contactEmailTable, contactTable));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(emailValueColumn, emailValues)));
            selectQuery.addSelectClause(emailValueColumn);

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (dataContainer.isEmpty()) {
                return emails;
            } else {
                return emails.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().stream().filter(email -> {
                                    final Row emailRow = dataContainer.getRow(emailTable,
                                            new WhereClause(Criteria.eq(emailValueColumn, email)));

                                    return Objects.isNull(emailRow);
                                })
                                .collect(Collectors.toUnmodifiableSet())
                ));
            }
        }

        return Collections.emptyMap();
    }

    /**
     * <p>
     * Fetches the existing contact id for merging the newly synced contacts from provider
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return the exiting contact id
     */
    public Long fetchExistingContactId(final Contact contact) {
        final Long id = contact.id();

        if (Objects.isNull(id)) {
            final Table table = getTable();
            final Long orgId = contact.orgId();
            final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(MCOCONTACT.OWNER_ID), contact.getOwnerId()))
                    .and(Criteria.eq(table.getColumn(MCOCONTACT.LAST_NAME), contact.getLastName()))
                    .and(Criteria.eq(table.getColumn(MCOCONTACT.FIRST_NAME), contact.getFirstName()));
            final Map<String, FieldInfo> fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
            final Long emailsAssociatedContactId = getEmailAssociatedContactId(orgId, whereClause, fieldInfo.get(ContactFields.EMAILS).id(), contact.getEmails());

            if (Objects.isNull(emailsAssociatedContactId)) {
                final Long phonesAssociatedContactId = getPhoneAssociatedContactId(orgId, whereClause, fieldInfo.get(ContactFields.PHONES).id(), contact.getPhones());

                if (Objects.isNull(phonesAssociatedContactId)) {
                    final Long companyAssociatedContactId = getCompanyAssociatedContactId(orgId, whereClause, fieldInfo.get(ContactFields.COMPANY).id(), contact.getCompany());

                    return Objects.isNull(companyAssociatedContactId) ? null : companyAssociatedContactId;
                } else {
                    return phonesAssociatedContactId;
                }
            } else {
                return emailsAssociatedContactId;
            }
        }

        return id;
    }

    /**
     * <p>
     * Gets the contact id that is associated to the given company
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause}
     * @param fieldId     Represents the field id for company
     * @param company     Represents the company
     * @return the contact id
     */
    private Long getCompanyAssociatedContactId(final Long orgId, final WhereClause whereClause, final Long fieldId, final Company company) {
        if (Objects.nonNull(company)) {
            final String companyName = company.getName();
            final Table table = getTable();
            final Table contactCompanyTable = getContactCompanyTable();
            final Table companyTable = companyManager.getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addSelectClause(table.getPKColumn());
            selectQuery.addJoinClause(List.of(new JoinClause(table.getPKColumn(), contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID)),
                    new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn())));
            selectQuery.setWhereClause(whereClause.and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), fieldId))
                    .and(Criteria.eq(companyTable.getColumn(MCOCOMPANY.NAME), companyName)));
            selectQuery.setLimitClause(new LimitClause(1));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : dataContainer.getRow(table).getPKValue();
        }
        return null;
    }

    /**
     * <p>
     * Gets the contact id that is associated to the given phones
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause}
     * @param fieldId     Represents the field id for phone
     * @param phones      Represents the phones
     * @return the contact id
     */
    private Long getPhoneAssociatedContactId(final Long orgId, final WhereClause whereClause, final Long fieldId, final Collection<Phone> phones) {
        if (Objects.nonNull(phones) && !phones.isEmpty()) {
            final Table table = getTable();
            final Table contactPhoneTable = getContactPhoneTable();
            final Table phoneTable = phoneManager.getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);
            final Column pkColumn = table.getPKColumn();

            selectQuery.addJoinClause(List.of(new JoinClause(pkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)),
                    new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn())));
            selectQuery.addSelectClause(pkColumn);
            selectQuery.setLimitClause(new LimitClause(1));
            selectQuery.setWhereClause(whereClause.and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId))
                    .and(Criteria.in(phoneTable.getColumn(OMETPHONE.NUMBER), phones.stream().map(Phone::getNumber).toList())));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : dataContainer.getRow(table).getPKValue();
        }
        return null;
    }

    /**
     * <p>
     * Gets the contact id that is associated to the given emails
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause}
     * @param fieldId     Represents the field id for email
     * @param emails      Represents the emails
     * @return the contact id
     */
    private Long getEmailAssociatedContactId(final long orgId, final WhereClause whereClause, final Long fieldId, final Collection<Email> emails) {
        if (Objects.nonNull(emails) && !emails.isEmpty()) {
            final Table table = getTable();
            final Table contactEmailTable = getContactEmailTable();
            final Table emailTable = emailManager.getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);
            final Column pkColumn = table.getPKColumn();

            selectQuery.addSelectClause(pkColumn);
            selectQuery.addJoinClause(List.of(new JoinClause(pkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
            selectQuery.setWhereClause(whereClause.and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId))
                    .and(Criteria.in(emailTable.getColumn(OMETEMAIL.VALUE), emails.stream().map(Email::getValue).toList())));
            selectQuery.setLimitClause(new LimitClause(1));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : dataContainer.getRow(table).getPKValue();
        }
        return null;
    }

    /**
     * <p>
     * Checks whether the contact name is already present or not
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return true, if exists
     */
    public boolean isNameExists(final Contact contact) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MCOCONTACT.FIRST_NAME), contact.getFirstName()))
                .and(Criteria.eq(table.getColumn(MCOCONTACT.LAST_NAME), contact.getLastName())));

        return orgDataStore(contact.orgId()).get(countQuery) > 0;
    }

    /**
     * <p>
     * Construct contact objects from the {@link DataContainer}
     * </p>
     *
     * @param emailIds      Represents the collection of email ids.
     * @param dataContainer Represents the entity of {@link DataContainer}
     * @return Collection of {@link Contact}
     */
    private Map<String, Contact> buildContacts(final Collection<Long> emailIds, final DataContainer dataContainer) {
        final Table emailTable = emailManager.getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table contactTable = getTable();
        final Column emailPkColumn = emailTable.getPKColumn();
        final Column contactEmailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Map<String, Contact> relatedContacts = new HashMap<>();
        final String phoneKey = ContactFields.PHONES;
        final String companyKey = ContactFields.COMPANY;
        final String emailKey = ContactFields.EMAILS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                phoneKey, companyKey, emailKey);
        final Long phoneFieldId = fields.get(phoneKey).id();
        final Long emailFieldId = fields.get(emailKey).id();
        final Long companyFieldId = fields.get(companyKey).id();

        for (final Long emailId : emailIds) {
            final Row emailRow = dataContainer.getRow(emailTable,
                    new WhereClause(Criteria.eq(emailPkColumn, emailId)));

            if (Objects.nonNull(emailRow)) {
                final Row contactEmailRow = dataContainer.getRow(contactEmailTable,
                        new WhereClause(Criteria.eq(contactEmailIdColumn, emailRow.getPKValue())));

                if (Objects.nonNull(contactEmailRow)) {
                    final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPkColumn,
                            contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID))));
                    final Long contactId = contactRow.getPKValue();
                    final Contact contact = new Contact();
                    final Long orgId = contactRow.getOrgId();

                    contact.setId(contactId);
                    contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
                    contact.setOrgId(orgId);
                    contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                    contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                    contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
                    contact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
                    contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
                    contact.setDesignation(contactRow.get(MCOCONTACT.DESIGNATION));
                    contact.setCompany(buildCompany(contactId, companyFieldId, dataContainer));

                    buildPhones(contactId, contact, contactRow, phoneFieldId, dataContainer);
                    buildEmails(contactId, contact, contactRow, emailFieldId, dataContainer);

                    relatedContacts.put(emailRow.get(OMETEMAIL.VALUE), contact);
                }
            }
        }

        return relatedContacts;
    }

    /**
     * <p>
     * Gets the total number of {@link Contact} by given company id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param companyId      Represents a {@link Company} id
     * @param companyFieldId Represents a {@link Company} field id
     * @return The total number of {@link Contact}
     */
    public Long getRelatedContactsCount(final long orgId, final long companyId, final long companyFieldId) {
        final Table contactCompanyTable = getContactCompanyTable();
        final CountQuery countQuery = CountQuery.get(contactCompanyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyId))
                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the related {@link Contact} by given company id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param companyId      Represents a {@link Company} id
     * @param companyFieldId Represents a {@link Company} field id
     * @param start          Represents the starting index of {@link Contact} to be fetched
     * @param limit          Represents the maximum number of {@link Contact} to be fetched
     * @return The collection of {@link Contact}
     */
    public Collection<Contact> getRelatedContactsByCompany(final long orgId, final long companyId, final long companyFieldId,
                                                           final int start, final int limit) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final Column contactPKColumn = contactTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery innerSelectQuery = SelectQuery.get(contactTable);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        innerSelectQuery.addSelectClause(contactPKColumn);
        innerSelectQuery.addJoinClause(new JoinClause(contactPKColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID)));
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(contactCompanyTable
                .getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyId))
                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));

        selectQuery.addJoinClause(List.of(
                new JoinClause(contactPKColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactPKColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin)));

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.ORG_ID), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE),
                phoneTable.getColumn(OMETPHONE.NUMBER), phoneTable.getColumn(OMETPHONE.TYPE),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                contactTable.getColumn(MCOCONTACT.DESIGNATION)));
        selectQuery.addSelectClause(contactEmailTable, contactPhoneTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPKColumn, innerSelectQuery)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(contactTable).map(contactRow ->
                convertRelatedContactRowToModel(contactRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Gets the already existing emails for the given contact
     * </p>
     *
     * @param id    Represents the contact id
     * @param orgId Represents an organization id
     * @return the collection of {@link Email}
     */
    public Collection<Email> getExistingEmails(final Long id, final long orgId, final Long emailFieldId) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(emailTable);
        selectQuery.addJoinClause(getJoinClauseForContactEmail());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), id))
                .and(Criteria.eq(getContactEmailTable().getColumn(MCONEMAILFIELDS.FIELD_ID), emailFieldId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(emailTable).map(this::convertEmailRowToModel).toList();
    }

    /**
     * <p>
     * Gets the already existing phones for the given contact
     * </p>
     *
     * @param id    Represents the contact id
     * @param orgId Represents an organization id
     * @return the collection of {@link Phone}
     */
    public Collection<Phone> getExistingPhones(final Long id, final long orgId, final Long phoneFieldId) {
        final Table contactTable = getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(phoneTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), id))
                .and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), phoneFieldId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(phoneTable).map(this::convertPhoneRowToModel).toList();
    }

    /**
     * <p>
     * Gets the existing address to update the new address
     * </p>
     *
     * @param id    Represents the contact id
     * @param orgId Represents an organization id
     * @return the address id
     */
    public Long getExistingAddressId(final Long id, final long orgId, final Long addressFieldId) {
        final Table contactTable = getTable();
        final Table contactAddressTable = getContactAddressTable();
        final Table addressTable = addressManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(addressTable.getPKColumn());
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), contactAddressTable.getColumn(MCONADDRESSFIELDS.ENTITY_ID)),
                new JoinClause(contactAddressTable.getColumn(MCONADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), id))
                .and(Criteria.eq(contactAddressTable.getColumn(MCONADDRESSFIELDS.FIELD_ID), addressFieldId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRow(addressTable).getPKValue();
    }

    /**
     * <p>
     * Converts the phone row into model
     * </p>
     *
     * @param row Represents the {@link Row}
     * @return the {@link Phone}
     */
    public Phone convertPhoneRowToModel(final Row row) {
        final Phone phone = new Phone();
        final Integer countryId = row.get(OMETPHONE.COUNTRY_ID);
        final Integer type = row.get(OMETPHONE.TYPE);

        if (Objects.nonNull(type)) {
            phone.setType(Phone.Type.valueOf(type));
        }

        phone.setNumber(row.get(OMETPHONE.NUMBER));
        phone.setOrgId(row.getOrgId());
        phone.setId(row.getPKValue());

        if (Objects.nonNull(countryId)) {
            phone.setCountry(Country.getCountryById(countryId));
        }
        return phone;
    }

    /**
     * <p>
     * Converts the email row into model
     * </p>
     *
     * @param row Represents the {@link Row}
     * @return the {@link Email}
     */
    public Email convertEmailRowToModel(final Row row) {
        final Email email = new Email();
        final Integer type = row.get(OMETEMAIL.TYPE);

        if (Objects.nonNull(type)) {
            email.setType(Email.Type.valueOf(type));
        }
        email.setValue(row.get(OMETEMAIL.VALUE));
        email.setId(row.getPKValue());
        email.setOrgId(row.getOrgId());
        return email;
    }

    /**
     * <p>
     * Converts the row to {@link Contact}
     * </p>
     *
     * @param contactRow    Represents the contact row
     * @param dataContainer A container that holds the contact values
     * @return The {@link Contact} data
     */
    private Contact convertRelatedContactRowToModel(final Row contactRow, final DataContainer dataContainer) {
        final Contact contact = new Contact();
        final Long contactId = contactRow.getPKValue();
        final String companyNameKey = ContactFields.COMPANY;
        final String phonesNameKey = ContactFields.PHONES;
        final String emailsNameKey = ContactFields.EMAILS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyNameKey);
        final Map<String, FieldInfo> secureFields = secureFieldService.groupDefaultFieldsByAliasName(emailsNameKey, phonesNameKey);

        contact.setId(contactId);
        contact.setOrgId(contactRow.getOrgId());
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setDesignation(contactRow.get(MCOCONTACT.DESIGNATION));
        contact.setCompany(buildCompany(contactId, fields.get(companyNameKey).id(), dataContainer));

        if (secureFields.containsKey(phonesNameKey)) {
            buildPhones(contactId, contact, contactRow, secureFields.get(phonesNameKey).id(), dataContainer);
        }

        if (secureFields.containsKey(emailsNameKey)) {
            buildEmails(contactId, contact, contactRow, secureFields.get(emailsNameKey).id(), dataContainer);
        }

        return contact;
    }

    /**
     * Groups the primary emails according to the provided contacts for a particular organization and user.
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the contact email ids
     * @return A {@link Map} of primary email ids to their corresponding contact ids
     */
    public Map<Long, Collection<Long>> groupPrimaryEmailsByContacts(final long orgId, final Collection<Long> contactIds) {

        if (Objects.isNull(contactIds) || !exists(orgId, contactIds)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
        final Table contactTable = getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Map<Long, Collection<Long>> contactEmails = new HashMap<>();

        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(contactTable).forEach(row -> {
            final Collection<Long> contactEmailIds = new HashSet<>();

            contactEmailIds.add(row.get(MCOCONTACT.PRIMARY_EMAIL));
            contactEmails.put(row.getPKValue(), contactEmailIds);
        });

        return contactEmails;
    }

    /**
     * Retrieves {@link Collection} of email values corresponding to the given contactEmailIds for a specific organization and user.
     *
     * @param orgId           Represents the organization id
     * @param userId          Represents the user id
     * @param contactEmailIds Represents the {@link Collection} of contact emails
     * @return {@link Collection} of email values
     */
    public Collection<String> getContactEmailValues(final long orgId, final long userId, final Collection<Long> contactEmailIds) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                subscriptionStatusKey).get(subscriptionStatusKey);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column contactEmailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final Table contactDropdownTable = getContactDropdownTable();
        final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);

        contactSelectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailIdColumn, emailTable.getPKColumn()),
                new JoinClause(contactPkColumn, contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID))));
        contactSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactEmailIdColumn, contactEmailIds))
                .and(Criteria.notEq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.OPTION_ID),
                        optionService.getOption(orgId, fieldInfo, Contact.SubscriptionStatus.SUBSCRIBED.getName()).id()))
                .and(Criteria.eq(contactTable.getColumn(MCOCONTACT.OWNER_ID), userId))
                .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldInfo.id())));

        return orgDataStore(orgId).get(contactSelectQuery).getRows(emailTable).map(row -> (String) row.get(OMETEMAIL.VALUE)).toList();
    }

    /**
     * <p>
     * Checks whether the primary email exists or not for the respective contact.
     * </p>
     *
     * @param orgId  Represents an organisation id
     * @param userId Represents the user id
     * @param id     Represents the contact id
     * @return true, if the primary email exists for the given contact; otherwise, false.
     */
    public boolean hasPrimaryEmail(final long orgId, final long userId, final Long id) {
        return hasPrimaryContactInfo(orgId, userId, id, Criteria.notEq(getTable().getColumn(MCOCONTACT.PRIMARY_EMAIL), null));
    }

    /**
     * <p>
     * Checks whether the primary phone exists or not for the respective contact.
     * </p>
     *
     * @param orgId  Represents an organisation id
     * @param userId Represents the user id
     * @param id     Represents the contact id
     * @return true, if the primary phone exists for the given contact; otherwise, false.
     */
    public boolean hasPrimaryPhone(final long orgId, final long userId, final Long id) {
        return hasPrimaryContactInfo(orgId, userId, id, Criteria.notEq(getTable().getColumn(MCOCONTACT.PRIMARY_PHONE), null));
    }

    /**
     * <p>
     * Fetches the recently mapped deal id for the provided contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Long}
     */
    public Long getRecentlyLinkedDealId(final long orgId, final long contactId) {
        final Table contactTable = getTable();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column contactPkColumn = contactTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactPkColumn));
        selectQuery.addSelectClause(dealTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId)));
        selectQuery.addOrderByClause(new OrderByClause(dealTable.getColumn(MDODEAL.CREATED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRow(dealTable).getPKValue();
    }

    /**
     * <p>
     * Checks whether the primary email or phone exists for an respective contact.
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param userId    Represents the user id
     * @param contactId Represents the contact id
     * @param criteria  Represents the {@link Criteria}
     * @return true, if the primary email or phone exists for the corresponding given contact; otherwise false
     */
    private boolean hasPrimaryContactInfo(final long orgId, final long userId, final Long contactId, final Criteria criteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), contactId))
                .and(Criteria.eq(table.getColumn(MCOCONTACT.OWNER_ID), userId)).and(criteria));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link Set} of {@link Email} mapped for the given {@link Collection} of contact ids
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the {@link Collection} of contact ids
     * @return {@link Set} of {@link Email}
     */
    public Collection<Email> getEmails(final long orgId, final Collection<Long> contactIds) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final String emailField = ContactFields.EMAILS;
        final Column contactPkColumn = contactTable.getPKColumn();

        selectQuery.addSelectClause(emailTable);
        selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds))
                .and(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID),
                        fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id()))));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptySet();
        } else {
            return dataContainer.getRows(emailTable).map(this::convertEmailRowToModel).collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Phone} mapped for the given {@link Collection} of contact ids
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the {@link Collection} of contact ids
     * @return the {@link Set} of {@link Phone}
     */
    public Collection<Phone> getPhones(final long orgId, final Collection<Long> contactIds) {
        final Table contactTable = getTable();
        final Table contactPhoneTable = getContactPhoneTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final String phoneField = ContactFields.PHONES;
        final Column contactPkColumn = contactTable.getPKColumn();

        selectQuery.addSelectClause(phoneTable);
        selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds))
                .and(new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID),
                        fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, phoneField).get(phoneField).id()))));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptySet();
        } else {
            return dataContainer.getRows(phoneTable).map(this::convertPhoneRowToModel).collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * <p>
     * Retrieves company details related to a contact within an organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return The {@link Company}
     */
    public Optional<Company> getRelatedCompany(final long orgId, final long contactId) {
        final String companyKey = ContactFields.COMPANY;
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey);

        if (fields.containsKey(companyKey)) {
            final Table contactTable = getTable();
            final Table companyTable = companyManager.getTable();
            final Table contactCompanyTable = getContactCompanyTable();
            final SelectQuery selectQuery = SelectQuery.get(contactTable);
            final Column contactPkColumn = contactTable.getPKColumn();
            final Column companyPkColumn = companyTable.getPKColumn();
            final Column companyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);
            final Column contactCompanyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
            final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

            selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactCompanyIdColumn, leftJoin),
                    new JoinClause(companyIdColumn, companyPkColumn, leftJoin)));
            selectQuery.addSelectClause(List.of(contactPkColumn, companyTable.getColumn(MCOCOMPANY.NAME)));
            selectQuery.addSelectClause(contactCompanyTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId)));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {
                final Row contactRow = dataContainer.getRow(contactTable);
                final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable,
                        new WhereClause(Criteria.eq(contactCompanyIdColumn, contactRow.getPKValue())).
                                and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id())));

                if (Objects.nonNull(contactCompanyRow)) {
                    final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyPkColumn,
                            contactCompanyRow.get(companyIdColumn))));

                    return Optional.ofNullable(companyRow).map(companyManager::convertRowToPartialModel);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Retrieves company details related to a contact within an organization. It does not consider custom company fields
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return The {@link Company}
     */
    public Optional<Long> getRelatedCompanyId(final long orgId, final long contactId) {
        final Table table = getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column contactPkColumn = table.getPKColumn();
        final Column contactCompanyColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);
        final Optional<FieldInfo> contactCompanyField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.COMPANY);

        selectQuery.addSelectClause(contactCompanyColumn);
        selectQuery.addJoinClause(new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId))
                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), contactCompanyField.get().id())));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(contactCompanyTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(row.get(contactCompanyColumn));
    }

    /**
     * <p>
     * Checks whether the company id is related to the contact id. It does not consider custom company fields
     * </p>
     *
     * @param orgId     Represents the org id
     * @param contactId Represents the contact id
     * @param companyId Represents the company id to check
     * @return true if related
     */
    public boolean isCompanyRelatedToContact(final long orgId, final long contactId, final long companyId) {
        final Table table = getTable();
        final Table contactCompanyTable = getContactCompanyTable();
        final CountQuery countQuery = CountQuery.get(table);
        final Column contactPkColumn = table.getPKColumn();
        final Optional<FieldInfo> contactCompanyField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.COMPANY);

        countQuery.addJoinClause(new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId))
                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), contactCompanyField.get().id()))
                .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Fetches email ids that are mapped with given emails addresses
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param emails Represents the email addresses
     * @return {@link Map} with collection of contact ids mapped to the email address
     */
    public Collection<Long> getEmailIds(final long orgId, final Collection<String> emails) {
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(emailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailTable.getColumn(OMETEMAIL.VALUE), emails)));
        selectQuery.addSelectClause(emailTable.getPKColumn());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return List.of();
        }

        return dataContainer.getRows(emailTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the {@link Map} of contact and email address mapped for given contact email ids
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param contactEmailIds Represents the contact email ids
     * @return {@link Map} of contact and email address
     */
    public Map<String, Contact> getContactEmails(final long orgId, final Collection<Long> contactEmailIds) {

        if (contactEmailIds.isEmpty()) {
            return Collections.emptyMap();
        }

        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Map<String, Contact> contactEmails = new HashMap<>();

        selectQuery.addSelectClause(emailTable, contactEmailTable);
        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.FIRST_NAME)));
        selectQuery.addJoinClause(getJoinClauseForContactEmail());
        selectQuery.setWhereClause(new WhereClause(Criteria.in(getContactEmailTable().getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        contactEmailIds.forEach(contactEmailId -> {
            final Row contactEmailRow = dataContainer.getRow(contactEmailTable, new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailId)));

            if (Objects.nonNull(contactEmailRow)) {
                final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID))));
                final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(), contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID))));

                if (Objects.nonNull(contactRow) && Objects.nonNull(emailRow)) {
                    final Contact contact = new Contact();
                    final Email email = new Email();
                    final String emailAddress = emailRow.get(OMETEMAIL.VALUE);

                    contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                    contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                    contact.setId(contactRow.getPKValue());
                    email.setValue(emailAddress);
                    contact.setPrimaryEmail(email);

                    contactEmails.put(emailAddress, contact);
                }
            }
        });

        return contactEmails;
    }

    /**
     * <p>
     * Gets the {@link Contact} mapped for the given contact email id and their respective organization
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactEmailIds Represents the {@link Collection} of contact email id
     * @return the {@link Contact}
     */
    public Map<Long, Contact> getContactByEmailIds(final long orgId, final Collection<Long> contactEmailIds) {

        if (Objects.nonNull(contactEmailIds) && !contactEmailIds.isEmpty()) {
            final Table contactEmailTable = getContactEmailTable();
            final Table contactTable = getTable();
            final Table emailTable = emailManager.getTable();
            final String emailField = ContactFields.EMAILS;
            final SelectQuery selectQuery = SelectQuery.get(contactEmailTable);
            final Column emailPkColumn = emailTable.getPKColumn();
            final Column contactEmailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);

            selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                    contactTable.getColumn(MCOCONTACT.OWNER_ID), contactTable.getColumn(MCOCONTACT.ORG_ID), contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID),
                    emailTable.getColumn(OMETEMAIL.VALUE), contactEmailEntityIdColumn));
            selectQuery.addJoinClause(List.of(new JoinClause(contactEmailEntityIdColumn, contactTable.getPKColumn()),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailPkColumn)));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailIds))
                    .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id())));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Map<Long, Contact> contactMap = new HashMap<>();

            if (!dataContainer.isEmpty()) {
                for (final Long contactEmailId : contactEmailIds) {
                    final Row row = dataContainer.getRow(contactEmailTable, new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailId)));

                    if (Objects.nonNull(row)) {
                        final Contact contact = convertRowToPartialModel(dataContainer.getRow(contactTable, (Long) row.get(MCONEMAILFIELDS.ENTITY_ID)));
                        final Email email = new Email();

                        email.setId(contactEmailId);
                        email.setValue(dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailPkColumn,
                                row.get(MCONEMAILFIELDS.EMAIL_ID)))).get(OMETEMAIL.VALUE));
                        contact.setPrimaryEmail(email);

                        contactMap.put(contactEmailId, contact);
                    }
                }
            }

            return dataContainer.isEmpty() ? Collections.emptyMap() : contactMap;
        }

        return Collections.emptyMap();
    }

    /**
     * <p>
     * Fetches the contacts email ids that are mapped with given email addresses
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param emails Represents the email addresses
     * @return {@link Map} of email address and contact email id
     */
    public Map<String, Long> getContactEmailIds(final long orgId, final Collection<String> emails) {
        final Table emailTable = emailManager.getTable();
        final Table contactEmailTable = getContactEmailTable();
        final SelectQuery selectQuery = SelectQuery.get(emailTable);
        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Map<String, Long> emailContactIds = new HashMap<>();
        final String emailField = ContactFields.EMAILS;
        final Long emailFieldId = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id();

        selectQuery.addSelectClause(emailValueColumn);
        selectQuery.addJoinClause(new JoinClause(emailTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailValueColumn, emails)).
                and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), emailFieldId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            for (final String email : emails) {
                final Row emailAddressRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailValueColumn, email)));

                if (Objects.nonNull(emailAddressRow)) {
                    emailContactIds.put(email, emailAddressRow.getPKValue());
                }
            }
        }

        return emailContactIds;
    }

    /**
     * <p>
     * Fetches a collection of {@link Contact} for the given owner ids
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerIds Which contains a collection of owner ids
     * @return The collection of {@link Contact}
     */
    public Collection<Contact> getContacts(final long orgId, final Collection<Long> ownerIds) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column ownerIdColumn = contactTable.getColumn(MCOCONTACT.OWNER_ID);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), ownerIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(ownerIdColumn, ownerIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }

        return dataContainer.getRows(contactTable).map(contactRow -> {
            final Contact contact = new Contact();

            contact.setOrgId(orgId);
            contact.setId(contactRow.getPKValue());
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
            return contact;
        }).toList();
    }

    /**
     * <p>
     * Gets the primary email mapped for the given contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Email}
     */
    public Optional<Email> getPrimaryEmail(final long orgId, final Long contactId) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)));

        return Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(emailTable)).map(this::convertEmailRowToModel);
    }

    /**
     * <p>
     * Gets the primary phone mapped for the given contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Phone}
     */
    public Optional<Phone> getPrimaryPhone(final long orgId, final Long contactId) {
        final Table contactTable = getTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(phoneTable.getColumn(OMETPHONE.NUMBER), phoneTable.getColumn(OMETPHONE.TYPE),
                phoneTable.getColumn(OMETPHONE.COUNTRY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)));

        return Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(phoneTable)).map(this::convertPhoneRowToModel);
    }

    /**
     * <p>
     * Retrieves the total contact count based on the dropdown criteria
     * </p>
     *
     * @param orgId           Represents the organization ID for which the contacts are counted.
     * @param isIncludedEmail If {@code true}, the count includes contacts that have any associated email.
     * @param criteria        The criteria to filter the contacts (e.g., based on first name, last name, email).
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final boolean isIncludedEmail, final DropdownCriteria criteria) {
        return getTotalCount(orgId, isIncludedEmail, false, criteria);
    }

    /**
     * <p>
     * Retrieves the total count of the contacts
     * </p>
     *
     * @param orgId             Represents the organization ID for which the contacts are counted.
     * @param isIncludedEmails  If {@code true}, the count includes contacts that have any associated email.
     * @param includePhones     If {@code true}, the count includes contacts that have any associated phone.
     * @param criteria          The criteria to filter the contacts (e.g., based on first name, last name, email, phone).
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final boolean isIncludedEmails, final boolean includePhones, final DropdownCriteria criteria) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final CountQuery contactCountQuery = CountQuery.get(contactTable);
        WhereClause whereClause = new WhereClause(criteria.build(firstNameColumn)).or(criteria.build(lastNameColumn))
                .or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim())));

        if (isIncludedEmails) {
            contactCountQuery.addJoinClause(List.of(new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
            whereClause = whereClause.or(criteria.build(emailTable.getColumn(OMETEMAIL.VALUE)));
        }

        if (includePhones) {
            final Table phoneTable = phoneManager.getTable();

            contactCountQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
            whereClause = whereClause.or(criteria.build(phoneTable.getColumn(OMETPHONE.NUMBER)));
        }
        contactCountQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(contactCountQuery);
    }

    /**
     * <p>
     * Retrieves the total count of the contacts
     * </p>
     *
     * @param orgId                Represents the org id
     * @param dropdownCriteria     Represents the dropdown criteria
     * @param includePhoneCriteria Represents the flag to indicate whether to include phone number criteria in where clause
     * @return the total count of the contacts
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria, final boolean includePhoneCriteria) {
        final Table contactTable = getTable();
        final CountQuery countQuery = CountQuery.get(contactTable);
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final WhereClause nameCriteria = new WhereClause(dropdownCriteria.build(firstNameColumn))
                .or(dropdownCriteria.build(lastNameColumn)).or(dropdownCriteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim())));
        final WhereClause whereClause;

        if (includePhoneCriteria) {
            final Table phoneTable = phoneManager.getTable();

            countQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
            whereClause = WhereClauseUtil.connect(nameCriteria, new WhereClause(dropdownCriteria.build(phoneTable.getColumn(OMETPHONE.NUMBER))), Connector.OR);
        } else {
            whereClause = nameCriteria;
        }
        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the total email count for an respective given contact id.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return the count of email for the respective contact
     */
    public long getEmailsCount(final long orgId, final Long contactId) {
        final Table contactTable = getTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final CountQuery countQuery = CountQuery.get(contactTable);
        final String emailField = ContactFields.EMAILS;
        final Column contactPkColumn = contactTable.getPKColumn();

        countQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId))
                .and(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID),
                        fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id()))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the {@link Contact} for the given contact email id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param contactEmailId Represents the contact email id
     * @return the contact
     */
    public Optional<Contact> getContactByEmailId(final long orgId, final Long contactEmailId) {
        final Table contactEmailTable = getContactEmailTable();
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final String emailField = ContactFields.EMAILS;
        final Column emailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(contactEmailTable);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.OWNER_ID), contactTable.getColumn(MCOCONTACT.ORG_ID), emailIdColumn,
                emailTable.getColumn(OMETEMAIL.VALUE)));
        selectQuery.addJoinClause(List.of(new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactTable.getPKColumn()),
                new JoinClause(emailIdColumn, emailTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailIdColumn, contactEmailId))
                .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField).get(emailField).id())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Row contactRow = dataContainer.getRow(contactTable);
            final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(), contactEmailId)));

            if (Objects.nonNull(contactRow) && Objects.nonNull(emailRow)) {
                final Contact contact = convertRowToPartialModel(contactRow);
                final Email email = new Email();

                email.setId(contactEmailId);
                email.setValue(emailRow.get(OMETEMAIL.VALUE));
                contact.setPrimaryEmail(email);

                return Optional.of(contact);
            }
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Fetches the {@link LifeCycleStatus} for the given contact id
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param contactId Represents the contact id
     * @return The {@link LifeCycleStatus}, wrapped in an {@link Optional}
     */
    public Optional<LifeCycleStatus> getContactLifeCycleStatus(final long orgId, final long contactId) {
        final Table table = getTable();
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
        final SelectQuery innerQuery = SelectQuery.get(table);
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);

        innerQuery.addSelectClause(table.getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID));
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), contactId)));

        selectQuery.addSelectClause(List.of(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL),
                lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORG_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifeCycleStatusTable.getPKColumn(), innerQuery)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(lifeCycleStatusTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.ofNullable(lifeCycleStatusManager.convertRowToModel(row));
    }

    /**
     * <p>
     * Fetches the life cycle stage for the given contact id
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param contactId Represents the contact id
     * @return The {@link LifeCycleStage}, wrapped in an {@link Optional
     */
    public Optional<LifeCycleStage> getContactLifeCycleStage(final long orgId, final long contactId) {
        final Table table = getTable();
        final Table lifeCycleStageTable = lifeCycleStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStageTable);

        selectQuery.addSelectClause(lifeCycleStageTable);
        selectQuery.addJoinClause(new JoinClause(lifeCycleStageTable.getPKColumn(), table.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), contactId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(lifeCycleStageTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.ofNullable(lifeCycleStageManager.convertRowToModel(row));
    }

    /**
     * <p>
     * Fetches the {@link LifeCycleStatus} for the given contact id, lifeCycle stage id
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param contactId        Represents the contact id
     * @param lifeCycleStageId Represents the lifecycle stage id
     * @return The {@link LifeCycleStatus}, wrapped in an {@link Optional}
     */
    public Optional<LifeCycleStatus> getContactLifeCycleStatus(final long orgId, final long contactId, final long lifeCycleStageId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();

        selectQuery.addJoinClause(new JoinClause(table.getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID), lifeCycleStatusTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), contactId))
                .and(Criteria.eq(table.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID), lifeCycleStageId)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(lifeCycleStatusTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.ofNullable(lifeCycleStatusManager.convertRowToModel(row));
    }

    /**
     * <p>
     * Fetches the names of web forms associated with a given collection of contact ids for a specified organization id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param contactIds Which contains the collection of contact ids
     * @return A map where the key is the contact id and the value is the name of the associated web form.
     */
    public Map<Long, String> getWebFormNames(final long orgId, final Collection<Long> contactIds) {
        final Table contactWebFormTable = Table.get(MWFCONTACTWEBFORM.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(contactWebFormTable);
        final Table webFormTable = webFormManager.getTable();
        final Column webFormContactIdColumn = contactWebFormTable.getColumn(MWFCONTACTWEBFORM.CONTACT_ID);
        final Column contactWebFormIdColumn = contactWebFormTable.getColumn(MWFCONTACTWEBFORM.WEB_FORM_ID);
        final Column webFormPKColumn = webFormTable.getPKColumn();

        selectQuery.addSelectClause(List.of(webFormTable.getColumn(OMWFWEBFORM.NAME), webFormContactIdColumn, contactWebFormIdColumn));
        selectQuery.addJoinClause(new JoinClause(contactWebFormIdColumn, webFormPKColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(webFormContactIdColumn, contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, String> contactWebFormNames = new HashMap<>();

        dataContainer.getRows(contactWebFormTable).forEach(contactRow -> {
            final Long contactId = contactRow.get(MWFCONTACTWEBFORM.CONTACT_ID);
            final Row webFormContactRow = dataContainer.getRow(contactWebFormTable, new WhereClause(Criteria.eq(webFormContactIdColumn, contactId)));

            if (Objects.nonNull(webFormContactRow)) {
                final Row webFormRow = dataContainer.getRow(webFormTable, new WhereClause(Criteria.eq(webFormPKColumn, webFormContactRow.get(MWFCONTACTWEBFORM.WEB_FORM_ID))));

                if (Objects.nonNull(webFormRow)) {
                    contactWebFormNames.put(contactId, webFormRow.get(OMWFWEBFORM.NAME));
                }
            }
        });
        return contactWebFormNames;
    }

    /**
     * <p>
     * Gets the contact preview details to be loaded in the contact list
     * </p>
     *
     * @param orgId      Represents the organization ID
     * @param contactIds Represents the collection of {@link Contact} ids
     * @return The {@link DataContainer}
     */
    public DataContainer getPreviewDetails(final Long orgId, final Collection<Long> contactIds) {
        final Table contactTable = getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getColumn(OMETEMAIL.TYPE), emailTable.getColumn(OMETEMAIL.VALUE),
                contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getColumn(OMETPHONE.COUNTRY_ID), phoneTable.getColumn(OMETPHONE.NUMBER)));
        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the contact life cycle timeline into container
     * </p>
     *
     * @param contact         Represents the {@link Contact}
     * @param targetContainer Holds the updated information of contact timeline field values
     */
    private void loadContactLifeCycleIntoTimeline(final Contact contact, final DataContainer targetContainer) {
        final Long orgId = contact.orgId();
        final String lifeCycleStageKey = ContactFields.LIFECYCLE_STAGE;
        final String lifeCycleStatusKey = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                List.of(lifeCycleStageKey, lifeCycleStatusKey, lostReasonKey));
        final Optional<Contact> existingContact = fetchContactsWithLifeCycleDetails(orgId, List.of(contact.id())).stream().findFirst();

        if (existingContact.isPresent()) {
            final Contact oldContact = existingContact.get();
            final LifeCycleStatus newlifeCycleStatus = contact.getLifeCycleStatus();
            final LifeCycleStage newlifeCycleStage = contact.getLifeCycleStage();
            final Option lostReason = contact.getLostReason();
            final LifeCycleStage oldLifeCycleStage = oldContact.getLifeCycleStage();
            final LifeCycleStatus oldLifeCycleStatus = oldContact.getLifeCycleStatus();

            if (Objects.nonNull(newlifeCycleStage) && Objects.nonNull(oldLifeCycleStage)) {
                final Long newLifeCycleStageId = newlifeCycleStage.id();

                if (!Objects.equals(oldLifeCycleStage.id(), newLifeCycleStageId)) {
                    final Timeline timeline = buildTimeline(contact);
                    final LifeCycleStage stage = lifeCycleStageManager.partialGet(orgId, newLifeCycleStageId);

                    timeline.setTimelineType(TimelineType.CONTACT_LIFE_CYCLE_STAGE_UPDATED);
                    loadFieldTimeline(timeline, fields.get(lifeCycleStageKey).i18nName(),
                            oldLifeCycleStage.getName(), stage.getName());
                    loadContactTimelineRowIntoContainer(timeline, contact.id(), targetContainer);
                }
            }

            if (Objects.nonNull(newlifeCycleStatus) && Objects.nonNull(oldLifeCycleStatus)) {
                final Long newLifeCycleStatusId = newlifeCycleStatus.id();

                if (!Objects.equals(oldLifeCycleStatus.id(), newLifeCycleStatusId)) {
                    final Timeline timeline = buildTimeline(contact);
                    final LifeCycleStatus status = lifeCycleStatusManager.partialGet(orgId, newLifeCycleStatusId);

                    timeline.setTimelineType(TimelineType.CONTACT_LIFE_CYCLE_STATUS_UPDATED);
                    loadFieldTimeline(timeline, fields.get(lifeCycleStageKey).i18nName(), oldLifeCycleStatus.getName(), status.getName());
                    loadContactTimelineRowIntoContainer(timeline, contact.id(), targetContainer);
                }
            }

            if (Objects.nonNull(lostReason)) {
                final Option oldLostReason = oldContact.getLostReason();
                final String oldLostReasonName = Objects.isNull(oldLostReason) ? null : oldLostReason.getOption();
                final FieldInfo lostReasonField = fields.get(lostReasonKey);
                final String lostReasonName = optionService.get(orgId, lostReasonField.id(), lostReason.id()).getOption();

                if (!Objects.equals(oldLostReasonName, lostReasonName)) {
                    final Timeline timeline = buildTimeline(contact);

                    if (Objects.isNull(oldLostReasonName)) {
                        timeline.setTimelineType(TimelineType.CONTACT_CLOSED_LOST_REASON_SET);
                        timeline.setAction(TimelineActionFields.SET);
                    } else {
                        timeline.setTimelineType(TimelineType.CONTACT_CLOSED_LOST_REASON_UPDATED);
                    }
                    loadFieldTimeline(timeline, lostReasonField.i18nName(), oldLostReasonName, lostReasonName);
                    loadContactTimelineRowIntoContainer(timeline, contact.id(), targetContainer);
                }
            }
        }
    }

    /**
     * <p>
     * Fetches the collection {@link Contact} with lifecycle details
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of contact ids
     * @return The collection of {@link Contact}
     */
    public Collection<Contact> fetchContactsWithLifeCycleDetails(final long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table lifeCycleStageTable = lifeCycleStageManager.getTable();
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
        final Table contactDropdownTable = getContactDropdownTable();

        selectQuery.addJoinClause(List.of(new JoinClause(table.getPKColumn(), contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(table.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID), lifeCycleStageTable.getPKColumn()),
                new JoinClause(table.getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID), lifeCycleStatusTable.getPKColumn())));

        selectQuery.addSelectClause(List.of(table.getColumn(MCOCONTACT.ORG_ID), lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME),
                lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME), table.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID),
                contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID),
                contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID),
                table.getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID), contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.OPTION_ID)));

        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        final Collection<Contact> contacts = new ArrayList<>();
        final FieldInfo lostReasonField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.CONTACT_LOST_REASON).orElseGet(() -> null);
        final Long lostReasonFieldId = lostReasonField.id();

        dataContainer.getRows(table).forEach(row -> {
            final Row lifeCycleStageRow = dataContainer.getRow(lifeCycleStageManager.getTable(),
                    (Long) row.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID));
            final Row lifeCycleStatusRow = dataContainer.getRow(lifeCycleStatusManager.getTable(),
                    (Long) row.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID));
            final Row optionRow = dataContainer.getRow(contactDropdownTable,
                    new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), row.getPKValue())).and(
                            Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), lostReasonFieldId)));
            final Contact contact = new Contact();

            if (Objects.nonNull(lifeCycleStageRow)) {
                final LifeCycleStage lifeCycleStage = new LifeCycleStage();

                lifeCycleStage.setId(lifeCycleStageRow.getPKValue());
                lifeCycleStage.setName(lifeCycleStageRow.get(OMCOLIFECYCLESTAGE.NAME));
                contact.setLifeCycleStage(lifeCycleStage);
            }

            if (Objects.nonNull(lifeCycleStatusRow)) {
                final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

                lifeCycleStatus.setId(lifeCycleStatusRow.getPKValue());
                lifeCycleStatus.setName(lifeCycleStatusRow.get(OMCOLIFECYCLESTATUS.NAME));
                contact.setLifeCycleStatus(lifeCycleStatus);
            }

            if (Objects.nonNull(optionRow)) {
                final Option lostReasonOption = optionService.get(orgId, lostReasonField.id(), optionRow.get(MCONDROPDOWNFIELDS.OPTION_ID));

                contact.setLostReason(lostReasonOption);
            }
            contact.setId(row.getPKValue());
            contacts.add(contact);
        });
        return contacts;
    }

    /**
     * <p>
     * Loads the field timeline into {@link Timeline}
     * </p>
     *
     * @param timeline  Represents the {@link Timeline}
     * @param fieldName Represents the field name
     * @param oldValue  Represents the old value
     * @param newValue  Represents the new value
     */
    private void loadFieldTimeline(final Timeline timeline, final I18nKey fieldName,
                                   final String oldValue, final String newValue) {

        if (!Objects.equals(oldValue, newValue)) {
            final FieldTimeline fieldTimeline = new FieldTimeline(Objects.isNull(oldValue)
                    ? Collections.emptyList() : List.of(oldValue), newValue, fieldName);

            timeline.setFieldTimeline(fieldTimeline);
        }
    }

    /**
     * <p>
     * Builds the {@link Timeline}
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return The  {@link Timeline}
     */
    private Timeline buildTimeline(final Contact contact) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(contact.orgId());
        timeline.setCategory(Timeline.Category.CONTACT);
        timeline.setAction(TimelineActionFields.UPDATED);
        timeline.setOwnerId(contact.getUpdatedBy().id());
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));

        return timeline;
    }

    /**
     * <p>
     * Gets the contact profile details
     * </p>
     *
     * @param orgId     Represents the organisation Id
     * @param contactId Represents the contact Id
     * @return The {@link Contact}
     */
    public Contact getContactProfile(final Long orgId, final Long contactId) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final String companyKey = ContactFields.COMPANY;
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey);
        final Table contactDisplayPicTable = Table.get(MCONDISPLAYPICTURE.TABLE);
        final Table displayPicTable = Table.get(OMDPDISPLAYPICTURE.TABLE);
        final Column displayPicPkColumn = displayPicTable.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), leftJoin),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactPkColumn, contactDisplayPicTable.getColumn(MCONDISPLAYPICTURE.CONTACT_ID), leftJoin),
                new JoinClause(contactDisplayPicTable.getColumn(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID), displayPicPkColumn, leftJoin)));
        selectQuery.addSelectClause(emailTable, phoneTable);
        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.ORG_ID), contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN),
                contactTable.getColumn(MCOCONTACT.TWITTER), contactTable.getColumn(MCOCONTACT.DESIGNATION), displayPicPkColumn,
                displayPicTable.getColumn(OMDPDISPLAYPICTURE.ORIGINAL_SIZE), displayPicTable.getColumn(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION),
                contactDisplayPicTable.getPKColumn(), contactDisplayPicTable.getColumn(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId)));

        if (fields.containsKey(companyKey)) {
            final Table companyTable = companyManager.getTable();
            final Table contactCompanyDerivedTable = companyManager.getCompanyFieldTable(fields.get(companyKey), getContactCompanyTable());

            selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
            selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.WEBSITE));
            selectQuery.addJoinClause(new JoinClause(contactPkColumn, contactCompanyDerivedTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin));
            selectQuery.addJoinClause(new JoinClause(contactCompanyDerivedTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin));
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToContactProfile(dataContainer.getRow(contactTable), dataContainer);
    }

    /**
     * Converts partial row into {@link Contact}
     *
     * @param contactRow Represents the contact row
     * @return The {@link Contact}
     */
    private Contact convertRowToContactProfile(final Row contactRow, final DataContainer dataContainer) {
        final Contact contact = new Contact();
        final Long orgId = contactRow.getOrgId();

        contact.setId(contactRow.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
        contact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
        contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
        contact.setDesignation(contactRow.get(MCOCONTACT.DESIGNATION));
        final Row emailRow = dataContainer.getRow(emailManager.getTable());

        if (Objects.nonNull(emailRow)) {
            contact.setPrimaryEmail(convertEmailRowToModel(emailRow));
        }
        final Row phoneRow = dataContainer.getRow(phoneManager.getTable());

        if (Objects.nonNull(phoneRow)) {
            contact.setPrimaryPhone(convertPhoneRowToModel(phoneRow));
        }
        final Row companyRow = dataContainer.getRow(companyManager.getTable());

        if (Objects.nonNull(companyRow)) {
            contact.setCompany(companyManager.convertRowToPartialModel(companyRow));
        }

        final Table contactDisplayPicTable = Table.get(MCONDISPLAYPICTURE.TABLE);
        final Table displayPicTable = Table.get(OMDPDISPLAYPICTURE.TABLE);

        Optional.ofNullable(dataContainer.getRow(contactDisplayPicTable)).ifPresent(row -> {
            final Row displayPicRow = dataContainer.getRow(displayPicTable, new WhereClause(Criteria.eq(displayPicTable.getPKColumn(), row.get(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID))));
            final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);

            displayPicture.setSize(displayPicRow.get(OMDPDISPLAYPICTURE.ORIGINAL_SIZE));
            displayPicture.setExtension(ProfilePicture.Extension.get((short) displayPicRow.get(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION)));

            contact.setOriginalPicture(displayPicture);
        });

        return contact;
    }

    /**
     * <p>
     * Gets the activity contact mapping table
     * </p>
     *
     * @return The activity contact mapping {@link Table}
     */
    @Override
    public Table getActivityEntityTable() {
        return Table.get(MACCONTACTACTIVITY.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Map} of {@link Contact} with their id
     * </p>
     *
     * @param contactIds Represents the {@link Collection} of contact ids
     * @param orgId      Represents an organization id
     * @return The {@link Map} contains contact IDs as keys and corresponding {@link Contact} as values
     */
    public Map<Long, Contact> getContactsForDealTimeline(final long orgId, final Collection<Long> contactIds) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, Contact> contacts = new HashMap<>();

        dataContainer.getRows(contactTable).forEach(contactRow -> {
            final Contact contact = new Contact();

            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

            contacts.put(contactRow.getPKValue(), contact);
        });

        return contacts;
    }

    /**
     * <p>
     * Gets the total number of contacts matching the given dropdown criteria.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param fieldInfo        Represents the {@link FieldInfo}
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return The total contact count
     */
    public long getContactDropdownCount(final Long orgId, final FieldInfo fieldInfo, final DropdownCriteria dropdownCriteria) {
        final Table contactTable = getTable();
        final CountQuery contactCountQuery = CountQuery.get(contactTable);
        final Column contactPKColumn = contactTable.getPKColumn();

        if (Objects.nonNull(fieldInfo)) {
            final Long fieldId = fieldInfo.id();
            final String aliasName = fieldInfo.aliasName();

            switch (aliasName) {
                case "firstName", "lastName" ->
                        contactCountQuery.setWhereClause(new WhereClause(dropdownCriteria.build
                                (Objects.equals(aliasName, ContactFields.FIRST_NAME)
                                        ? contactTable.getColumn(MCOCONTACT.FIRST_NAME)
                                        : contactTable.getColumn(MCOCONTACT.LAST_NAME))));
                case "emails" -> {
                    final Table contactEmailTable = getContactEmailTable();
                    final Table emailTable = emailManager.getTable();

                    contactCountQuery.addJoinClause(List.of(new JoinClause(contactPKColumn,
                                    contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                            new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
                    contactCountQuery.setWhereClause(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId))
                            .and(dropdownCriteria.build(emailTable.getColumn(OMETEMAIL.VALUE))));
                }
                case "phones" -> {
                    final Table contactPhoneTable = getContactPhoneTable();
                    final Table phoneTable = phoneManager.getTable();

                    contactCountQuery.addJoinClause(List.of(new JoinClause(contactPKColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)),
                            new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn())));
                    contactCountQuery.setWhereClause(new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId))
                            .and(dropdownCriteria.build(phoneTable.getColumn(OMETPHONE.NUMBER))));
                }
            }
        }

        return orgDataStore(orgId).get(contactCountQuery);
    }

    /**
     * <p>
     * Gets the collection of {@link Contact}
     * </p>
     *
     * @param orgId     Represents an org id
     * @param fieldInfo Represents the {@link FieldInfo}
     * @param limit     Represents the {@link DropdownLimit}
     * @param criteria  Represents the {@link DropdownCriteria}
     * @return The collection of {@link Contact}
     */
    public Collection<Contact> getContactDropdownList(final Long orgId, final FieldInfo fieldInfo,
                                                      final DropdownLimit limit, final DropdownCriteria criteria) {
        return getContactDropdownList(orgId, fieldInfo, limit, false, criteria);
    }

    /**
     * <p>
     * Fetches the {@link Contact} for the dropdown actions.
     * </p>
     *
     * @param orgId                 Represents an org id
     * @param fieldInfo             Represents the {@link FieldInfo}
     * @param limit                 Represents the {@link DropdownLimit}
     * @param includeCompanyAddress Whether to include the contact company address in the result
     * @param criteria              Represents the {@link DropdownCriteria}
     * @return The collection of {@link Contact}
     */
    public Collection<Contact> getContactDropdownList(final Long orgId, final FieldInfo fieldInfo,
                                                      final DropdownLimit limit, final boolean includeCompanyAddress,
                                                      final DropdownCriteria criteria) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Table phoneTable = phoneManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column firstName = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastName = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final Column primaryPhoneColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE);
        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Column phoneNumberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);
        final Column phonePkColumn = phoneTable.getPKColumn();
        final Column emailPkColumn = emailTable.getPKColumn();
        final String aliasName = fieldInfo.aliasName();
        final Long fieldId = fieldInfo.id();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = getContactCompanyTable();

        selectQuery.addJoinClause(List.of(
                new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.NAME)));
        selectQuery.addSelectClause(contactCompanyTable);

        if (includeCompanyAddress) {
            addCompanyAddressJoins(selectQuery, contactCompanyTable);
        }

        switch (aliasName) {
            case "firstName", "lastName" -> {
                selectQuery.setWhereClause(new WhereClause(criteria.build(Objects.equals(aliasName,
                        ContactFields.FIRST_NAME) ? contactTable.getColumn(MCOCONTACT.FIRST_NAME)
                        : contactTable.getColumn(MCOCONTACT.LAST_NAME))));
                selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                        emailPkColumn, JoinClause.JoinType.LEFT), new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                        phonePkColumn, JoinClause.JoinType.LEFT)));
            }
            case "emails" -> {
                final Table contactEmailTable = getContactEmailTable();

                selectQuery.addSelectClause(contactEmailTable);
                selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                        new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailPkColumn),
                        new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phonePkColumn, JoinClause.JoinType.LEFT)));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldId))
                        .and(criteria.build(emailValueColumn)));
                selectQuery.addOrderByClause(new OrderByClause(emailPkColumn));
            }
            case "phones" -> {
                final Table contactPhoneTable = getContactPhoneTable();

                selectQuery.addSelectClause(contactPhoneTable);
                selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID)),
                        new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phonePkColumn),
                        new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailPkColumn, JoinClause.JoinType.LEFT)));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldId))
                        .and(criteria.build(phoneNumberColumn)));
                selectQuery.addOrderByClause(new OrderByClause(phonePkColumn));
            }
        }
        selectQuery.addSelectClause(List.of(firstName, lastName, primaryEmailColumn, primaryPhoneColumn, emailValueColumn, phoneNumberColumn));
        selectQuery.setLimitClause(limit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Contact> contactList = new ArrayList<>();

        switch (aliasName) {
            case "firstName", "lastName" -> dataContainer.getRows(contactTable).forEach(row -> {
                final Contact contact = convertRowToContact(orgId, row);

                addPrimaryEmail(contact, row, emailTable, dataContainer);
                addPrimaryPhone(contact, row, phoneTable, dataContainer);
                addCompanyToContact(contact, row, dataContainer, companyTable, contactCompanyTable, includeCompanyAddress);
                contactList.add(contact);
            });
            case "emails" -> {
                final Table contactEmailTable = getContactEmailTable();

                dataContainer.getRows(contactEmailTable).forEach(relRow -> {
                    final Long contactId = relRow.get(MCONEMAILFIELDS.ENTITY_ID);
                    final Long emailId = relRow.get(MCONEMAILFIELDS.EMAIL_ID);
                    final Row contactRow = dataContainer.getRow(contactTable, contactId);
                    final Row emailRow = dataContainer.getRow(emailTable, emailId);
                    final Contact contact = convertRowToContact(orgId, contactRow);

                    if (Objects.nonNull(emailRow)) {
                        final Email email = new Email();

                        email.setValue(emailRow.get(OMETEMAIL.VALUE));
                        contact.addEmail(email);
                    }
                    addPrimaryPhone(contact, contactRow, phoneTable, dataContainer);
                    addCompanyToContact(contact, contactRow, dataContainer, companyTable, contactCompanyTable, includeCompanyAddress);
                    contactList.add(contact);
                });
            }
            case "phones" -> {
                final Table contactPhoneTable = getContactPhoneTable();

                dataContainer.getRows(contactPhoneTable).forEach(relRow -> {
                    final Long contactId = relRow.get(MCONPHONEFIELDS.ENTITY_ID);
                    final Long phoneId = relRow.get(MCONPHONEFIELDS.PHONE_ID);
                    final Row contactRow = dataContainer.getRow(contactTable, contactId);
                    final Row phoneRow = dataContainer.getRow(phoneTable, phoneId);
                    final Contact contact = convertRowToContact(orgId, contactRow);

                    if (Objects.nonNull(phoneRow)) {
                        final Phone phone = new Phone();

                        phone.setNumber(phoneRow.get(phoneTable.getColumn(OMETPHONE.NUMBER)));
                        contact.addPhone(phone);
                    }
                    addPrimaryEmail(contact, contactRow, emailTable, dataContainer);
                    addCompanyToContact(contact, contactRow, dataContainer, companyTable, contactCompanyTable, includeCompanyAddress);
                    contactList.add(contact);
                });
            }
        }

        return contactList;
    }

    /**
     * <p>
     * Gets the lost reason option associated with a given contact.
     * </p>
     *
     * @param orgId     Represents an org  id
     * @param contactId Represents the contact id
     * @return @return an {@link Optional} containing the lost reason {@link Option} if found,
     * or an empty {@link Optional if no lost reason is associated
     */
    public Optional<Option> getContactLostReason(final long orgId, final long contactId) {
        final Optional<FieldInfo> lostReasonFieldInfo = secureFieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.CONTACT_LOST_REASON);

        if (lostReasonFieldInfo.isPresent()) {
            final Table table = getTable();
            final Table contactDropdownTable = getContactDropdownTable();
            final SelectQuery selectQuery = SelectQuery.get(table);
            final Column contactPKColumn = table.getPKColumn();
            final FieldInfo lostReasonField = lostReasonFieldInfo.get();

            selectQuery.addSelectClause(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.OPTION_ID));
            selectQuery.addJoinClause(new JoinClause(contactPKColumn,
                    contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPKColumn, contactId))
                    .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), lostReasonField.id())));
            final Row contactDropdownRow = orgDataStore(orgId).get(selectQuery).getRow(contactDropdownTable);

            return Optional.ofNullable(contactDropdownRow).map(row -> optionService.get(orgId, lostReasonField.id(), row.get(MCONDROPDOWNFIELDS.OPTION_ID)));
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Converts the contact row into {@link Contact}
     * </p>
     *
     * @param orgId      Represents an org id
     * @param contactRow Represents the contact row
     * @return The {@link Contact}
     */
    private Contact convertRowToContact(final Long orgId, final Row contactRow) {
        final Contact contact = new Contact();

        contact.setId(contactRow.getPKValue());
        contact.setOrgId(orgId);
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));

        return contact;
    }

    /**
     * <p>
     * Adds the primary email to the given contact
     * </p>
     *
     * @param contact       Represents the {@link Contact}
     * @param contactRow    Represents the contact row
     * @param emailTable    Represents the email table
     * @param dataContainer The data container used to fetch the email row based on the primary email ID
     */
    private void addPrimaryEmail(final Contact contact, final Row contactRow, final Table emailTable, final DataContainer dataContainer) {
        final Long primaryEmailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);

        if (Objects.nonNull(primaryEmailId)) {
            final Row emailRow = dataContainer.getRow(emailTable, primaryEmailId);

            if (Objects.nonNull(emailRow)) {
                final Email email = new Email();

                email.setValue(emailRow.get(OMETEMAIL.VALUE));
                contact.addEmail(email);
            }
        }
    }

    /**
     * <p>
     * Adds the primary phone number to the given contact
     * </p>
     *
     * @param contact       Represents the {@link Contact}
     * @param contactRow    Represents the contact row
     * @param phoneTable    Represents the email table
     * @param dataContainer The data container used to fetch the email row based on the primary email ID
     */
    private void addPrimaryPhone(final Contact contact, final Row contactRow, final Table phoneTable,
                                 final DataContainer dataContainer) {
        final Long primaryPhoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

        if (Objects.nonNull(primaryPhoneId)) {
            final Row phoneRow = dataContainer.getRow(phoneTable, primaryPhoneId);

            if (Objects.nonNull(phoneRow)) {
                final Phone phone = new Phone();

                phone.setNumber(phoneRow.get(phoneTable.getColumn(OMETPHONE.NUMBER)));
                contact.addPhone(phone);
            }
        }
    }


    /**
     * <p>
     * Gets the contact profile details
     * </p>
     *
     * @param orgId     Represents the organisation Id
     * @param contactId Represents the contact Id
     * @return The {@link Contact}
     */
    public Contact getSocialProfile(final Long orgId, final Long contactId) {
        final Table contactTable = getTable();

        final Row contactRow = orgDataStore(orgId).get(contactTable, contactId, List.of(contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN),
                contactTable.getColumn(MCOCONTACT.TWITTER)));

        return convertSocialProfile(contactRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the org id
     * @param contactId Represents the contact id
     * @return the user ids
     */
    @Override
    public Collection<Long> getRestrictedIamUserIdsForEntity(Long orgId, Long contactId) {
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Table contactTable = getTable();
        final Column userTablePkColumn = userTable.getPKColumn();
        final Column iamUserIdColumn = userTable.getColumn(OMCRMUSER.IAM_USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Collection<Long> userIdsWithAccess = getUserIdsWithEntityAccess(orgId, contactId, contactTable, contactTable.getColumn(MCOCONTACT.TERRITORY_ID), SubCategory.Type.CONTACTS);

        userIdsWithAccess.addAll(sourceFollowersManager.getFollowerIdsByEntityId(orgId, contactId, Source.CONTACT));

        selectQuery.addSelectClause(List.of(userTablePkColumn, iamUserIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.notIn(userTablePkColumn, userIdsWithAccess)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(userRow -> (Long) userRow.get(OMCRMUSER.IAM_USER_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Converts partial row into {@link Contact}
     *
     * @param contactRow Represents the contact row
     * @return The {@link Contact}
     */
    private Contact convertSocialProfile(final Row contactRow) {
        final Contact contact = new Contact();
        final Long orgId = contactRow.getOrgId();

        contact.setId(contactRow.getPKValue());
        contact.setOrgId(orgId);
        contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
        contact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
        contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));

        return contact;
    }

    /**
     * <p>
     * Gets the total number of contact by applying the given criteria list.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param criteriaList Represents the collection of {@link FilterCriteria}
     * @return The total number of contact.
     */
    public Long getContactCount(final Long orgId, final Collection<FilterCriteria> criteriaList) {
        final Table contactTable = getTable();
        final CountQuery countQuery = CountQuery.get(contactTable);

        filterCriteriaHandler.applyCriteria(orgId, Provider.CONTACT_LIST, countQuery, criteriaList);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves a map of {@link Contact} instances from the provided {@link DataContainer}
     * for the given collection of email addresses.
     * </p>
     *
     * @param emailAddresses The collection of email address strings to resolve; returns empty map if null or empty
     * @param dataContainer  The {@link DataContainer} holding pre-fetched rows for email, contact-email mapping, and contact tables
     * @return a {@link Map} of email address strings.
     */
    public Map<String, Contact> getContacts(final Collection<String> emailAddresses, final DataContainer dataContainer) {
        final Map<String, Contact> contactMap = new HashMap<>();

        if (Objects.isNull(emailAddresses) || emailAddresses.isEmpty()) {
            return contactMap;
        }
        final Table contactEmailTable = getContactEmailTable();
        final Table emailTable = emailManager.getTable();
        final Table contactTable = getTable();

        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column contactEmailIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID);

        for (final String emailAddress : emailAddresses) {
            final Row contactEmailAddressRow = dataContainer.getRow(emailTable,
                    new WhereClause(Criteria.eq(emailValueColumn, emailAddress)));

            if (Objects.nonNull(contactEmailAddressRow)) {
                final Row contactEmailMappingRow = dataContainer.getRow(contactEmailTable,
                        new WhereClause(Criteria.eq(contactEmailIdColumn, contactEmailAddressRow.getPKValue())));

                if (Objects.nonNull(contactEmailMappingRow)) {
                    final Row contactRow = dataContainer.getRow(contactTable,
                            new WhereClause(Criteria.eq(contactPkColumn, contactEmailMappingRow.get(MCONEMAILFIELDS.ENTITY_ID))));

                    if (Objects.nonNull(contactRow)) {
                        final Contact contact = convertRowToPartialModel(contactRow);
                        final Email email = new Email();

                        email.setValue(emailAddress);
                        email.setId(contactEmailMappingRow.get(contactEmailIdColumn));
                        contact.setPrimaryEmail(email);
                        contactMap.put(emailAddress, contact);
                    }
                }
            }
        }

        return contactMap;
    }

    /**
     * Gets the collection of {@link Contact} given by the IDs.
     *
     * @param orgId      Represents the organization ID
     * @param contactIds Represents the collection of contact IDs.
     * @return the collection of {@link Contact}
     */
    public Collection<Contact> getContactsByIds(final Long orgId, final Set<Long> contactIds) {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(contactTable).map(contactRow -> {
            final Contact contact = new Contact();

            contact.setId(contactRow.getPKValue());
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

            return contact;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param entityId  Represents the entity id
     * @return The entity name
     */
    @Override
    public String getName(final Long orgId, final Long entityId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, entityId, List.of(table.getColumn(MCOCONTACT.FIRST_NAME), table.getColumn(MCOCONTACT.LAST_NAME)));

        if (Objects.isNull(row)) {
            return null;
        } else {
            return Contact.getFullName(row.get(MCOCONTACT.FIRST_NAME), row.get(MCOCONTACT.LAST_NAME));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     the organization ID
     * @param dealId    the deal ID to associate with
     * @param contactId the contact ID to associate
     */
    @Override
    public void associateEntityWithDeal(final long orgId, final long contactId, final long dealId) {
        if (isContactAssociatedWithDeal(orgId, dealId, contactId)) {
            return;
        }
        final Participant participant = new Participant();

        participant.setOrgId(orgId);
        participant.setDealId(dealId);
        participant.setContactId(contactId);

        ParticipantManager.getInstance().create(participant);
    }

    /**
     * <p>
     * Checks whether the given contact is associated with the specified deal
     * </p>
     *
     * @param orgId     the organization ID
     * @param dealId    the deal ID
     * @param contactId the contact ID to validate
     * @return {@code true} if the contact is associated with the deal, {@code false} otherwise
     */
    private boolean isContactAssociatedWithDeal(final long orgId, final long dealId, final long contactId) {
        final Table participantTable = Table.get(MDOPARTICIPANT.TABLE);
        final CountQuery participantCountQuery = CountQuery.get(participantTable);

        participantCountQuery.setWhereClause(new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId))
                .and(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactId)));

        return  orgDataStore(orgId).get(participantCountQuery) > 0;
    }

}