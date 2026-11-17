package com.tlc.crm.core.workflow.internal.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.summary.SummaryFields;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.resolver.DateUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The converter that facilitates different ways to convert data from/to {@link Contact}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ContactWorkflowConverter extends EntityWorkflowConverter<Contact> {

    private final OptionService optionService;
    private final EmailConverter emailConverter;
    private final PhoneConverter phoneConverter;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;

    private ContactWorkflowConverter() {
        this.optionService = OptionService.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.phoneConverter = PhoneConverter.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.dateUtil = DateUtil.getInstance();
    }

    private static class Instance {
        private static final ContactWorkflowConverter INSTANCE = new ContactWorkflowConverter();
    }

    public static ContactWorkflowConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact Refers the model.
     * @return The converted json.
     */
    @Override
    public JsonObject convert(final Contact contact) {
        final JsonObject jsonObject = Json.object();
        final JsonArray emails = Json.array();
        final JsonArray phoneNumbers = Json.array();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final ValueHolder valueHolder = contact.getValueHolder();
        final String idKey = ContactFields.ID;
        final Company company = contact.getCompany();
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final Option lostReason = contact.getLostReason();
        final Option source = contact.getContactSource();
        final Address address = contact.getAddress();
        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final Option timeZone = contact.getTimeZone();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final Territory territory = contact.getTerritory();
        final Collection<MarketingList> marketingLists = contact.getMarketingLists();

        jsonObject.put(idKey, contact.id());
        jsonObject.put(ContactFields.ORG_ID, contact.orgId());
        jsonObject.put(fields.get(ContactFields.FIRST_NAME).idAsString(), contact.getFirstName());
        jsonObject.put(fields.get(ContactFields.LAST_NAME).idAsString(), contact.getLastName());

        if (Objects.nonNull(company)) {
            jsonObject.put(fields.get(ContactFields.COMPANY).idAsString(), company.id());
        }

        if (Objects.nonNull(lifeCycleStage)) {
            jsonObject.put(fields.get(ContactFields.LIFECYCLE_STAGE).idAsString(), lifeCycleStage.id());
        }

        if (Objects.nonNull(lifeCycleStatus)) {
            jsonObject.put(fields.get(ContactFields.LIFECYCLE_STATUS).idAsString(), lifeCycleStatus.id());
        }

        if (Objects.nonNull(lostReason)) {
            jsonObject.put(fields.get(ContactFields.CONTACT_LOST_REASON).idAsString(), lostReason.id());
        }

        if (Objects.nonNull(source)) {
            jsonObject.put(fields.get(ContactFields.SOURCE).idAsString(), source.id());
        }

        if (Objects.nonNull(address)) {
            jsonObject.put(fields.get(ContactFields.ADDRESS).idAsString(), addressConverter.convert(address));
        }

        if (Objects.nonNull(subscriptionStatus)) {
            jsonObject.put(fields.get(ContactFields.SUBSCRIPTION_STATUS).idAsString(), subscriptionStatus.id());
        }

        if (Objects.nonNull(subscriptionTypes)) {
            jsonObject.put(fields.get(ContactFields.SUBSCRIPTION_TYPES).idAsString(),
                    subscriptionTypes.stream().map(Option::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(unsubscribeReason)) {
            jsonObject.put(fields.get(ContactFields.UNSUBSCRIBE_REASON).idAsString(), unsubscribeReason.id());
        }

        if (Objects.nonNull(timeZone)) {
            jsonObject.put(fields.get(ContactFields.TIMEZONE).idAsString(), timeZone.id());
        }

        if (Objects.nonNull(createdBy)) {
            jsonObject.put(fields.get(ContactFields.CREATED_BY).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy)) {
            jsonObject.put(fields.get(ContactFields.UPDATED_BY).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory)) {
            jsonObject.put(fields.get(ContactFields.TERRITORY).idAsString(), territory.id());
        }
        jsonObject.put(fields.get(ContactFields.OTHER_UNSUBSCRIBE_REASON).idAsString(), contact.getOtherUnsubscribeReason());
        jsonObject.put(fields.get(ContactFields.SALES_OWNER).idAsString(), contact.getSalesOwner().id());
        jsonObject.put(fields.get(ContactFields.DESIGNATION).idAsString(), contact.getDesignation());
        jsonObject.put(fields.get(ContactFields.DEPARTMENT).idAsString(), contact.getDepartment());
        jsonObject.put(fields.get(ContactFields.FACEBOOK).idAsString(), contact.getFacebook());
        jsonObject.put(fields.get(ContactFields.TWITTER).idAsString(), contact.getTwitter());
        jsonObject.put(fields.get(ContactFields.LINKEDIN).idAsString(), contact.getLinkedIn());
        jsonObject.put(fields.get(ContactFields.DATE_OF_BIRTH).idAsString(), dateUtil.getTimeStamp(contact.getDateOfBirth()));
        jsonObject.put(ContactFields.CREATED_TIME, dateTimeUtil.getTimeStamp(contact.getCreatedTime()));
        jsonObject.put(ContactFields.UPDATED_TIME, dateTimeUtil.getTimeStamp(contact.getModifiedTime()));
        jsonObject.put(CompanyFields.RECENT_NOTE, contact.getRecentNote());

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject);
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {

            for (final Email email : contactEmails) {
                final JsonObject emailJson = emailConverter.convertEmail(email);

                if (Objects.equals(contact.getPrimaryEmail(), email)) {
                    emailJson.put(ContactFields.IS_PRIMARY, true);
                }
                emails.put(emailJson);
            }
            jsonObject.put(fields.get(ContactFields.EMAILS).idAsString(), emails);
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = phoneConverter.convertPhone(phone);

                if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                    phoneJson.put(ContactFields.IS_PRIMARY, true);
                }
                phoneNumbers.put(phoneJson);
            }
            jsonObject.put(fields.get(ContactFields.PHONES).idAsString(), phoneNumbers);
        }
        final JsonObject summaryJson = summaryInfoConverter.convert(contact.getSummaryInfo());

        if (summaryJson.size() > 0) {
            jsonObject.put(SummaryFields.SUMMARY, summaryJson);
        }

        if (Objects.nonNull(marketingLists) && !marketingLists.isEmpty()) {
            jsonObject.put(fields.get(ContactFields.MARKETING_LIST).idAsString(),
                    marketingLists.stream().map(MarketingList::id).collect(Collectors.toSet()));
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object.
     * @return The {@link Contact}.
     */
    @Override
    public Contact convert(final JsonObject jsonObject) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final String createdByKey = ContactFields.CREATED_BY;
        final Long orgId = jsonObject.getLong(ContactFields.ORG_ID);
        final Long ownerId = jsonObject.getLong(fields.get(createdByKey).idAsString());
        final ConverterConfig converterConfig = new ConverterConfig(orgId, ownerId, null, false, jsonObject);
        final Contact contact = new Contact();
        final String timeZoneKey = ContactFields.TIMEZONE;

        final String firstNameKey = fields.get(ContactFields.FIRST_NAME).idAsString();
        final String lastNameKey = fields.get(ContactFields.LAST_NAME).idAsString();
        final String emailKey = fields.get(ContactFields.EMAILS).idAsString();
        final String phoneKey = fields.get(ContactFields.PHONES).idAsString();
        final String dobKey = fields.get(ContactFields.DATE_OF_BIRTH).idAsString();
        final FieldInfo subscriptionStatusField = fields.get(ContactFields.SUBSCRIPTION_STATUS);
        final FieldInfo subscriptionTypeField = fields.get(ContactFields.SUBSCRIPTION_TYPES);
        final FieldInfo unsubscribeReasonField = fields.get(ContactFields.UNSUBSCRIBE_REASON);
        final String subscriptionStatusKey = subscriptionStatusField.idAsString();
        final String subscriptionTypeKey = subscriptionTypeField.idAsString();
        final String unsubscribeReasonKey = unsubscribeReasonField.idAsString();
        final String otherUnsubscribeReasonKey = fields.get(ContactFields.OTHER_UNSUBSCRIBE_REASON).idAsString();
        final String timeZoneFieldKey = fields.get(timeZoneKey).idAsString();
        final String salesOwnerKey = fields.get(ContactFields.SALES_OWNER).idAsString();
        final String territoryKey = fields.get(ContactFields.TERRITORY).idAsString();
        final FieldInfo contactSourceField = fields.get(ContactFields.SOURCE);
        final String contactSourceKey = contactSourceField.idAsString();
        final String addressKey = fields.get(ContactFields.ADDRESS).idAsString();
        final String lifecycleStageKey = fields.get(ContactFields.LIFECYCLE_STAGE).idAsString();
        final String lifecycleStatusKey = fields.get(ContactFields.LIFECYCLE_STATUS).idAsString();
        final FieldInfo lostReasonField = fields.get(ContactFields.CONTACT_LOST_REASON);
        final String lostReasonKey = lostReasonField.idAsString();
        final String companyKey = fields.get(ContactFields.COMPANY).idAsString();
        final Long subscriptionStatusFieldId = subscriptionStatusField.id();
        final CrmUser user = userService.getUser(orgId, ownerId);
        final String marketingListKey = fields.get(ContactFields.MARKETING_LIST).idAsString();

        contact.setOrgId(orgId);
        contact.setId(jsonObject.getLong(ContactFields.ID));
        contact.setCreatedBy(user);
        contact.setCreatedTime(dateTimeUtil.getTemporal(jsonObject.getLong(ContactFields.CREATED_TIME)));
        contact.setModifiedTime(dateTimeUtil.getTemporal(jsonObject.getLong(ContactFields.UPDATED_TIME)));
        contact.setFirstName(jsonObject.containsKey(firstNameKey) ? jsonObject.getString(firstNameKey)
                : contact.getFirstName());
        contact.setLastName(jsonObject.containsKey(lastNameKey) ? jsonObject.getString(lastNameKey)
                : contact.getLastName());
        contact.setDesignation(jsonObject.optString(fields.get(ContactFields.DESIGNATION).idAsString(), contact.getDesignation()));
        contact.setDepartment(jsonObject.optString(fields.get(ContactFields.DEPARTMENT).idAsString(), contact.getDepartment()));
        contact.setFacebook(jsonObject.optString(fields.get(ContactFields.FACEBOOK).idAsString(), contact.getFacebook()));
        contact.setTwitter(jsonObject.optString(fields.get(ContactFields.TWITTER).idAsString(), contact.getTwitter()));
        contact.setLinkedIn(jsonObject.optString(fields.get(ContactFields.LINKEDIN).idAsString(), contact.getLinkedIn()));
        contact.setSalesOwner(jsonObject.containsKey(salesOwnerKey) ? userService.getUser(orgId, jsonObject.getLong(salesOwnerKey))
                : user);
        contact.setUpdatedBy(user);
        contact.setContactSource(jsonObject.containsKey(contactSourceKey) ? optionConverter.convert(orgId,
                contactSourceField.id(), jsonObject.getLong(contactSourceKey)) : contact.getContactSource());
        contact.setRecentNote(jsonObject.optString(CompanyFields.RECENT_NOTE, null));
        entityBuilder.buildValueHolder(contact, converterConfig, jsonObject);

        if (jsonObject.containsKey(dobKey)) {
            contact.setDateOfBirth(new Date(jsonObject.getLong(dobKey)));
        }

        if (jsonObject.containsKey(emailKey)) {
            final JsonArray emails = jsonObject.getJsonArray(emailKey);
            final String primaryKey = ContactFields.IS_PRIMARY;

            for (int index = 0; index < emails.size(); index++) {
                final JsonObject emailJson = emails.getJsonObject(index);
                final Email email = emailConverter.convert(converterConfig, emailJson);

                if (emailJson.optBoolean(primaryKey)) {
                    contact.setPrimaryEmail(email);
                }

                contact.addEmail(email);
            }
        }

        if (jsonObject.containsKey(phoneKey)) {
            final JsonArray phones = jsonObject.getJsonArray(phoneKey);
            final String primaryKey = ContactFields.IS_PRIMARY;

            for (int index = 0; index < phones.size(); index++) {
                final JsonObject phoneJson = phones.getJsonObject(index);
                final Phone phone = phoneConverter.convert(converterConfig, phoneJson);

                if (phoneJson.optBoolean(primaryKey)) {
                    contact.setPrimaryPhone(phone);
                }

                contact.addPhone(phone);
            }
        }

        if (jsonObject.containsKey(companyKey)) {
            final Company company = new Company();

            company.setId(jsonObject.getLong(companyKey));
            company.setOrgId(orgId);
            contact.setCompany(company);
        }

        if (jsonObject.containsKey(lifecycleStageKey)) {
            final LifeCycleStage lifeCycleStage = new LifeCycleStage();

            lifeCycleStage.setId(jsonObject.getLong(lifecycleStageKey));
            contact.setLifeCycleStage(lifeCycleStage);
        }

        if (jsonObject.containsKey(lifecycleStatusKey)) {
            final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

            lifeCycleStatus.setId(jsonObject.getLong(lifecycleStatusKey));
            contact.setLifeCycleStatus(lifeCycleStatus);
        }

        if (jsonObject.containsKey(lostReasonKey)) {
            contact.setLostReason(optionConverter.convert(orgId, lostReasonField.id(), jsonObject.getLong(lostReasonKey)));
        }

        if (jsonObject.containsKey(addressKey)) {
            final JsonObject addressJson = jsonObject.getJsonObject(addressKey);

            contact.setAddress(addressConverter.convert(converterConfig, addressJson));
        }

        if (jsonObject.containsKey(subscriptionStatusKey)) {
            contact.setSubscriptionStatus(optionConverter.convert(orgId, subscriptionStatusFieldId,
                    jsonObject.getLong(subscriptionStatusKey)));
        } else if (Objects.isNull(contact.id())) {
            contact.setSubscriptionStatus(optionService.getOption(orgId, subscriptionStatusField,
                    Contact.SubscriptionStatus.SUBSCRIBED.getName()));
        }

        if (jsonObject.containsKey(subscriptionTypeKey)) {
            jsonObject.getJsonArray(subscriptionTypeKey).forEach(subscriptionTypeId ->
                    contact.addSubscriptionType(optionService.get(orgId, subscriptionTypeField.id(), NumberUtils.parseNumber(subscriptionTypeId).longValue())));
        }

        if (jsonObject.containsKey(unsubscribeReasonKey)) {
            contact.setUnsubscribeReason(optionConverter.convert(orgId, unsubscribeReasonField.id(), jsonObject.getLong(unsubscribeReasonKey)));
        }

        if (jsonObject.containsKey(otherUnsubscribeReasonKey)) {
            contact.setOtherUnsubscribeReason(jsonObject.getString(otherUnsubscribeReasonKey));
        }

        if (jsonObject.containsKey(timeZoneFieldKey)) {
            contact.setTimeZone(optionConverter.convert(orgId, fields.get(timeZoneKey).id(), jsonObject.getLong(timeZoneFieldKey)));
        }

        if (jsonObject.containsKey(territoryKey)) {
            final Territory territory = new Territory();

            territory.setId(jsonObject.getLong(territoryKey));
            contact.setTerritory(territory);
        }

        if (jsonObject.containsKey(marketingListKey)) {
            final Collection<MarketingList> marketingLists = new ArrayList<>();
            final JsonArray marketingListIdArray = jsonObject.getJsonArray(marketingListKey);
            final int size = marketingListIdArray.size();

            for (int index = 0; index < size; index++) {
                final MarketingList marketingList = new MarketingList();

                marketingList.setId(marketingListIdArray.getLong(index));
                marketingList.setOrgId(orgId);
                marketingLists.add(marketingList);
            }

            if (!marketingLists.isEmpty()) {
                contact.setMarketingLists(marketingLists);
            }
        }

        if (jsonObject.containsKey(SummaryFields.SUMMARY)) {
            contact.setSummaryInfo(summaryInfoConverter.convert(converterConfig, jsonObject.getJsonObject(SummaryFields.SUMMARY)));
        }

        return contact;
    }
}
