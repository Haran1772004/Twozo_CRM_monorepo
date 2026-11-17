package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.internal.event.ZapEventData;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.model.Date;

import com.tlc.iam.common.models.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the conversion of model data into a JSON format suitable for Zapier event payloads.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContactZapEventDataConverter extends AbstractZapEventDataConverter<Contact> {

    private static final class Instance {
        private static final ContactZapEventDataConverter EVENT_DATA_CONVERTER = new ContactZapEventDataConverter();
    }

    public static ContactZapEventDataConverter getInstance() {
        return Instance.EVENT_DATA_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact      The specific model instance to convert.
     * @param zapEventData The {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    @Override
    protected JsonObject convert(final Contact contact, final ZapEventData zapEventData) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(contact.orgId(), Source.CONTACT.getName());
        final JsonObject contactJson = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(zapEventData.getLocale());
        final Map<Long, User> userMap = zapEventData.getGroupedUsers();
        final ValueHolder valueHolder = contact.getValueHolder();

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(fieldInfo, valueHolder.get(fieldInfo.id()), contactJson, i18nResolver, userMap);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "firstName" -> contactJson.put(aliasName, contact.getFirstName());
                    case "lastName" -> contactJson.put(aliasName, contact.getLastName());
                    case "facebook" -> contactJson.put(aliasName, contact.getFacebook());
                    case "twitter" -> contactJson.put(aliasName, contact.getTwitter());
                    case "linkedIn" -> contactJson.put(aliasName, contact.getLinkedIn());
                    case "emails" -> {
                        final Collection<Email> contactEmails = contact.getEmails();

                        if (Objects.nonNull(contactEmails)) {
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final JsonArray emails = Json.array();

                            for (final Email email : contactEmails) {
                                final JsonObject emailJson = Json.object();

                                emailJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).name()), email.getValue());
                                emailJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.EMAIL_TYPE).name()), email.getType());

                                emails.put(emailJson);
                            }
                            contactJson.put(aliasName, emails);
                        }
                    }
                    case "phones" -> {
                        final Collection<Phone> contactPhones = contact.getPhones();

                        if (Objects.nonNull(contactPhones)) {
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final JsonArray phones = Json.array();

                            for (final Phone phone : contactPhones) {
                                final JsonObject phoneJson = Json.object();

                                phoneJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).name()), phone.getNumber());
                                phoneJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.PHONE_TYPE).name()), phone.getType());

                                phones.put(phoneJson);
                            }
                            contactJson.put(aliasName, phones);
                        }
                    }
                    case "company" -> {
                        final Company company = contact.getCompany();

                        if (Objects.nonNull(company)) {
                            final JsonObject companyJson = Json.object();

                            companyJson.put(NAME, company.getName());
                            companyJson.put(ID, company.id());

                            contactJson.put(aliasName, companyJson);
                        }
                    }
                    case "designation" -> contactJson.put(aliasName, contact.getDesignation());
                    case "salesOwner" -> {
                        final JsonObject saleOwnerJson = Json.object();
                        final CrmUser salesOwner = contact.getSalesOwner();

                        saleOwnerJson.put(NAME, userMap.get(salesOwner.getIamId()).getFullName());
                        saleOwnerJson.put(ID, salesOwner.id());

                        contactJson.put(aliasName, saleOwnerJson);
                    }
                    case "lifeCycleStage" -> {
                        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();

                        if (Objects.nonNull(lifeCycleStage)) {
                            final JsonObject lifecycleStageJson = Json.object();

                            lifecycleStageJson.put(NAME, lifeCycleStage.getName());
                            lifecycleStageJson.put(ID, lifeCycleStage.id());

                            contactJson.put(aliasName, lifecycleStageJson);
                        }
                    }
                    case "lifeCycleStatus" -> {
                        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();

                        if (Objects.nonNull(lifeCycleStatus)) {
                            final JsonObject lifeCycleStatusJson = Json.object();

                            lifeCycleStatusJson.put(NAME, lifeCycleStatus.getName());
                            lifeCycleStatusJson.put(ID, lifeCycleStatus.id());

                            contactJson.put(aliasName, lifeCycleStatusJson);
                        }
                    }
                    case "contactLostReason" -> {
                        final Option lostReason = contact.getLostReason();

                        if (Objects.nonNull(lostReason)) {
                            contactJson.put(aliasName, i18nResolver.get(lostReason.getOption()));
                        }
                    }
                    case "contactSource" -> {
                        final Option contactSource = contact.getContactSource();

                        if (Objects.nonNull(contactSource)) {
                            contactJson.put(aliasName, i18nResolver.get(contactSource.getOption()));
                        }
                    }
                    case "territory" -> {
                        final Territory territory = contact.getTerritory();

                        if (Objects.nonNull(territory)) {
                            final JsonObject territoryJson = Json.object();

                            territoryJson.put(NAME, territory.getName());
                            territoryJson.put(ID, territory.id());

                            contactJson.put(aliasName, territoryJson);
                        }
                    }
                    case "tags" -> {
                        final SummaryInfo contactSummary = contact.getSummaryInfo();

                        if (Objects.nonNull(contactSummary)) {
                            final Collection<Tag> contactTags = contactSummary.getTags();

                            if (Objects.nonNull(contactTags)) {
                                final JsonArray tags = Json.array();

                                for (final Tag tag : contactTags) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(NAME, tag.getName());
                                    tagJson.put(ID, tag.id());

                                    tags.put(tagJson);
                                }
                                contactJson.put(aliasName, tags);
                            }
                        }
                    }
                    case "dateOfBirth" -> {
                        final Date dateOfBirth = contact.getDateOfBirth();

                        if (Objects.nonNull(dateOfBirth)) {
                            contactJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(dateOfBirth.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "department" -> contactJson.put(aliasName, contact.getDepartment());
                    case "address" -> {
                        final Address address = contact.getAddress();

                        if (Objects.nonNull(address)) {
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final JsonObject addressJson = Json.object();

                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).name()), address.getAddressLine1());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE2).name()), address.getAddressLine2());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CITY).name()), address.getCity());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.STATE).name()), address.getState());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.COUNTRY).name()),
                                    Optional.ofNullable(address.getCountry()).map(Country::getName).orElseGet(() -> null));
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ZIPCODE).name()), address.getZipcode());

                            contactJson.put(aliasName, addressJson);
                        }
                    }
                    case "subscriptionStatus" -> {
                        final Option subscriptionStatus = contact.getSubscriptionStatus();

                        if (Objects.nonNull(subscriptionStatus)) {
                            contactJson.put(aliasName, i18nResolver.get(subscriptionStatus.getOption()));
                        }
                    }
                    case "subscriptionTypes" -> {
                        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();

                        if (Objects.nonNull(subscriptionTypes)) {
                            final JsonArray types = Json.array();

                            for (final Option subscriptionType : subscriptionTypes) {
                                types.put(i18nResolver.get(subscriptionType.getOption()));
                            }

                            contactJson.put(aliasName, types);
                        }
                    }
                    case "unsubscribeReason" -> {
                        final Option unsubscribeReason = contact.getUnsubscribeReason();

                        if (Objects.nonNull(unsubscribeReason)) {
                            contactJson.put(aliasName, i18nResolver.get(unsubscribeReason.getOption()));
                        }
                    }
                    case "otherUnsubscribeReason" -> contactJson.put(aliasName, contact.getOtherUnsubscribeReason());
                    case "timeZone" -> {
                        final Option timezoneInfo = contact.getTimeZone();

                        if (Objects.nonNull(timezoneInfo)) {
                            contactJson.put(aliasName,
                                    i18nResolver.get(timezoneInfo.getOption()));
                        }
                    }
                    case "createdBy" -> {
                        final JsonObject creatorJson = Json.object();
                        final CrmUser creator = contact.getCreatedBy();

                        creatorJson.put(NAME, userMap.get(creator.getIamId()).getFullName());
                        creatorJson.put(ID, creator.id());

                        contactJson.put(aliasName, creatorJson);
                    }
                    case "updatedBy" -> {
                        final JsonObject updaterJson = Json.object();
                        final CrmUser updatedBy = contact.getUpdatedBy();

                        updaterJson.put(NAME, userMap.get(updatedBy.getIamId()).getFullName());
                        updaterJson.put(ID, updatedBy.id());

                        contactJson.put(aliasName, updaterJson);
                    }
                    case "createdAt" ->
                            contactJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(contact.getCreatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "updatedAt" ->
                            contactJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(contact.getModifiedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "lastAssignedAt" ->
                            contactJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(contact.getLastAssignedAt().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                }
            }
        }

        return contactJson;
    }

}
