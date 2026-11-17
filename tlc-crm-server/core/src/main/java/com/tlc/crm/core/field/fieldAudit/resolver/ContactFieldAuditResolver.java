package com.tlc.crm.core.field.fieldAudit.resolver;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MCONDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MTCONTACTTAG;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTAGE;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTATUS;

import com.tlc.crm.core.sql.resource.OMKLMARKETINGLISTCONTACT;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.sql.resource.OMKLMARKETINGLIST;
import com.tlc.crm.entity.sql.resource.OMTTAG;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.l10n.temporal.model.Date;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles field edit history value loading, resolution, and comparison for {@link Contact} entities.
 * Extracts field-history-specific logic out of {@link ContactManager} for better separation of concerns.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ContactFieldAuditResolver extends AbstractFieldAuditResolver {

    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final CompanyManager companyManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final MarketingListContactManager marketingListContactManager;

    private ContactFieldAuditResolver() {
        this.emailManager = EmailManager.getInstance();
        this.phoneManager = PhoneManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.marketingListContactManager = MarketingListContactManager.getInstance();
    }

    /**
     * Gets the singleton instance of {@link ContactFieldAuditResolver}.
     *
     * @return the singleton instance of {@link ContactFieldAuditResolver}
     */
    public static ContactFieldAuditResolver getInstance() {
        return Instance.CONTACT_FIELD_HISTORY_RESOLVER;
    }

    /**
     * Using Initialization-on-demand holder idiom for lazy-loaded, thread-safe singleton
     */
    private static class Instance {
        private static final ContactFieldAuditResolver CONTACT_FIELD_HISTORY_RESOLVER = new ContactFieldAuditResolver();
    }

    /**
     * Gets the contact table
     */
    public Table getTable() {
        return Table.get(MCOCONTACT.TABLE);
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
     * <p>
     * Loads field edit history entries for the given contact update into the target container.
     * </p>
     */
    public void loadFieldAudit(final Contact contact, final Row oldRow, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Long orgId = contact.orgId();
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.CONTACT.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final Collection<FieldInfo> allFields = fieldService.getFieldsBySource(orgId, Contact.ENTITY_TYPE);
        final Map<String, String> oldContactValues = new HashMap<>();
        final Map<String, String> newContactValues = buildNewContactSystemFieldValues(contact, config);
        final Map<String, String> oldCustomValues = FieldAuditHandler.extractFieldValues(contact.getExistingValueHolder(), allFields, config);
        final Map<String, String> newCustomValues = FieldAuditHandler.extractFieldValues(contact.getValueHolder(), allFields, config);

        if (Objects.nonNull(oldRow)) {
            loadOldSystemFieldValues(config, oldRow, oldContactValues, sourceContainer);
        }
        FieldAuditHandler.resolveNewCustomFieldValues(config, contact.getValueHolder(), allFields, newCustomValues,
                (field, rawValue) -> resolveCustomFieldValue(orgId, field, rawValue, field.storeType()));
        oldCustomValues.forEach(oldContactValues::putIfAbsent);
        newCustomValues.forEach(newContactValues::putIfAbsent);

        fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.CONTACT, contact.id(),
                contact.getUpdatedBy().id(), contact.getFieldAuditInfo(), allFields, oldContactValues, newContactValues, targetContainer);
    }

    /**
     * <p>
     * Loads old system field values from the existing contact row for field edit history comparison.
     * </p>
     */
    private void loadOldSystemFieldValues(final FieldTrackingConfig config, final Row oldRow, final Map<String, String> oldValues, final DataContainer sourceContainer) {
        final String firstNameField = ContactFields.FIRST_NAME;
        final String lastNameField = ContactFields.LAST_NAME;
        final String designationField = ContactFields.DESIGNATION;
        final String departmentField = ContactFields.DEPARTMENT;
        final String facebookField = ContactFields.FACEBOOK;
        final String twitterField = ContactFields.TWITTER;
        final String linkedinField = ContactFields.LINKEDIN;
        final String otherUnsubscribeReasonField = ContactFields.OTHER_UNSUBSCRIBE_REASON;
        final String dateOfBirthField = ContactFields.DATE_OF_BIRTH;
        final String salesOwnerField = ContactFields.SALES_OWNER;
        final String lifecycleStageField = ContactFields.LIFECYCLE_STAGE;
        final String lifecycleStatusField = ContactFields.LIFECYCLE_STATUS;
        final String territoryField = ContactFields.TERRITORY;

        putIfTrackEnabled(config, oldRow, oldValues, firstNameField, MCOCONTACT.FIRST_NAME);
        putIfTrackEnabled(config, oldRow, oldValues, lastNameField, MCOCONTACT.LAST_NAME);
        putIfTrackEnabled(config, oldRow, oldValues, designationField, MCOCONTACT.DESIGNATION);
        putIfTrackEnabled(config, oldRow, oldValues, departmentField, MCOCONTACT.DEPARTMENT);
        putIfTrackEnabled(config, oldRow, oldValues, facebookField, MCOCONTACT.FACEBOOK);
        putIfTrackEnabled(config, oldRow, oldValues, twitterField, MCOCONTACT.TWITTER);
        putIfTrackEnabled(config, oldRow, oldValues, linkedinField, MCOCONTACT.LINKEDIN);
        putIfTrackEnabled(config, oldRow, oldValues, otherUnsubscribeReasonField, MCOCONTACT.OTHER_UNSUBSCRIBE_REASON);
        final Long contactId = oldRow.getPKValue();
        final Long orgId = oldRow.getOrgId();

        if (config.isFieldTracked(dateOfBirthField)) {
            final Date dateOfBirth = oldRow.get(MCOCONTACT.DATE_OF_BIRTH);

            putIfNonNull(oldValues, dateOfBirthField, Objects.isNull(dateOfBirth) ? null : dateOfBirth.getTimeStamp());
        }

        if (config.isFieldTracked(salesOwnerField)) {
            final Long ownerId = oldRow.get(MCOCONTACT.OWNER_ID);

            if (Objects.nonNull(ownerId)) {
                oldValues.putIfAbsent(salesOwnerField, userService.getUser(orgId, ownerId).getFullName());
            }
        }

        if (config.isFieldTracked(lifecycleStageField)) {
            final Long oldLifeCycleStageId = oldRow.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);

            if (Objects.nonNull(oldLifeCycleStageId)) {
                final Row stageRow = sourceContainer.getRow(lifeCycleStageManager.getTable(), oldLifeCycleStageId);

                putIfNonNull(oldValues, lifecycleStageField, Objects.isNull(stageRow) ? null : stageRow.get(OMCOLIFECYCLESTAGE.NAME));
            }
        }

        if (config.isFieldTracked(lifecycleStatusField)) {
            final Long oldLifeCycleStatusId = oldRow.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID);

            if (Objects.nonNull(oldLifeCycleStatusId)) {
                final Row statusRow = sourceContainer.getRow(lifeCycleStatusManager.getTable(), oldLifeCycleStatusId);

                putIfNonNull(oldValues, lifecycleStatusField, Objects.isNull(statusRow) ? null : statusRow.get(OMCOLIFECYCLESTATUS.NAME));
            }
        }

        if (config.isFieldTracked(territoryField)) {
            final Long territoryId = oldRow.get(MCOCONTACT.TERRITORY_ID);

            if (Objects.nonNull(territoryId)) {
                final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
                final Row territoryRow = sourceContainer.getRow(territoryTable, new WhereClause(Criteria.eq(territoryTable.getPKColumn(), territoryId)));

                putIfNonNull(oldValues, territoryField, Objects.isNull(territoryRow) ? null : territoryRow.get(OMCRMTERRITORY.NAME));
            }
        }
        final String emailsField = ContactFields.EMAILS;
        final String phonesField = ContactFields.PHONES;
        final String companyField = ContactFields.COMPANY;
        final String addressField = ContactFields.ADDRESS;
        final String tagsField = ContactFields.TAGS;
        final String sourceField = ContactFields.SOURCE;
        final String subscriptionStatusField = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesField = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonField = ContactFields.UNSUBSCRIBE_REASON;
        final String lostReasonField = ContactFields.CONTACT_LOST_REASON;
        final String timezoneField = ContactFields.TIMEZONE;
        final String marketingList = ContactFields.MARKETING_LIST;
        final List<String> trackedComplexFields = new ArrayList<>();

        addFieldIfTrackEnabled(config, emailsField, trackedComplexFields);
        addFieldIfTrackEnabled(config, phonesField, trackedComplexFields);
        addFieldIfTrackEnabled(config, companyField, trackedComplexFields);
        addFieldIfTrackEnabled(config, addressField, trackedComplexFields);
        addFieldIfTrackEnabled(config, tagsField, trackedComplexFields);
        addFieldIfTrackEnabled(config, sourceField, trackedComplexFields);
        addFieldIfTrackEnabled(config, subscriptionStatusField, trackedComplexFields);
        addFieldIfTrackEnabled(config, subscriptionTypesField, trackedComplexFields);
        addFieldIfTrackEnabled(config, unsubscribeReasonField, trackedComplexFields);
        addFieldIfTrackEnabled(config, lostReasonField, trackedComplexFields);
        addFieldIfTrackEnabled(config, timezoneField, trackedComplexFields);
        addFieldIfTrackEnabled(config, marketingList, trackedComplexFields);

        if (trackedComplexFields.isEmpty()) {
            return;
        }
        final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE, trackedComplexFields);

        if (config.isFieldTracked(emailsField) && fieldInfoMap.containsKey(emailsField)) {
            final Table contactEmailTable = getContactEmailTable();
            final Table emailTable = emailManager.getTable();
            final List<Long> emailIds = sourceContainer.getRows(contactEmailTable, new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), fieldInfoMap.get(emailsField).id()))).map(row -> (Long) row.get(MCONEMAILFIELDS.EMAIL_ID)).toList();

            if (!emailIds.isEmpty()) {
                final String emailValues = sourceContainer.getRows(emailTable, new WhereClause(Criteria.in(emailTable.getPKColumn(), emailIds)))
                        .filter(Objects::nonNull).map(row -> (String) row.get(OMETEMAIL.VALUE)).collect(Collectors.joining(","));

                putIfNonNull(oldValues, emailsField, emailValues.isEmpty() ? null : emailValues);
            }
        }

        if (config.isFieldTracked(phonesField) && fieldInfoMap.containsKey(phonesField)) {
            final Table contactPhoneTable = getContactPhoneTable();
            final Table phoneTable = phoneManager.getTable();
            final List<Long> phoneIds = sourceContainer.getRows(contactPhoneTable, new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), fieldInfoMap.get(phonesField).id())))
                    .map(row -> (Long) row.get(MCONPHONEFIELDS.PHONE_ID)).toList();

            if (!phoneIds.isEmpty()) {
                final String phoneValues = sourceContainer.getRows(phoneTable, new WhereClause(Criteria.in(phoneTable.getPKColumn(), phoneIds)))
                        .filter(Objects::nonNull).map(row -> phoneManager.convertRowToModel(row).getNumberWithCode()).collect(Collectors.joining(","));

                putIfNonNull(oldValues, phonesField, phoneValues.isEmpty() ? null : phoneValues);
            }
        }

        if (config.isFieldTracked(companyField) && fieldInfoMap.containsKey(companyField)) {
            final Table contactCompanyTable = getContactCompanyTable();
            final Row contactCompanyRow = sourceContainer.getRow(contactCompanyTable, new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), fieldInfoMap.get(companyField).id())));

            if (Objects.nonNull(contactCompanyRow)) {
                final Table companyTable = companyManager.getTable();
                final Row companyRow = sourceContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(), contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID))));

                putIfNonNull(oldValues, companyField, Objects.isNull(companyRow) ? null : companyRow.get(MCOCOMPANY.NAME));
            }
        }

        if (config.isFieldTracked(addressField) && fieldInfoMap.containsKey(addressField)) {
            final Table contactAddressTable = getContactAddressTable();
            final Row contactAddressRow = sourceContainer.getRow(contactAddressTable, new WhereClause(Criteria.eq(contactAddressTable.getColumn(MCONADDRESSFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactAddressTable.getColumn(MCONADDRESSFIELDS.FIELD_ID), fieldInfoMap.get(addressField).id())));

            if (Objects.nonNull(contactAddressRow)) {
                final Table addressTable = addressManager.getTable();
                final Row addressRow = sourceContainer.getRow(addressTable, new WhereClause(Criteria.eq(addressTable.getPKColumn(),
                        contactAddressRow.get(MCONADDRESSFIELDS.ADDRESS_ID))));

                putIfNonNull(oldValues, addressField, Objects.isNull(addressRow) ? null : addressManager.convertRowToModel(addressRow).toString());
            }
        }

        if (config.isFieldTracked(tagsField) && fieldInfoMap.containsKey(tagsField)) {
            final Table contactTagTable = SourceTagManager.getInstance().getContactTagTable();
            final Table tagTable = SourceTagManager.getInstance().getTable();
            final List<Long> tagIds = sourceContainer.getRows(contactTagTable, new WhereClause(Criteria.eq(contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID), contactId)))
                    .filter(Objects::nonNull).sorted(Comparator.comparing(row -> row.get(MTCONTACTTAG.TAG_ID)))
                    .map(row -> (Long) row.get(MTCONTACTTAG.TAG_ID)).toList();

            if (!tagIds.isEmpty()) {
                final Map<Long, String> tagNameMap = sourceContainer.getRows(tagTable, new WhereClause(Criteria.in(tagTable.getPKColumn(), tagIds)))
                        .filter(Objects::nonNull).collect(Collectors.toMap(row -> row.getPKValue(), row -> row.get(OMTTAG.NAME)));
                final String tagNames = tagIds.stream().map(tagNameMap::get).filter(Objects::nonNull).collect(Collectors.joining(","));

                putIfNonNull(oldValues, tagsField, tagNames.isEmpty() ? null : tagNames);
            }
        }

        if (config.isFieldTracked(marketingList) && fieldInfoMap.containsKey(marketingList)) {
            final Table marketingListContactTable = marketingListContactManager.getMarketingListContactTable();
            final Table marketingListTable = marketingListContactManager.getMarketingListTable();
            final List<Long> marketingListIds = sourceContainer.getRows(marketingListContactTable, new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId)))
                    .filter(Objects::nonNull).sorted(Comparator.comparing(Row::getPKValue))
                    .map(row -> (Long) row.get(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID)).toList();

            if (!marketingListIds.isEmpty()) {
                putIfNonNull(oldValues, marketingList, sourceContainer.getRows(marketingListTable, new WhereClause(Criteria.in(marketingListTable.getColumn(OMKLMARKETINGLIST.ID), marketingListIds)))
                        .filter(Objects::nonNull).map(row -> (String) row.get(OMKLMARKETINGLIST.NAME)).collect(Collectors.joining(",")));
            }
        }
        final Table contactDropdownTable = getContactDropdownTable();

        getSystemOptionFieldOldValue(oldValues, sourceContainer, contactDropdownTable, contactId, sourceField, fieldInfoMap);
        getSystemOptionFieldOldValue(oldValues, sourceContainer, contactDropdownTable, contactId, subscriptionStatusField, fieldInfoMap);
        getSystemOptionFieldOldValue(oldValues, sourceContainer, contactDropdownTable, contactId, unsubscribeReasonField, fieldInfoMap);
        getSystemOptionFieldOldValue(oldValues, sourceContainer, contactDropdownTable, contactId, lostReasonField, fieldInfoMap);
        getSystemOptionFieldOldValue(oldValues, sourceContainer, contactDropdownTable, contactId, timezoneField, fieldInfoMap);

        if (config.isFieldTracked(subscriptionTypesField) && fieldInfoMap.containsKey(subscriptionTypesField)) {
            final Long subscriptionTypesFieldId = fieldInfoMap.get(subscriptionTypesField).id();
            final List<Row> subscriptionTypeRows = sourceContainer.getRows(contactDropdownTable,
                    new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), subscriptionTypesFieldId))).toList();

            if (!subscriptionTypeRows.isEmpty()) {
                final String subscriptionTypeValues = subscriptionTypeRows.stream()
                        .map(row -> optionService.get(orgId, subscriptionTypesFieldId, row.get(MCONDROPDOWNFIELDS.OPTION_ID)).getOption())
                        .collect(Collectors.joining(","));

                putIfNonNull(oldValues, subscriptionTypesField, subscriptionTypeValues.isEmpty() ? null : subscriptionTypeValues);
            }
        }
    }

    /**
     * <p>
     * Builds a map of current contact system field values for field edit history comparison.
     * </p>
     */
    public Map<String, String> buildNewContactSystemFieldValues(final Contact contact, final FieldTrackingConfig config) {
        final Long orgId = contact.orgId();
        final Map<String, String> newValues = new HashMap<>();
        final CrmUser salesOwner = contact.getSalesOwner();
        final Territory territory = contact.getTerritory();
        final Address address = contact.getAddress();
        final Collection<Email> emails = contact.getEmails();
        final Collection<Phone> phones = contact.getPhones();
        final Company company = contact.getCompany();
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final Option lostReason = contact.getLostReason();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final Option contactSource = contact.getContactSource();
        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final Date dateOfBirth = contact.getDateOfBirth();
        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
        final Option timeZone = contact.getTimeZone();
        final String salesOwnerKey = ContactFields.SALES_OWNER;
        final String lifeCycleStageKey = ContactFields.LIFECYCLE_STAGE;
        final String lifeCycleStatusKey = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
        final String sourceKey = ContactFields.SOURCE;
        final String territoryKey = ContactFields.TERRITORY;
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonKey = ContactFields.UNSUBSCRIBE_REASON;
        final String companyKey = ContactFields.COMPANY;
        final String tagsKey = ContactFields.TAGS;
        final String timezoneKey = ContactFields.TIMEZONE;
        final String marketingListKey = ContactFields.MARKETING_LIST;

        putIfNonNull(newValues, ContactFields.FIRST_NAME, contact.getFirstName());
        putIfNonNull(newValues, ContactFields.LAST_NAME, contact.getLastName());
        putIfNonNull(newValues, ContactFields.EMAILS, Objects.isNull(emails) ? null : emails.stream().map(Email::getValue).collect(Collectors.joining(",")));
        putIfNonNull(newValues, ContactFields.PHONES, Objects.isNull(phones) ? null : phones.stream().map(Phone::getNumberWithCode).collect(Collectors.joining(",")));
        putIfNonNull(newValues, ContactFields.DESIGNATION, contact.getDesignation());
        putIfNonNull(newValues, ContactFields.DATE_OF_BIRTH, Objects.isNull(dateOfBirth) ? null : String.valueOf(dateOfBirth.getTimeStamp()));
        putIfNonNull(newValues, ContactFields.DEPARTMENT, contact.getDepartment());
        putIfNonNull(newValues, ContactFields.OTHER_UNSUBSCRIBE_REASON, contact.getOtherUnsubscribeReason());
        putIfNonNull(newValues, ContactFields.ADDRESS, address);
        putIfNonNull(newValues, ContactFields.LINKEDIN, contact.getLinkedIn());
        putIfNonNull(newValues, ContactFields.TWITTER, contact.getTwitter());
        putIfNonNull(newValues, ContactFields.FACEBOOK, contact.getFacebook());
        final SummaryInfo summaryInfo = contact.getSummaryInfo();

        if (Objects.nonNull(summaryInfo) && config.isFieldTracked(tagsKey)) {
            putIfNonNull(newValues, tagsKey, resolveTagNames(orgId, summaryInfo));
        }

        if (config.isFieldTracked(marketingListKey)) {
            final Collection<MarketingList> marketingLists = contact.getMarketingLists();

            putIfNonNull(newValues, marketingListKey, resolveMarketingListNames(orgId, marketingLists));
        }

        if (config.isFieldTracked(salesOwnerKey) && Objects.nonNull(salesOwner)) {
            final String fullName = salesOwner.getFullName();

            putIfNonNull(newValues, salesOwnerKey, Objects.isNull(fullName) ? userService.getUser(orgId, salesOwner.id()).getFullName() : fullName);
        }

        if (config.isFieldTracked(lifeCycleStageKey) && Objects.nonNull(lifeCycleStage)) {
            final String lifeCycleStageName = lifeCycleStage.getName();

            putIfNonNull(newValues, lifeCycleStageKey, Objects.isNull(lifeCycleStageName) ? lifeCycleStageManager.get(orgId, lifeCycleStage.id()).getName() : lifeCycleStageName);
        }

        if (config.isFieldTracked(lifeCycleStatusKey) && Objects.nonNull(lifeCycleStatus)) {
            final String lifeCycleStatusName = lifeCycleStatus.getName();

            putIfNonNull(newValues, lifeCycleStatusKey, Objects.isNull(lifeCycleStatusName) ? lifeCycleStatusManager.get(orgId, lifeCycleStatus.id()).getName() : lifeCycleStatusName);
        }

        if (config.isFieldTracked(lostReasonKey) && Objects.nonNull(lostReason)) {
            putIfNonNull(newValues, lostReasonKey, resolveOptionName(orgId, lostReason));
        }

        if (config.isFieldTracked(sourceKey) && Objects.nonNull(contactSource)) {
            putIfNonNull(newValues, sourceKey, resolveOptionName(orgId, contactSource));
        }

        if (config.isFieldTracked(timezoneKey) && Objects.nonNull(timeZone)) {
            putIfNonNull(newValues, timezoneKey, resolveOptionName(orgId, timeZone));
        }

        if (config.isFieldTracked(territoryKey) && Objects.nonNull(territory)) {
            final String territoryName = territory.getName();

            putIfNonNull(newValues, territoryKey, Objects.isNull(territoryName) ? resolveTerritoryName(orgId, territory.id()) : territoryName);
        }

        if (config.isFieldTracked(subscriptionStatusKey) && Objects.nonNull(subscriptionStatus)) {
            putIfNonNull(newValues, subscriptionStatusKey, resolveOptionName(orgId, subscriptionStatus));
        }

        if (config.isFieldTracked(subscriptionTypesKey) && Objects.nonNull(subscriptionTypes) && !subscriptionTypes.isEmpty()) {
            putIfNonNull(newValues, subscriptionTypesKey, subscriptionTypes.stream().map(opt -> resolveOptionName(orgId, opt))
                    .filter(Objects::nonNull).collect(Collectors.joining(",")));
        }

        if (config.isFieldTracked(unsubscribeReasonKey) && Objects.nonNull(unsubscribeReason)) {
            putIfNonNull(newValues, unsubscribeReasonKey, resolveOptionName(orgId, unsubscribeReason));
        }

        if (config.isFieldTracked(companyKey) && Objects.nonNull(company)) {
            final String companyName = company.getName();

            putIfNonNull(newValues, companyKey, Objects.isNull(companyName) ? companyManager.getName(orgId, company.id()) : companyName);
        }
        return newValues;
    }

    /**
     * <p>
     * Loads field edit history for contact unsubscription triggered via email.
     * Tracks changes to subscription_status, subscription_types, and unsubscribe_reason fields.
     * </p>
     *
     * @param orgId                  The organization id
     * @param contactId              The contact id
     * @param oldSubscriptionStatus  The old subscription status option name (nullable)
     * @param oldSubscriptionTypes   The old subscription types as comma-separated option names (nullable)
     * @param newUnsubscribeReason   The new unsubscribe reason option name
     * @param targetContainer        The target container to load history rows into
     */
    public void loadUnsubscribeFieldAuditEntry(final Long orgId, final Long contactId,
            final String oldSubscriptionStatus, final Collection<Option> oldSubscriptionTypes,
            final String newUnsubscribeReason, final DataContainer targetContainer) {
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.CONTACT.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonKey = ContactFields.UNSUBSCRIBE_REASON;
        final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE,
                List.of(subscriptionStatusKey, subscriptionTypesKey, unsubscribeReasonKey)).values();
        final Map<String, String> oldValues = new HashMap<>();
        final Map<String, String> newValues = new HashMap<>();

        if (config.isFieldTracked(subscriptionStatusKey)) {
            putIfNonNull(oldValues, subscriptionStatusKey, oldSubscriptionStatus);
            putIfNonNull(newValues, subscriptionStatusKey, Contact.SubscriptionStatus.UNSUBSCRIBED.getName());
        }

        if (config.isFieldTracked(subscriptionTypesKey)) {
            putIfNonNull(oldValues, subscriptionTypesKey, oldSubscriptionTypes.stream().map(Option::getOption).collect(Collectors.joining(",")));
            newValues.put(subscriptionTypesKey, null);
        }

        if (config.isFieldTracked(unsubscribeReasonKey)) {
            putIfNonNull(newValues, unsubscribeReasonKey, newUnsubscribeReason);
        }
        fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.CONTACT, contactId,
                null, new FieldAuditInfo(FieldAuditSource.CONTACT), fields, oldValues, newValues, targetContainer);
    }

    /**
     * Fetches the old value of a system option field from the database for field edit history comparison.
     */
    private void getSystemOptionFieldOldValue(final Map<String, String> oldValues, final DataContainer sourceContainer,
            final Table contactDropdownTable, final Long contactId, final String field, final Map<String, FieldInfo> fieldInfoMap) {

        if (fieldInfoMap.containsKey(field)) {
            final Long fieldId = fieldInfoMap.get(field).id();
            final Row dropdownRow = sourceContainer.getRow(contactDropdownTable,
                    new WhereClause(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldId)));

            if (Objects.nonNull(dropdownRow)) {
                final Long orgId = sourceContainer.getRow(getTable(), contactId).getOrgId();

                putIfNonNull(oldValues, field, optionService.get(orgId, fieldId, dropdownRow.get(MCONDROPDOWNFIELDS.OPTION_ID)).getOption());
            }
        }
    }

    /**
     * Resolves marketing list names, fetching names from the database for marketing lists that only have ids.
     */
    private String resolveMarketingListNames(final Long orgId, final Collection<MarketingList> marketingLists) {
        if (Objects.isNull(marketingLists) || marketingLists.isEmpty()) {
            return null;
        }
        final List<Long> marketingListIds = new ArrayList<>();
        final List<String> marketingListNames = new ArrayList<>();

        marketingLists.stream().sorted(Comparator.comparing(MarketingList::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(marketingList -> {
            final Long id = marketingList.id();
            final String name = marketingList.getName();

            if (Objects.nonNull(id) && Objects.isNull(name)) {
                marketingListIds.add(id);
            } else if (Objects.nonNull(name)) {
                marketingListNames.add(name);
            }
        });

        if (!marketingListIds.isEmpty()) {
            marketingListNames.addAll(marketingListContactManager.getNames(orgId, marketingListIds));
        }
        return marketingListNames.isEmpty() ? null : String.join(",", marketingListNames);
    }
}