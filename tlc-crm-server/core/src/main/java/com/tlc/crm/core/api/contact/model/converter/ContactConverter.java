package com.tlc.crm.core.api.contact.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.marketinglist.model.converter.MarketingListConverter;
import com.tlc.crm.core.api.summaryInfo.model.converter.SummaryInfoConverter;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.entity.builder.ViewConverter;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.AddressConverter;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.commons.util.TypeChecker;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.l10n.model.number.PhoneNumber;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link Contact}
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class ContactConverter extends ViewConverter<Contact> {

    private final FieldTypeService fieldTypeService;
    private final CompanyConverter companyConverter;
    private final ContactManager contactManager;
    private final EmailConverter emailConverter;
    private final PhoneConverter phoneConverter;
    private final AddressConverter addressConverter;
    private final LifeCycleStageConverter lifeCycleStageConverter;
    private final LifeCycleStatusConverter lifeCycleStatusConverter;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final OptionConverter optionConverter;
    private final UserService userService;
    private final CrmUserConverter crmUserConverter;
    private final TerritoryConverter territoryConverter;
    private final TagConverter tagConverter;
    private final SummaryInfoConverter summaryInfoConverter;
    private DealConverter dealConverter;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;
    private final MarketingListConverter marketingListConverter;

    private static class Instance {
        private static final ContactConverter INSTANCE = new ContactConverter();
    }

    private ContactConverter() {
        contactManager = ContactManager.getInstance();
        emailConverter = EmailConverter.getInstance();
        phoneConverter = PhoneConverter.getInstance();
        addressConverter = AddressConverter.getInstance();
        companyConverter = CompanyConverter.getInstance();
        lifeCycleStageConverter = LifeCycleStageConverter.getInstance();
        lifeCycleStatusConverter = LifeCycleStatusConverter.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        optionConverter = OptionConverter.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        userService = UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        territoryConverter = TerritoryConverter.getInstance();
        tagConverter = TagConverter.getInstance();
        summaryInfoConverter = SummaryInfoConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        dateUtil = DateUtil.getInstance();
        marketingListConverter = MarketingListConverter.getInstance();
    }

    public static ContactConverter getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Contact convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return constructContactByPartialData(converterConfig, jsonObject);
        } else {
            return constructContact(converterConfig, jsonObject);
        }
    }

    private Contact constructContactByPartialData(ConverterConfig converterConfig, JsonObject jsonObject) {
        final Contact contact = contactManager.get(converterConfig.orgId(), jsonObject.getLong(ContactFields.ID));

        setJsonDataToModel(converterConfig, contact, jsonObject);
        return contact;
    }

    private Contact constructContact(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Contact contact = new Contact();
        final long orgId = converterConfig.orgId();
        final String idKey = ContactFields.ID;

        if (jsonObject.containsKey(idKey)) {
            final long id = jsonObject.getLong(idKey);

            contact.setId(id);
            contact.setSalesOwner(contactManager.partialGet(orgId, id).getSalesOwner());
        } else {
            contact.setCreatedBy(userService.getUser(orgId, converterConfig.ownerId()));
        }
        setJsonDataToModel(converterConfig, contact, jsonObject);
        return contact;
    }

    /**
     * Sets JSON data into {@link Contact}
     *
     * @param converterConfig Converter configuration
     * @param contact         A model to which the contact attributes will be set
     * @param jsonObject      Holds the field values of contact
     */
    public void setJsonDataToModel(final ConverterConfig converterConfig, final Contact contact, final JsonObject jsonObject) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final String firstNameKey = fields.get(ContactFields.FIRST_NAME).idAsString();
        final String lastNameKey = fields.get(ContactFields.LAST_NAME).idAsString();
        final String emailKey = fields.get(ContactFields.EMAILS).idAsString();
        final String phoneKey = fields.get(ContactFields.PHONES).idAsString();
        final String dobKey = fields.get(ContactFields.DATE_OF_BIRTH).idAsString();
        final String companyKey = fields.get(ContactFields.COMPANY).idAsString();
        final FieldInfo timeZoneField = fields.get(ContactFields.TIMEZONE);
        final String timeZoneKey = timeZoneField.idAsString();
        final String salesOwnerKey = fields.get(ContactFields.SALES_OWNER).idAsString();
        final String territoryKey = fields.get(ContactFields.TERRITORY).idAsString();
        final String tagKey = fields.get(ContactFields.TAGS).idAsString();
        final Long orgId = converterConfig.orgId();
        final FieldInfo contactSourceField = fields.get(ContactFields.SOURCE);
        final String contactSourceKey = contactSourceField.idAsString();
        final Long ownerId = converterConfig.ownerId();
        final String addressKey = fields.get(ContactFields.ADDRESS).idAsString();
        final String designationKey = fields.get(ContactFields.DESIGNATION).idAsString();
        final String departmentKey = fields.get(ContactFields.DEPARTMENT).idAsString();
        final String facebookKey = fields.get(ContactFields.FACEBOOK).idAsString();
        final String twitterKey = fields.get(ContactFields.TWITTER).idAsString();
        final String linkedInKey = fields.get(ContactFields.LINKEDIN).idAsString();
        final String marketingListKey = fields.get(ContactFields.MARKETING_LIST).idAsString();

        contact.setOrgId(orgId);
        setSalesOwner(orgId, salesOwnerKey, jsonObject, contact, ownerId);
        contact.setUpdatedBy(userService.getUser(orgId, ownerId));
        contact.setFirstName(jsonObject.optNullableValue(firstNameKey, contact.getFirstName()));
        contact.setLastName(jsonObject.optNullableValue(lastNameKey, contact.getLastName()));
        contact.setDesignation(jsonObject.optNullableValue(designationKey, contact.getDesignation()));
        contact.setDepartment(jsonObject.optNullableValue(departmentKey, contact.getDepartment()));
        contact.setFacebook(jsonObject.optNullableValue(facebookKey, contact.getFacebook()));
        contact.setTwitter(jsonObject.optNullableValue(twitterKey, contact.getTwitter()));
        contact.setLinkedIn(jsonObject.optNullableValue(linkedInKey, contact.getLinkedIn()));

        if (jsonObject.containsKey(dobKey)) {
            if (jsonObject.isKeyPresentWithNullValue(dobKey)) {
                contact.setDateOfBirth(null);
            } else {
                contact.setDateOfBirth(new Date(jsonObject.getNullableValue(dobKey)));
            }
        }
        entityBuilder.buildValueHolder(contact, converterConfig, jsonObject);

        if (jsonObject.containsKey(emailKey) && jsonObject.isKeyPresentWithNullValue(emailKey)) {
            contact.setEmails(null);
        } else {
            buildEmails(converterConfig, contact, jsonObject, emailKey);
        }

        if (jsonObject.containsKey(phoneKey) && jsonObject.isKeyPresentWithNullValue(phoneKey)) {
            contact.setPhones(null);
        } else {
            buildPhones(converterConfig, contact, jsonObject, phoneKey);
        }

        if (jsonObject.containsKey(companyKey) && jsonObject.isKeyPresentWithNullValue(companyKey)) {
            contact.setCompany(null);
        } else {
            buildCompany(converterConfig, fields, contact, jsonObject);
        }
        buildLifeCycle(fields, contact, jsonObject);
        buildSubscription(orgId, fields, contact, jsonObject, converterConfig);
        buildMarketingList(orgId, contact, jsonObject, marketingListKey);

        if (jsonObject.containsKey(addressKey)) {
            if (jsonObject.isKeyPresentWithNullValue(addressKey)) {
                contact.setAddress(null);
            } else {
                final Address contactAddress;
                final JsonObject addressJson = jsonObject.getJsonObject(addressKey);

                if (converterConfig.isPartialData()) {
                    final Address oldAddress = contact.getAddress();
                    final Address address = Objects.isNull(oldAddress) ? new Address() : oldAddress;

                    addressConverter.setDataToAddress(converterConfig, address, addressJson);
                    contactAddress = address;
                } else {
                    contactAddress = addressConverter.convert(converterConfig, addressJson);
                }
                contact.setAddress(contactAddress);
            }
        }

        if (jsonObject.containsKey(contactSourceKey)) {
            contact.setContactSource(jsonObject.isKeyPresentWithNullValue(contactSourceKey) ? null : optionConverter.convert(orgId,
                    contactSourceField.id(), jsonObject.getLong(contactSourceKey)));
        }

        if (jsonObject.containsKey(timeZoneKey)) {
            if (jsonObject.isKeyPresentWithNullValue(timeZoneKey)) {
                contact.setTimeZone(null);
            } else {
                contact.setTimeZone(optionConverter.convert(orgId, timeZoneField.id(), jsonObject.getLong(timeZoneKey)));
            }
        }

        if (jsonObject.containsKey(territoryKey)) {
            if (jsonObject.isKeyPresentWithNullValue(territoryKey)) {
                contact.setTerritory(null);
            } else {
                final Territory territory = new Territory();

                territory.setId(jsonObject.getLong(territoryKey));
                contact.setTerritory(territory);
            }
        }

        if (jsonObject.containsKey(tagKey)) {
            contact.setSummaryInfo(summaryInfoConverter.buildTags(tagKey, contact.getSummaryInfo(), converterConfig, jsonObject));
        }

        if (converterConfig.areReadOnlyFieldsRequired()) {
            final String createdAtKey = fields.get(ContactFields.CREATED_AT).idAsString();

            if (jsonObject.containsKey(createdAtKey)) {
                contact.setCreatedTime(new DateTime(jsonObject.getLong(createdAtKey)));
            }
        }
        defaultFieldValuesLoader.load(contact);
    }

    /**
     * <p>
     * Sets the default subscription for given contact
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param contact Represents the {@link Contact}
     */
    private void setDefaultSubscriptions(final long orgId, final Contact contact) {
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, List.of(subscriptionStatusKey, subscriptionTypesKey));

        contact.setSubscriptionStatus(optionService.getOption(orgId, fields.get(subscriptionStatusKey),
                Contact.SubscriptionStatus.SUBSCRIBED.getName()));
        contact.setSubscriptionTypes(optionService.getDefaultOptions(orgId, fields.get(subscriptionTypesKey)));
        contact.setUnsubscribeReason(null);
        contact.setOtherUnsubscribeReason(null);
    }

    /**
     * <p>
     * Builds the lifecycle entity from the given json object and map it to contact
     * </p>
     *
     * @param fields     System fields
     * @param contact    An entity that holds the attributes of contact
     * @param jsonObject Holds the field values of contact
     */
    private void buildLifeCycle(final Map<String, FieldInfo> fields, final Contact contact,
                                final JsonObject jsonObject) {
        final Long orgId = contact.orgId();
        final String lifecycleStageKey = fields.get(ContactFields.LIFECYCLE_STAGE).idAsString();
        final String lifecycleStatusKey = fields.get(ContactFields.LIFECYCLE_STATUS).idAsString();

        if (jsonObject.containsKey(lifecycleStageKey)) {
            final LifeCycleStage lifeCycleStage = new LifeCycleStage();

            lifeCycleStage.setId(jsonObject.getLong(lifecycleStageKey));
            contact.setLifeCycleStage(lifeCycleStage);
        } else if (Objects.isNull(contact.getLifeCycleStage())) {
            contact.setLifeCycleStage(lifeCycleStageManager.getDefaultStage(orgId));
        }

        if (jsonObject.containsKey(lifecycleStatusKey)) {
            final LifeCycleStatus lifeCycleStatus = lifeCycleStatusManager.get(orgId, jsonObject.getLong(lifecycleStatusKey));
            final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
            final FieldInfo lostReasonField = fields.get(lostReasonKey);

            loadLostReasonIntoModel(orgId, false, lostReasonField, lostReasonField.idAsString(), lifeCycleStatus.getLabel(), contact, jsonObject);
            contact.setLifeCycleStatus(lifeCycleStatus);
        } else if (Objects.isNull(contact.getLifeCycleStatus())) {
            contact.setLifeCycleStatus(lifeCycleStatusManager.getDefaultStatus(orgId, contact.getLifeCycleStage().id()));
        }
    }

    /**
     * <p>
     * Sets the lost reason into {@link Contact}.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param needDefaultReason Represents the flag that indicates whether the default lost reason is necessary or not.
     * @param label             Represents the label of the lifecycle status.
     * @param contact           Holds the details of the contact.
     * @param jsonObject        Holds the requested data.
     */
    public void loadLostReasonIntoModel(final long orgId, final boolean needDefaultReason, final FieldInfo lostReasonField,
                                        final String lostReasonKey, final LifeCycleStatus.Label label, final Contact contact,
                                        final JsonObject jsonObject) {
        if (LifeCycleStatus.Label.CLOSED_LOST == label) {
            if (jsonObject.containsKey(lostReasonKey)) {
                final long lostReasonFieldId = lostReasonField.id();
                final long lostReasonId = jsonObject.getLong(lostReasonKey);

                if (optionService.exists(orgId, lostReasonFieldId, lostReasonId)) {
                    contact.setLostReason(optionConverter.convert(orgId, lostReasonFieldId, lostReasonId));
                } else {
                    throw ErrorCode.get(ContactErrorCodes.LOST_REASON_NOT_FOUND, "i18n.contact.lostReason.not.found");
                }
            } else if (needDefaultReason) {
                contact.setLostReason(optionService.getDefaultOption(orgId, lostReasonField));
            } else {
                throw ErrorCode.get(ContactErrorCodes.INVALID_LOST_REASON, "i18n.contact.lostReason.invalid");
            }
        } else if (Objects.nonNull(contact.getLostReason())) {
            contact.setLostReason(null);
        }
    }

    /**
     * <p>
     * Builds the marketing list for the provided contact
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param contact          Represents the {@link Contact}
     * @param jsonData         Represents the request json data
     * @param marketingListKey Represents the marketing list field key
     */
    private void buildMarketingList(final Long orgId, final Contact contact, final JsonObject jsonData, final String marketingListKey) {
        if (jsonData.containsKey(marketingListKey)) {
            if (jsonData.isKeyPresentWithNullValue(marketingListKey)) {
                contact.setMarketingLists(null);
            } else {
                final Collection<Long> marketingListIds = (Collection<Long>) jsonData.get(marketingListKey);
                final Collection<MarketingList> marketingLists = new ArrayList<>(marketingListIds.size());

                marketingListIds.forEach(marketingListId -> {
                    final MarketingList marketingList = new MarketingList();

                    marketingList.setId(marketingListId);
                    marketingList.setOrgId(orgId);
                    marketingLists.add(marketingList);
                });

                if (!marketingListIds.isEmpty()) {
                    contact.setMarketingLists(marketingLists);
                }
            }
        }
    }

    /**
     * <p>
     * Builds subscription for the provided contact
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param fields          Represents the collection of contact fields
     * @param contact         Represents the {@link Contact}
     * @param jsonObject      Represents the json which contains the contact details
     * @param converterConfig Represents the config to convert
     */
    private void buildSubscription(final long orgId, final Map<String, FieldInfo> fields, final Contact contact, final JsonObject jsonObject, final ConverterConfig converterConfig) {
        final FieldInfo subscriptionStatusField = fields.get(ContactFields.SUBSCRIPTION_STATUS);
        final FieldInfo subscriptionTypeField = fields.get(ContactFields.SUBSCRIPTION_TYPES);
        final String subscriptionStatusKey = subscriptionStatusField.idAsString();
        final String subscriptionTypeKey = subscriptionTypeField.idAsString();
        final FieldInfo unsubscribeReasonField = fields.get(ContactFields.UNSUBSCRIBE_REASON);
        final String unsubscribeReasonKey = unsubscribeReasonField.idAsString();
        final String otherUnsubscribeReasonKey = fields.get(ContactFields.OTHER_UNSUBSCRIBE_REASON).idAsString();

        if (jsonObject.containsKey(subscriptionStatusKey)) {
            if (jsonObject.isKeyPresentWithNullValue(subscriptionStatusKey)) {
                setDefaultSubscriptions(orgId, contact);
            } else {
                final long subscriptionStatusFieldId = subscriptionStatusField.id();
                final Long subscriptionStatusId = jsonObject.getLong(subscriptionStatusKey);
                final Option subscriptionStatus = optionService.get(orgId, subscriptionStatusFieldId, subscriptionStatusId);

                contact.setSubscriptionStatus(Objects.nonNull(subscriptionStatus) ? subscriptionStatus
                        : optionConverter.convert(orgId, subscriptionStatusFieldId, subscriptionStatusId));
            }
        } else if (Objects.isNull(contact.id())) {
            final Option defaultSubscriptionStatus = optionService.getDefaultOption(orgId, subscriptionStatusField);
            final Collection<Option> defaultSubscriptionTypes = optionService.getDefaultOptions(orgId, subscriptionTypeField);
            final Option subscriptionStatus = Objects.isNull(defaultSubscriptionStatus) ? optionService.getOption(orgId, subscriptionStatusField,
                    Contact.SubscriptionStatus.SUBSCRIBED.getName()) : defaultSubscriptionStatus;

            contact.setSubscriptionStatus(subscriptionStatus);
            contact.setSubscriptionTypes(defaultSubscriptionTypes.isEmpty() ? optionService.get(orgId, subscriptionTypeField) : defaultSubscriptionTypes);

            if (Contact.SubscriptionStatus.UNSUBSCRIBED.getName().equals(subscriptionStatus.getOption())) {
                final Option defaultUnSubscribeReasonOption = optionService.getDefaultOption(orgId, unsubscribeReasonField);

                contact.setUnsubscribeReason(Objects.isNull(defaultUnSubscribeReasonOption) ? optionService.get(orgId, unsubscribeReasonField)
                        .stream().findFirst().orElseGet(() -> null) : defaultUnSubscribeReasonOption);
            }
        }

        if (jsonObject.containsKey(subscriptionTypeKey)) {

            if (converterConfig.isPartialData()) {
                contact.setSubscriptionTypes(null);
            }

            if (jsonObject.isKeyPresentWithNullValue(subscriptionTypeKey)) {
                final String subscriptionStatus = contact.getSubscriptionStatus().getOption();

                if (Objects.equals(Contact.SubscriptionStatus.SUBSCRIBED.getName(), subscriptionStatus)) {
                    setDefaultSubscriptions(orgId, contact);
                }
            } else {
                final long subscriptionTypeFieldId = subscriptionTypeField.id();

                jsonObject.getJsonArray(subscriptionTypeKey).forEach(subscriptionTypeId -> {
                    final Option subscriptionType = optionService.get(orgId, subscriptionTypeFieldId, (Long) subscriptionTypeId);

                    contact.addSubscriptionType(Objects.nonNull(subscriptionType) ? subscriptionType
                            : optionConverter.convert(orgId, subscriptionTypeFieldId, (long) subscriptionTypeId));
                });
            }
        }

        if (jsonObject.containsKey(unsubscribeReasonKey)) {
            if (jsonObject.isKeyPresentWithNullValue(unsubscribeReasonKey)) {
                final String subscriptionStatus = contact.getSubscriptionStatus().getOption();

                if (Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatus)) {
                    contact.setUnsubscribeReason(null);
                }
            } else {
                final long unsubscribeReasonFieldId = unsubscribeReasonField.id();
                final Long unsubscribeReasonId = jsonObject.getLong(unsubscribeReasonKey);
                final Option unsubscribeReason = optionService.get(orgId, unsubscribeReasonFieldId, unsubscribeReasonId);

                contact.setUnsubscribeReason(Objects.nonNull(unsubscribeReason) ? unsubscribeReason
                        : optionConverter.convert(orgId, unsubscribeReasonFieldId, unsubscribeReasonId));
            }
        }

        if (jsonObject.containsKey(otherUnsubscribeReasonKey)) {

            if (jsonObject.isKeyPresentWithNullValue(otherUnsubscribeReasonKey)) {
                final String subscriptionStatus = contact.getSubscriptionStatus().getOption();

                if (Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatus)) {
                    contact.setOtherUnsubscribeReason(null);
                }
            } else {
                contact.setOtherUnsubscribeReason(jsonObject.getString(otherUnsubscribeReasonKey));
            }
        }

        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final String subscriptionStatusOption = Objects.nonNull(subscriptionStatus) ? subscriptionStatus.getOption() : null;
        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final String unSubscribeReasonOption = Objects.nonNull(unsubscribeReason) ? unsubscribeReason.getOption() : null;
        final String otherUnsubscribeReason = contact.getOtherUnsubscribeReason();

        if (Objects.nonNull(subscriptionStatusOption)) {

            if (Objects.equals(Contact.SubscriptionStatus.SUBSCRIBED.getName(), subscriptionStatusOption)) {

                if (Objects.nonNull(unsubscribeReason)) {
                    contact.setUnsubscribeReason(null);
                }

                if (Objects.nonNull(otherUnsubscribeReason)) {
                    contact.setOtherUnsubscribeReason(null);
                }

            } else if (Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatusOption)) {

                if (Objects.nonNull(subscriptionTypes)) {
                    contact.setSubscriptionTypes(null);
                }

                if (Objects.nonNull(unSubscribeReasonOption) && !Objects.equals(Contact.UnsubscribeReason.OTHER_UNSUBSCRIBE_REASON.getName(), unSubscribeReasonOption)) {
                    contact.setOtherUnsubscribeReason(null);
                }
            } else {

                if (Objects.nonNull(subscriptionTypes)) {
                    contact.setSubscriptionTypes(null);
                }

                if (Objects.nonNull(unsubscribeReason)) {
                    contact.setUnsubscribeReason(null);
                }

                if (Objects.nonNull(otherUnsubscribeReason)) {
                    contact.setOtherUnsubscribeReason(null);
                }
            }
        }
    }

    /**
     * <p>
     * Builds the {@link Company} entity from the given json objects and map it to contact
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param fields          System fields
     * @param contact         An entity that holds the attributes of contact
     * @param jsonObject      Holds the field values of contact
     */
    private void buildCompany(final ConverterConfig converterConfig, final Map<String, FieldInfo> fields,
                              final Contact contact, final JsonObject jsonObject) {
        final FieldInfo companyField = fields.get(ContactFields.COMPANY);
        final Object value = jsonObject.opt(companyField.idAsString());
        final Company oldCompany = contact.getCompany();
        final boolean isPartial = converterConfig.isPartialData();

        if (Objects.nonNull(value)) {
            final Company company;

            if (TypeChecker.isNonDecimalNumber(value)) {
                company = new Company();
                company.setId((Long) value);
                company.setOrgId(converterConfig.orgId());
            } else {
                converterConfig.setPartialData(false);
                company = companyConverter.convert(converterConfig, jsonObject.optJsonObject(companyField.idAsString()));

                company.setNewEntity(true);
                converterConfig.setPartialData(isPartial);
            }
            contact.setCompany(company);
        } else if (isPartial && Objects.nonNull(oldCompany)) {
            oldCompany.setOrgId(converterConfig.orgId());
            contact.setCompany(oldCompany);
        }
    }

    /**
     * Sets {@link Phone} details into {@link Contact}
     *
     * @param converterConfig Converter configuration
     * @param contact         An entity that holds the attributes of contact
     * @param jsonObject      Holds the field values of contact
     * @param phoneKey        Represents the field ID as String to obtain the phone values from the Json
     */
    private void buildPhones(final ConverterConfig converterConfig, final Contact contact, final JsonObject jsonObject,
                             final String phoneKey) {
        final Object value = jsonObject.opt(phoneKey);
        final Collection<Phone> existingPhones = contact.getPhones();
        final List<Phone> oldPhones = Objects.isNull(existingPhones) ? new ArrayList<>() : new ArrayList<>(existingPhones);
        final boolean isPartialData = converterConfig.isPartialData();

        if (value != null) {
            contact.setPhones(null);
            converterConfig.setPartialData(false);

            if (value instanceof final Map<?, ?> map) {
                final Phone phone = convertJsonToPhone(converterConfig, Json.object((Map<String, ?>) map), contact);
                final Phone existingPrimaryPhone = contact.getPrimaryPhone();

                if (Objects.nonNull(existingPrimaryPhone)) {
                    oldPhones.remove(existingPrimaryPhone);
                }

                oldPhones.add(phone);
                contact.setPrimaryPhone(phone);
                contact.setPhones(oldPhones);
            } else {
                final JsonArray phones = jsonObject.optJsonArray(phoneKey);
                boolean hasPrimaryPhone = false;

                if (Objects.nonNull(phones)) {
                    final Collection<Phone> newPhones = new ArrayList<>();

                    for (int i = 0; i < phones.size(); i++) {
                        final JsonObject phoneJson = phones.optJsonObject(i);

                        if (Objects.nonNull(phoneJson)) {

                            if (!hasPrimaryPhone && phoneJson.optBoolean(ContactFields.IS_PRIMARY, false)) {
                                hasPrimaryPhone = true;
                            }
                            newPhones.add(convertJsonToPhone(converterConfig, phoneJson, contact));
                        }
                    }

                    if (!newPhones.isEmpty()) {
                        contact.setPhones(newPhones);
                    }

                    if (!hasPrimaryPhone) {
                        final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                        contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
                    }
                }
            }
            converterConfig.setPartialData(isPartialData);

            if (Objects.isNull(contact.getPrimaryPhone())) {
                final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
            }
        } else if (isPartialData) {
            contact.setPhones(oldPhones);
        }
    }

    /**
     * Gets the primary phone
     *
     * @param phones Represents the {@link Collection<Phone>}
     * @return the primary phone
     */
    private Optional<Phone> getPrimaryPhone(final Collection<Phone> phones) {
        return Objects.isNull(phones) ? Optional.empty() : Optional.ofNullable(phones.stream().findFirst().orElseGet(() -> null));
    }

    /**
     * Gets the primary email
     *
     * @param emails Represents the {@link Collection<Email>}
     * @return the primary email
     */
    private Optional<Email> getPrimaryEmail(final Collection<Email> emails) {
        return Objects.isNull(emails) ? Optional.empty() : Optional.ofNullable(emails.stream().findFirst().orElseGet(() -> null));
    }

    /**
     * Sets {@link Email} details into {@link Contact}
     *
     * @param converterConfig Converter configuration
     * @param contact         An entity that holds the attributes of contact
     * @param jsonObject      Holds the field values of contact
     * @param emailKey        Represents the field ID as String to obtain the email address values from the Json
     */
    private void buildEmails(final ConverterConfig converterConfig, final Contact contact, final JsonObject jsonObject,
                             final String emailKey) {
        final Object value = jsonObject.opt(emailKey);
        final Collection<Email> existingEmails = contact.getEmails();
        final List<Email> oldEmails = Objects.isNull(existingEmails) ? new ArrayList<>() : new ArrayList<>(existingEmails);
        final boolean isPartialData = converterConfig.isPartialData();

        if (value != null) {
            contact.setEmails(null);
            converterConfig.setPartialData(false);

            if (value instanceof final Map<?, ?> map) {
                final Email email = convertJsonToEmail(converterConfig, Json.object((Map<String, ?>) map), contact);
                final Email existingPrimaryEmail = contact.getPrimaryEmail();

                if (Objects.nonNull(existingPrimaryEmail)) {
                    oldEmails.remove(existingPrimaryEmail);
                }

                oldEmails.add(email);
                contact.setPrimaryEmail(email);
                contact.setEmails(oldEmails);
            } else {
                final JsonArray emails = jsonObject.optJsonArray(emailKey);

                if (Objects.nonNull(emails) && !Objects.equals(emails.size(), NumericalConstant.ZERO)) {
                    final Collection<Email> newEmails = new ArrayList<>();

                    for (int i = 0; i < emails.size(); i++) {
                        final JsonObject emailJson = emails.optJsonObject(i);

                        if (Objects.nonNull(emailJson)) {
                            newEmails.add(convertJsonToEmail(converterConfig, emailJson, contact));
                        }
                    }

                    if (!newEmails.isEmpty()) {
                        contact.setEmails(newEmails);
                    }
                }
            }
            converterConfig.setPartialData(isPartialData);
            final Email primaryEmail = contact.getPrimaryEmail();
            final Collection<Email> contactEmails = contact.getEmails();

            if (Objects.isNull(primaryEmail) || Objects.isNull(contactEmails) || contactEmails.isEmpty()
                    || !contactEmails.contains(primaryEmail)) {
                contact.setPrimaryEmail(getPrimaryEmail(contactEmails).orElseGet(() -> null));
            }
        } else if (isPartialData) {
            contact.setEmails(oldEmails);
        }
    }

    /**
     * Converts a JSON object representing an email into an {@link Email} object.
     *
     * @param converterConfig Refers the configuration settings for the conversion process.
     * @param emailJson       Refers the JSON object containing the email data.
     * @param contact         Refers the contact associated with the email.
     * @return The constructed {@link Email}.
     */
    private Email convertJsonToEmail(final ConverterConfig converterConfig, final JsonObject emailJson, final Contact contact) {
        final Email email = emailConverter.convert(converterConfig, emailJson);
        final String primaryKey = ContactFields.IS_PRIMARY;

        if (emailJson.containsKey(primaryKey)) {
            final Email primaryEmail = contact.getPrimaryEmail();

            if (!Objects.equals(email, primaryEmail) && emailJson.getBoolean(primaryKey)) {
                contact.setPrimaryEmail(email);
            }
        }

        return email;
    }

    /**
     * Converts a JSON object representing an phone into an {@link Phone} object.
     *
     * @param converterConfig Refers the configuration settings for the conversion process.
     * @param phoneJson       Refers the JSON object containing the phone data.
     * @param contact         Refers the contact associated with the phone.
     * @return The constructed {@link Phone}.
     */
    private Phone convertJsonToPhone(final ConverterConfig converterConfig, final JsonObject phoneJson, final Contact contact) {
        final String primaryKey = ContactFields.IS_PRIMARY;
        final Phone phone = phoneConverter.convert(converterConfig, phoneJson);

        if (phoneJson.containsKey(primaryKey)) {
            final Phone primaryPhone = contact.getPrimaryPhone();

            if (!Objects.equals(phone, primaryPhone) && phoneJson.getBoolean(primaryKey)) {
                contact.setPrimaryPhone(phone);
            }
        }

        return phone;
    }

    /**
     * <p>
     * Converts {@link Contact} into {@link JsonObject}
     * </p>
     *
     * @param contact An entity that holds the attributes of contact details
     * @return {@link JsonObject} filled with the contact data
     */
    @Override
    public JsonObject convert(final Contact contact) {
        final JsonObject jsonObject = Json.object();
        final JsonArray emails = Json.array();
        final JsonArray phoneNumbers = Json.array();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
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
        final Option timeZone = contact.getTimeZone();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final SummaryInfo summaryInfo = contact.getSummaryInfo();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final Territory territory = contact.getTerritory();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final CrmUser salesOwner = contact.getSalesOwner();
        final DateTime createdTime = contact.getCreatedTime();

        final String lastNameAlias = ContactFields.LAST_NAME;
        final String companyAlias = ContactFields.COMPANY;
        final String lifecycleStageAlias = ContactFields.LIFECYCLE_STAGE;
        final String lifecycleStatusAlias = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonAlias = ContactFields.CONTACT_LOST_REASON;
        final String sourceAlias = ContactFields.SOURCE;
        final String addressAlias = ContactFields.ADDRESS;
        final String subscriptionStatusAlias = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesAlias = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonAlias = ContactFields.UNSUBSCRIBE_REASON;
        final String timezoneAlias = ContactFields.TIMEZONE;
        final String createdByAlias = ContactFields.CREATED_BY;
        final String updatedByAlias = ContactFields.UPDATED_BY;
        final String territoryAlias = ContactFields.TERRITORY;
        final String salesOwnerAlias = ContactFields.SALES_OWNER;
        final String designationAlias = ContactFields.DESIGNATION;
        final String departmentAlias = ContactFields.DEPARTMENT;
        final String facebookAlias = ContactFields.FACEBOOK;
        final String twitterAlias = ContactFields.TWITTER;
        final String linkedinAlias = ContactFields.LINKEDIN;
        final String dateOfBirthAlias = ContactFields.DATE_OF_BIRTH;
        final String otherUnsubscribeReasonAlias = ContactFields.OTHER_UNSUBSCRIBE_REASON;
        final String emailsAlias = ContactFields.EMAILS;
        final String phonesAlias = ContactFields.PHONES;
        final String tagsAlias = ContactFields.TAGS;
        final String createdAtAlias = ContactFields.CREATED_AT;

        jsonObject.put(idKey, contact.id());
        jsonObject.put(ContactFields.ORG_ID, contact.orgId());
        jsonObject.put(fields.get(ContactFields.FIRST_NAME).idAsString(), contact.getFirstName());

        if (fields.containsKey(lastNameAlias)) {
            jsonObject.put(fields.get(lastNameAlias).idAsString(), contact.getLastName());
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            jsonObject.put(fields.get(companyAlias).idAsString(), company.id());
        }

        if (Objects.nonNull(lifeCycleStage) && fields.containsKey(lifecycleStageAlias)) {
            jsonObject.put(fields.get(lifecycleStageAlias).idAsString(), lifeCycleStage.id());
        }

        if (Objects.nonNull(lifeCycleStatus) && fields.containsKey(lifecycleStatusAlias)) {
            jsonObject.put(fields.get(lifecycleStatusAlias).idAsString(), lifeCycleStatus.id());
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            jsonObject.put(fields.get(lostReasonAlias).idAsString(), lostReason.id());
        }

        if (Objects.nonNull(source) && fields.containsKey(sourceAlias)) {
            jsonObject.put(fields.get(sourceAlias).idAsString(), source.id());
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).idAsString(), addressConverter.convert(address));
        }

        if (Objects.nonNull(subscriptionStatus) && fields.containsKey(subscriptionStatusAlias)) {
            jsonObject.put(fields.get(subscriptionStatusAlias).idAsString(), subscriptionStatus.id());
        }

        if (Objects.nonNull(subscriptionTypes) && fields.containsKey(subscriptionTypesAlias)) {
            jsonObject.put(fields.get(subscriptionTypesAlias).idAsString(),
                    subscriptionTypes.stream().map(Option::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(unsubscribeReason) && fields.containsKey(unsubscribeReasonAlias)) {
            jsonObject.put(fields.get(unsubscribeReasonAlias).idAsString(), unsubscribeReason.id());
        }

        if (Objects.nonNull(timeZone) && fields.containsKey(timezoneAlias)) {
            jsonObject.put(fields.get(timezoneAlias).idAsString(), timeZone.id());
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).idAsString(), territory.id());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), salesOwner.id());
        }

        if (fields.containsKey(designationAlias)) {
            jsonObject.put(fields.get(designationAlias).idAsString(), contact.getDesignation());
        }

        if (fields.containsKey(departmentAlias)) {
            jsonObject.put(fields.get(departmentAlias).idAsString(), contact.getDepartment());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).idAsString(), contact.getFacebook());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).idAsString(), contact.getTwitter());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).idAsString(), contact.getLinkedIn());
        }

        if (fields.containsKey(dateOfBirthAlias)) {
            jsonObject.put(fields.get(dateOfBirthAlias).idAsString(), dateUtil.getTimeStamp(contact.getDateOfBirth()));
        }

        if (fields.containsKey(otherUnsubscribeReasonAlias)) {
            jsonObject.put(fields.get(otherUnsubscribeReasonAlias).idAsString(), contact.getOtherUnsubscribeReason());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject);
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty() && fields.containsKey(emailsAlias)) {

            if (Objects.isNull(contact.getPrimaryEmail())) {
                final Optional<Email> primaryEmail = getPrimaryEmail(contact.getEmails());

                contact.setPrimaryEmail(primaryEmail.orElseGet(() -> null));
            }

            for (final Email email : contactEmails) {
                final JsonObject emailJson = emailConverter.convertEmail(email);

                if (Objects.equals(contact.getPrimaryEmail(), email)) {
                    emailJson.put(ContactFields.IS_PRIMARY, true);
                }
                emails.put(emailJson);
            }
            jsonObject.put(fields.get(emailsAlias).idAsString(), emails);
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty() && fields.containsKey(phonesAlias)) {

            if (Objects.isNull(contact.getPrimaryPhone())) {
                final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
            }

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = phoneConverter.convertPhone(phone);

                if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                    phoneJson.put(ContactFields.IS_PRIMARY, true);
                }
                phoneNumbers.put(phoneJson);
            }
            jsonObject.put(fields.get(phonesAlias).idAsString(), phoneNumbers);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).idAsString(), tagConverter.convertTags(tags));
            }
        }

        jsonObject.put(ContactFields.TOTAL_OPEN_DEALS, contact.getDealCount());

        if (Objects.nonNull(createdTime) && fields.containsKey(createdAtAlias)) {
            jsonObject.put(fields.get(createdAtAlias).idAsString(), createdTime.getTimeStamp());
        }
        return jsonObject;
    }

    /**
     * Converts {@link Contact} into {@link JsonObject}
     *
     * @param contact         An entity that holds the attributes of contact details
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Contact contact) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(contact);
        } else {
            final Company company = contact.getCompany();
            final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
            final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
            final Option lostReason = contact.getLostReason();
            final Option contactSource = contact.getContactSource();
            final ValueHolder valueHolder = contact.getValueHolder();
            final Collection<Email> contactEmails = contact.getEmails();
            final Collection<Phone> contactPhones = contact.getPhones();
            final JsonArray emails = Json.array();
            final JsonArray phoneNumbers = Json.array();

            jsonObject.put(ContactFields.ID, contact.id());
            jsonObject.put(ContactFields.ORG_ID, contact.orgId());
            jsonObject.put(ContactFields.FIRST_NAME, contact.getFirstName());
            jsonObject.put(ContactFields.LAST_NAME, contact.getLastName());

            if (Objects.nonNull(company)) {
                jsonObject.put(ContactFields.COMPANY, company.id());
            }

            if (Objects.nonNull(lifeCycleStage)) {
                jsonObject.put(ContactFields.LIFECYCLE_STAGE, lifeCycleStage.id());
            }

            if (Objects.nonNull(lifeCycleStatus)) {
                jsonObject.put(ContactFields.LIFECYCLE_STATUS, lifeCycleStatus.id());
            }

            if (Objects.nonNull(lostReason)) {
                jsonObject.put(ContactFields.CONTACT_LOST_REASON, lostReason.id());
            }

            if (Objects.nonNull(contactSource)) {
                jsonObject.put(ContactFields.SOURCE, contactSource.id());
            }

            if (Objects.nonNull(valueHolder)) {
                valueHolder.mapToJson(jsonObject);
            }

            if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {

                if (Objects.isNull(contact.getPrimaryEmail())) {
                    final Optional<Email> primaryEmail = getPrimaryEmail(contact.getEmails());

                    contact.setPrimaryEmail(primaryEmail.orElseGet(() -> null));
                }

                for (final Email email : contactEmails) {
                    final JsonObject emailJson = emailConverter.convert(new ConverterConfig(false, false), email);

                    if (Objects.equals(contact.getPrimaryEmail(), email)) {
                        emailJson.put(ContactFields.IS_PRIMARY, true);
                    }
                    emails.put(emailJson);
                }
                jsonObject.put(ContactFields.EMAILS, emails);
            }

            if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {

                if (Objects.isNull(contact.getPrimaryPhone())) {
                    final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                    contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
                }

                for (final Phone phone : contactPhones) {
                    final JsonObject phoneJson = phoneConverter.convert(new ConverterConfig(false, false), phone);

                    if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                        phoneJson.put(ContactFields.IS_PRIMARY, true);
                    }
                    phoneNumbers.put(phoneJson);
                }
                jsonObject.put(ContactFields.PHONES, phoneNumbers);
            }
            jsonObject.put(ContactFields.TOTAL_OPEN_DEALS, contact.getDealCount());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Contact} into partial {@link JsonObject}
     *
     * @param contact An entity that holds the attributes of contact details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Contact contact) {
        final JsonObject contactJson = Json.object();

        if (Objects.nonNull(contact)) {
            contactJson.put(ContactFields.ID, contact.id());
            contactJson.put(ContactFields.FULL_NAME, contact.getFullName());
        }

        return contactJson;
    }

    /**
     * <p>
     * Converts the {@link Contact} to a compact {@link JsonObject} containing
     * the contact name, ID, email, and phone number.
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return the compact {@link JsonObject}
     */
    public JsonObject convertContactWithEmailAndPhone(final Contact contact) {
        final JsonObject contactJson = Json.object();
        final Email primaryEmail = contact.getPrimaryEmail();
        final Phone primaryPhone = contact.getPrimaryPhone();

        contactJson.put(ContactFields.NAME, contact.getFullName());
        contactJson.put(ContactFields.VALUE, contact.id());

        if (Objects.nonNull(primaryEmail)) {
            contactJson.put(ContactFields.EMAIL, primaryEmail.getValue());
        }

        if (Objects.nonNull(primaryPhone)) {
            contactJson.put(ContactFields.PHONE, primaryPhone.getNumber());
        }

        return contactJson;
    }

    /**
     * Converts duplicate contact into {@link JsonObject}
     *
     * @param contact An entity that holds the attributes of duplicate contact details
     * @return {@link JsonObject} of duplicate contact
     */
    public JsonObject convertDuplicateContactToJson(final Contact contact) {
        final JsonArray emails = Json.array();
        final JsonArray phoneNumbers = Json.array();
        final JsonObject jsonObject = convertPartialData(contact);
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final Company company = contact.getCompany();

        if (Objects.nonNull(contactPhones)) {

            for (final Phone phone : contactPhones) {
                phoneNumbers.put(phoneConverter.convert(new ConverterConfig(false, false), phone));
            }
        }

        if (Objects.nonNull(contactEmails)) {

            for (final Email email : contactEmails) {
                emails.put(emailConverter.convert(new ConverterConfig(false, false), email));
            }
        }

        if (Objects.nonNull(company)) {
            jsonObject.put(ContactFields.COMPANY, company.getName());
        }
        jsonObject.put(ContactFields.EMAILS, emails);
        jsonObject.put(ContactFields.PHONES, phoneNumbers);
        jsonObject.put(ContactFields.TOTAL_DEAL, contact.getDealCount());
        jsonObject.put(ContactFields.TOTAL_ACTIVITY, contact.getActivityCount());
        jsonObject.put(ContactFields.CREATED_TIME, dateTimeUtil.getTimeStamp(contact.getCreatedTime()));
        jsonObject.put(ContactFields.OWNER_ID, contact.getOwnerId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link Contact} into {@link JsonObject}
     * </p>
     *
     * @param primaryContact   An entity which holds the primary contact attributes
     * @param duplicateContact An entity which holds the duplicate contact attributes
     * @return {@link JsonObject}
     */
    public JsonObject convertContactToJson(final Contact primaryContact, final Contact duplicateContact) {
        final JsonObject jsonObject = convertPartialData(duplicateContact);
        final Collection<Email> duplicateContactEmails = duplicateContact.getEmails();
        final Collection<Phone> duplicateContactPhones = duplicateContact.getPhones();
        final Collection<Email> primaryContactEmails = primaryContact.getEmails();
        final Collection<Phone> primaryContactPhones = primaryContact.getPhones();
        final Collection<String> primaryEmails = Objects.isNull(primaryContactEmails) ? Collections.emptySet() : primaryContactEmails.stream()
                .filter(Objects::nonNull).map(Email::getValue).collect(Collectors.toUnmodifiableSet());
        final Collection<String> primaryPhones = Objects.isNull(primaryContactPhones) ? Collections.emptySet() : primaryContactPhones.stream()
                .filter(Objects::nonNull).map(Phone::getNumber).collect(Collectors.toUnmodifiableSet());
        final Collection<String> secondaryEmails = Objects.isNull(duplicateContactEmails) ? Collections.emptySet() : duplicateContactEmails.stream().filter(Objects::nonNull)
                .map(Email::getValue).filter(primaryEmails::contains).collect(Collectors.toUnmodifiableSet());
        final Collection<String> secondaryPhones = Objects.isNull(duplicateContactPhones) ? Collections.emptySet() : duplicateContactPhones.stream()
                .filter(Objects::nonNull).map(Phone::getNumber).filter(primaryPhones::contains).collect(Collectors.toUnmodifiableSet());
        final Long id = duplicateContact.id();
        final DisplayPicture thumbnail = duplicateContact.getThumbnail();

        if (!secondaryEmails.isEmpty()) {
            jsonObject.put(ContactFields.EMAILS, secondaryEmails);
        }

        if (!secondaryPhones.isEmpty()) {
            jsonObject.put(ContactFields.PHONES, secondaryPhones);
        }

        jsonObject.put(ContactFields.CREATED_TIME, dateTimeUtil.getTimeStamp(duplicateContact.getCreatedTime()));
        jsonObject.put(ContactFields.OWNER_ID, duplicateContact.getOwnerId());
        jsonObject.put(ContactFields.ID, id);
        jsonObject.put(ContactFields.ORG_ID, duplicateContact.orgId());

        if (Objects.nonNull(thumbnail)) {
            jsonObject.put(ContactFields.THUMBNAIL, convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts collection of phone contacts into {@link JsonObject}
     * </p>
     *
     * @param contact An entity that holds the attributes of contact details using duplicate phone
     * @return {@link JsonObject} of duplicate phone contacts
     */
    public JsonObject convertContactPhoneToJson(final Contact contact) {

        if (Objects.nonNull(contact)) {
            final JsonObject jsonObject = convertPartialData(contact);
            final Collection<Phone> contactPhones = contact.getPhones();
            final DisplayPicture thumbnail = contact.getThumbnail();

            if (contact.hasPhones()) {
                contactPhones.stream().map(Phone::getNumber).findFirst()
                        .ifPresent(phoneNumber -> jsonObject.put(ContactFields.PHONE, phoneNumber));
            }

            if (Objects.nonNull(thumbnail)) {
                jsonObject.put(ContactFields.THUMBNAIL, convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
            }

            return jsonObject;
        }

        return Json.object();
    }

    /**
     * <p>
     * Converts collection of email contacts into {@link JsonObject}
     * </p>
     *
     * @param contact An entity that holds the attributes of contact details using duplicate email
     * @return {@link JsonObject} of duplicate email contacts
     */
    public JsonObject convertContactEmailToJson(final Contact contact) {
        final JsonObject jsonObject = convertPartialData(contact);
        final Collection<Email> contactEmails = contact.getEmails();
        final DisplayPicture thumbnail = contact.getThumbnail();

        if (!contactEmails.isEmpty()) {
            jsonObject.put(ContactFields.EMAIL, contactEmails.stream().map(Email::getValue).findFirst().get());
        }

        if (Objects.nonNull(thumbnail)) {
            jsonObject.put(ContactFields.THUMBNAIL, convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject summary(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJsonObject(contact, converterConfig);
        final Email primaryEmail = contact.getPrimaryEmail();
        final Phone primaryPhone = contact.getPrimaryPhone();

        if (Objects.nonNull(primaryPhone)) {
            final JsonObject phoneJson = Json.object();

            phoneJson.put(ContactFields.ID, primaryPhone.id());
            phoneJson.put(ContactFields.NUMBER, primaryPhone.getNumberWithCode());

            profileJson.put(ContactFields.PRIMARY_PHONE, phoneJson);
        }

        if (Objects.nonNull(primaryEmail)) {
            final JsonObject emailJson = Json.object();

            emailJson.put(ContactFields.ID, primaryEmail.id());
            emailJson.put(ContactFields.TYPE, primaryEmail.getType());
            emailJson.put(ContactFields.VALUE, primaryEmail.getValue());

            profileJson.put(ContactFields.PRIMARY_EMAIL, emailJson);
        }
        final DisplayPicture thumbnail = contact.getThumbnail();

        if (Objects.nonNull(thumbnail)) {
            profileJson.put(ContactFields.THUMBNAIL, convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
        }
        json.put(ContactFields.ID, contact.id());
        json.put(ContactFields.PROFILE, profileJson);
        json.put(ContactFields.SUMMARY, convertSummaryToJsonArray(contact, converterConfig));
        convertSubFieldToJson(contact, converterConfig, json);

        if (converterConfig.isAccessConfigRequired()) {
            json.put(FieldKeys.IS_EDITABLE.getName(), contact.isEditable());
            json.put(FieldKeys.IS_DELETABLE.getName(), contact.isDeletable());
        }

        return json;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact         Represents the {@link Contact}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing contact summary details
     */
    @Override
    public JsonObject summaryV2(final Long orgId, final Long userId, final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject contactSummaryJson = Json.object();

        contactSummaryJson.put(ContactFields.ID, contact.id());
        loadSummaryFieldPreference(orgId, userId, Source.CONTACT.getId(), contactSummaryJson);
        loadSummaryFields(contact, contactSummaryJson, converterConfig);
        convertSubFieldToJson(contact, converterConfig, contactSummaryJson);

        if (converterConfig.isAccessConfigRequired()) {
            contactSummaryJson.put(FieldKeys.IS_EDITABLE.getName(), contact.isEditable());
            contactSummaryJson.put(FieldKeys.IS_DELETABLE.getName(), contact.isDeletable());
        }
        return contactSummaryJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact         Represents the {@link Contact}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing contact summary details
     */
    @Override
    public JsonObject profile(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject profileJson = convertProfileToJsonObject(contact, converterConfig);
        final Email primaryEmail = contact.getPrimaryEmail();
        final Phone primaryPhone = contact.getPrimaryPhone();

        if (Objects.nonNull(primaryPhone)) {
            final JsonObject phoneJson = Json.object();

            phoneJson.put(ContactFields.ID, primaryPhone.id());
            phoneJson.put(ContactFields.NUMBER, primaryPhone.getNumberWithCode());

            profileJson.put(ContactFields.PRIMARY_PHONE, phoneJson);
        }

        if (Objects.nonNull(primaryEmail)) {
            final JsonObject emailJson = Json.object();

            emailJson.put(ContactFields.ID, primaryEmail.id());
            emailJson.put(ContactFields.TYPE, primaryEmail.getType());
            emailJson.put(ContactFields.VALUE, primaryEmail.getValue());

            profileJson.put(ContactFields.PRIMARY_EMAIL, emailJson);
        }
        final DisplayPicture thumbnail = contact.getThumbnail();
        final DisplayPicture original = contact.getOriginalPicture();

        if (Objects.nonNull(thumbnail) && Objects.nonNull(original)) {
            profileJson.put(FieldKeys.PROFILE_PIC.getName(), convertProfilePictureToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail, original));
        }
        profileJson.put(ContactFields.ID, contact.id());
        convertSubFieldToJson(contact, converterConfig, profileJson);

        if (converterConfig.isAccessConfigRequired()) {
            profileJson.put(FieldKeys.IS_EDITABLE.getName(), contact.isEditable());
            profileJson.put(FieldKeys.IS_DELETABLE.getName(), contact.isDeletable());
        }
        return profileJson;
    }

    /**
     * <p>
     * Converts the fields into json object
     * </p>
     *
     * @param fieldInfo Represents a data carrier that holds field details
     * @param contact   A model that represents contact
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertFieldInfoToJsonObject(final FieldInfo fieldInfo, final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        this.dealConverter = Objects.isNull(dealConverter) ? DealConverter.getInstance() : dealConverter;

        if (fieldInfo.isPersistent()) {
            setValuesToJson(contact.getValueHolder().get(fieldInfo.id()), jsonObject, fieldInfo);
        } else {
            switch (fieldInfo.aliasName()) {
                case "emails" -> {
                    final Collection<Email> contactEmails = contact.getEmails();
                    final JsonArray emails = Json.array();

                    if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {
                        final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAILS);
                        final Email primaryEmail = contact.getPrimaryEmail();
                        final String idKey = FieldKeys.ID.getName();
                        final String primaryKey = ContactFields.IS_PRIMARY;

                        if (Objects.nonNull(primaryEmail)) {
                            final JsonObject json = Json.object()
                                    .put(idKey, primaryEmail.id())
                                    .put(primaryKey, true);

                            subTypes.forEach((key, value) -> json.put(key.getName(), convertSubType(primaryEmail, key, value)));
                            emails.put(json);
                        }

                        for (final Email email : contactEmails) {

                            if (!Objects.equals(primaryEmail, email)) {
                                final JsonObject json = Json.object();

                                json.put(idKey, email.id());
                                subTypes.forEach((key, value) -> json.put(key.getName(), convertSubType(email, key, value)));
                                emails.put(json);
                            }
                        }
                        jsonObject.put(FieldKeys.VALUE.getName(), emails);
                    }
                }
                case "contactSource" -> {
                    final Option contactSource = contact.getContactSource();

                    if (Objects.nonNull(contactSource)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(contactSource, converterConfig));
                    }
                }
                case "lifeCycleStage" -> {
                    final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();

                    if (Objects.nonNull(lifeCycleStage)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), lifeCycleStageConverter.convert(lifeCycleStage));
                    }
                }
                case "lifeCycleStatus" -> {
                    final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();

                    if (Objects.nonNull(lifeCycleStatus)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), lifeCycleStatusConverter.convert(lifeCycleStatus));
                    }
                }
                case "contactLostReason" -> {
                    final Option lostReason = contact.getLostReason();

                    if (Objects.nonNull(lostReason)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), optionConverter.convert(lostReason));
                    }
                }
                case "phones" -> {
                    final Collection<Phone> contactPhones = contact.getPhones();
                    final JsonArray phoneNumbers = Json.array();

                    if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {
                        final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES);
                        final Phone primaryPhone = contact.getPrimaryPhone();
                        final String idKey = FieldKeys.ID.getName();
                        final String primaryKey = ContactFields.IS_PRIMARY;

                        if (Objects.nonNull(primaryPhone)) {
                            final JsonObject primaryJson = Json.object()
                                    .put(idKey, primaryPhone.id())
                                    .put(primaryKey, true);

                            subTypes.forEach((key, value) -> primaryJson.put(key.getName(), convertSubType(primaryPhone, key, value)));
                            phoneNumbers.put(primaryJson);
                        }

                        for (final Phone phone : contactPhones) {

                            if (!Objects.equals(primaryPhone, phone)) {
                                final JsonObject json = Json.object();

                                json.put(idKey, phone.id());

                                subTypes.forEach((key, value) -> json.put(key.getName(), convertSubType(phone, key, value)));
                                phoneNumbers.put(json);
                            }
                        }
                        jsonObject.put(FieldKeys.VALUE.getName(), phoneNumbers);
                    }
                }
                case "firstName" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getFirstName());
                case "lastName" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getLastName());
                case "company" -> {
                    final Company company = contact.getCompany();

                    if (Objects.nonNull(company)) {
                        final JsonObject json = Json.object();

                        json.put(ContactFields.ID, company.id());
                        json.put(ContactFields.NAME, company.getName());
                        json.put(ContactFields.WEBSITE, company.getWebsite());
                        jsonObject.put(FieldKeys.VALUE.getName(), json);
                    }
                }
                case "twitter" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getTwitter());
                case "facebook" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getFacebook());
                case "linkedIn" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getLinkedIn());
                case "dateOfBirth" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(contact.getDateOfBirth()));
                case "address" -> {
                    final Address address = contact.getAddress();

                    if (Objects.nonNull(address)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.ADDRESS, Collections.singleton(address)));
                    }
                }
                case "subscriptionTypes" -> {
                    final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();

                    if (Objects.nonNull(subscriptionTypes)) {
                        final JsonArray jsonArray = Json.array();

                        for (final Option subscriptionType : subscriptionTypes) {
                            jsonArray.put(optionConverter.convertStaticOptionToJson(subscriptionType, converterConfig.locale()));
                        }
                        jsonObject.put(FieldKeys.VALUE.getName(), jsonArray);
                    }
                }
                case "subscriptionStatus" -> {
                    final Option subscriptionStatus = contact.getSubscriptionStatus();

                    if (Objects.nonNull(subscriptionStatus)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), optionConverter.convertStaticOptionToJson(subscriptionStatus, converterConfig.locale()));
                    }
                }
                case "timeZone" -> {
                    final Option timeZone = contact.getTimeZone();

                    if (Objects.nonNull(timeZone)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), optionConverter.convertStaticOptionToJson(timeZone, converterConfig.locale()));
                    }
                }
                case "department" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getDepartment());
                case "designation" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getDesignation());
                case "lastActivityDate" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(contact.getSummaryInfo().getActivitySummary().getLastActivityDate()));
                case "nextActivityDate" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(contact.getSummaryInfo().getActivitySummary().getNextActivityDate()));
                case "totalActivities" -> {
                    final long totalActivities = contact.getSummaryInfo().getActivitySummary().getTotalActivities();

                    if (totalActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), totalActivities);
                    }
                }
                case "upcomingActivities" -> {
                    final long upcomingActivities = contact.getSummaryInfo().getActivitySummary().getUpcomingActivities();

                    if (upcomingActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), upcomingActivities);
                    }
                }
                case "doneActivities" -> {
                    final long doneActivities = contact.getSummaryInfo().getActivitySummary().getDoneActivities();

                    if (doneActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), doneActivities);
                    }
                }
                case "lastActivityType" -> {
                    final String lastActivityType = contact.getSummaryInfo().getActivitySummary().getLastActivityType();

                    if (Objects.nonNull(lastActivityType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), lastActivityType);
                    }
                }
                case "id" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.id());
                case "deals" ->
                        dealConverter.loadSummaryInfoToJson(contact.getSummaryInfo(), Deal.State.OPEN.getName(), jsonObject);
                case "wonDeals" ->
                        dealConverter.loadSummaryInfoToJson(contact.getSummaryInfo(), Deal.State.WON.getName(), jsonObject);
                case "lostDeals" ->
                        dealConverter.loadSummaryInfoToJson(contact.getSummaryInfo(), Deal.State.LOST.getName(), jsonObject);
                case "closedDeals" ->
                        dealConverter.loadSummaryInfoToJson(contact.getSummaryInfo(), DealFields.CLOSED, jsonObject);
                case "webForms" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getSummaryInfo().getWebFormName());
                case "territory" -> {
                    final Territory territory = contact.getTerritory();

                    if (Objects.nonNull(territory)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), territoryConverter.convertTerritoryToJson(territory));
                    }
                }
                case "salesOwner" -> {
                    final CrmUser salesOwner = contact.getSalesOwner();

                    if (Objects.nonNull(salesOwner)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(salesOwner, true, true));
                    }
                }
                case "createdBy" -> {
                    final CrmUser createdBy = contact.getCreatedBy();

                    if (Objects.nonNull(createdBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(createdBy, true, true));
                    }
                }
                case "updatedBy" -> {
                    final CrmUser updatedBy = contact.getUpdatedBy();

                    if (Objects.nonNull(updatedBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(updatedBy, true, true));
                    }
                }
                case "tags" -> {
                    final Collection<Tag> tags = contact.getSummaryInfo().getTags();

                    if (Objects.nonNull(tags) && !tags.isEmpty()) {
                        jsonObject.put(FieldKeys.VALUE.getName(), tagConverter.convertTagsForSummary(tags));
                    }
                }
                case "emailMessageCount" -> {
                    final EmailSummary emailSummary = contact.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Long emailMessageCount = emailSummary.getEmailMessageCount();

                        if (Objects.nonNull(emailMessageCount) && emailMessageCount > 0) {
                            jsonObject.put(FieldKeys.VALUE.getName(), emailMessageCount);
                        }
                    }
                }
                case "lastEmailSent" -> {
                    final EmailSummary emailSummary = contact.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailSent = emailSummary.getLastEmailSent();

                        if (Objects.nonNull(lastEmailSent)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailSent.getTimeStamp());
                        }
                    }
                }
                case "lastEmailReceived" -> {
                    final EmailSummary emailSummary = contact.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailReceived = emailSummary.getLastEmailReceived();

                        if (Objects.nonNull(lastEmailReceived)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailReceived.getTimeStamp());
                        }
                    }
                }
                case "updatedAt" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(contact.getModifiedTime()));
                case "createdAt" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(contact.getCreatedTime()));
                case "lastAssignedAt" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(contact.getLastAssignedAt()));
                case "recentNote" -> jsonObject.put(FieldKeys.VALUE.getName(), contact.getRecentNote());
                case "unsubscribeReason" -> {
                    final Option unsubscribedReason = contact.getUnsubscribeReason();

                    if (Objects.nonNull(unsubscribedReason)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(unsubscribedReason, converterConfig));
                    }
                }
                case "otherUnsubscribeReason" -> {
                    final String otherUnsubscribeReason = contact.getOtherUnsubscribeReason();

                    if (Objects.nonNull(otherUnsubscribeReason)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), otherUnsubscribeReason);
                    }
                }
                case "marketingList" -> {
                    final Collection<MarketingList> marketingLists = contact.getMarketingLists();

                    if (Objects.nonNull(marketingLists) && !marketingLists.isEmpty()) {
                        jsonObject.put(FieldKeys.VALUE.getName(), marketingListConverter.convertForSummary(marketingLists));
                    }
                }
            }
        }
        loadFieldsIntoJson(jsonObject, fieldInfo, converterConfig);

        return jsonObject;
    }

    /**
     * Converts the primary contact to {@link JsonObject} for merge preview/get APIs.
     *
     * @param contact      The primary {@link Contact} entity
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return {@link JsonObject} representation of the contact
     */
    public JsonObject convertPrimaryContactToJson(final Contact contact, final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = contact.getValueHolder();
        final Company company = contact.getCompany();
        final Option source = contact.getContactSource();
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final Option lostReason = contact.getLostReason();
        final Address address = contact.getAddress();
        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
        final Option timeZone = contact.getTimeZone();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final Territory territory = contact.getTerritory();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final CrmUser salesOwner = contact.getSalesOwner();
        final Date dateOfBirth = contact.getDateOfBirth();
        final SummaryInfo summaryInfo = contact.getSummaryInfo();
        final ActivitySummary activitySummary = Objects.nonNull(summaryInfo) ? summaryInfo.getActivitySummary() : null;
        final EmailSummary emailSummary = Objects.nonNull(summaryInfo) ? summaryInfo.getEmailSummary() : null;

        jsonObject.put(ContactFields.ID, contact.id());
        jsonObject.put(ContactFields.ORG_ID, contact.orgId());
        jsonObject.put(ContactFields.FIRST_NAME, contact.getFirstName());
        jsonObject.put(ContactFields.LAST_NAME, contact.getLastName());
        jsonObject.put(ContactFields.FULL_NAME, contact.getFullName());
        jsonObject.put(ContactFields.DESIGNATION, contact.getDesignation());
        jsonObject.put(ContactFields.DEPARTMENT, contact.getDepartment());
        jsonObject.put(ContactFields.FACEBOOK, contact.getFacebook());
        jsonObject.put(ContactFields.TWITTER, contact.getTwitter());
        jsonObject.put(ContactFields.LINKEDIN, contact.getLinkedIn());
        jsonObject.put(ContactFields.CREATED_AT, dateTimeUtil.getTimeStamp(contact.getCreatedTime()));
        jsonObject.put(ContactFields.UPDATED_AT, dateTimeUtil.getTimeStamp(contact.getModifiedTime()));
        jsonObject.put(ContactFields.LAST_ASSIGNED_AT, dateTimeUtil.getTimeStamp(contact.getLastAssignedAt()));
        jsonObject.put(ContactFields.CREATED_BY, createdBy.getFullName());
        jsonObject.put(ContactFields.UPDATED_BY, updatedBy.getFullName());
        jsonObject.put(ContactFields.SALES_OWNER, salesOwner.getFullName());
        jsonObject.put(ContactFields.RECENT_NOTE, contact.getRecentNote());

        if (Objects.nonNull(dateOfBirth)) {
            jsonObject.put(ContactFields.DATE_OF_BIRTH, dateOfBirth.getTimeStamp());
        }

        if (Objects.nonNull(source)) {
            jsonObject.put(ContactFields.CONTACT_SOURCE, i18nResolver.get(source.getOption()));
        }

        if (Objects.nonNull(lifeCycleStage)) {
            jsonObject.put(ContactFields.LIFECYCLE_STAGE, lifeCycleStage.getName());
        }

        if (Objects.nonNull(lifeCycleStatus)) {
            jsonObject.put(ContactFields.LIFECYCLE_STATUS, lifeCycleStatus.getName());
        }

        if (Objects.nonNull(lostReason)) {
            jsonObject.put(ContactFields.CONTACT_LOST_REASON, lostReason.getOption());
        }

        if (Objects.nonNull(address)) {
            jsonObject.put(ContactFields.ADDRESS, addressConverter.convert(address));
        }

        if (Objects.nonNull(company)) {
            final JsonObject companyJson = Json.object();

            companyJson.put(CompanyFields.ID, company.id());
            companyJson.put(CompanyFields.NAME, company.getName());
            jsonObject.put(ContactFields.COMPANY, companyJson);
        }

        if (Objects.nonNull(contact.getPrimaryEmail())) {
            jsonObject.put(ContactFields.PRIMARY_EMAIL, contact.getPrimaryEmail().getValue());
        }

        if (Objects.nonNull(contact.getPrimaryPhone())) {
            jsonObject.put(ContactFields.PRIMARY_PHONE, contact.getPrimaryPhone().getNumber());
        }

        if (Objects.nonNull(contact.getEmails())) {
            final JsonArray emailsJson = Json.array();

            for (final Email email : contact.getEmails()) {
                emailsJson.put(email.getValue());
            }
            jsonObject.put(ContactFields.EMAILS, emailsJson);
        }

        if (Objects.nonNull(contact.getPhones())) {
            final JsonArray phonesJson = Json.array();

            for (final Phone phone : contact.getPhones()) {
                phonesJson.put(phone.getNumber());
            }
            jsonObject.put(ContactFields.PHONES, phonesJson);
        }

        if (Objects.nonNull(subscriptionStatus)) {
            jsonObject.put(ContactFields.SUBSCRIPTION_STATUS, i18nResolver.get(subscriptionStatus.getOption()));
        }

        if (Objects.nonNull(subscriptionTypes)) {
            final JsonArray typesJson = Json.array();

            for (final Option subscriptionType : subscriptionTypes) {
                typesJson.put(i18nResolver.get(subscriptionType.getOption()));
            }
            jsonObject.put(ContactFields.SUBSCRIPTION_TYPES, typesJson);
        }

        if (Objects.nonNull(timeZone)) {
            jsonObject.put(ContactFields.TIMEZONE, i18nResolver.get(timeZone.getOption()));
        }

        if (Objects.nonNull(unsubscribeReason)) {
            jsonObject.put(ContactFields.UNSUBSCRIBE_REASON, i18nResolver.get(unsubscribeReason.getOption()));
            jsonObject.put(ContactFields.OTHER_UNSUBSCRIBE_REASON, contact.getOtherUnsubscribeReason());
        }

        if (Objects.nonNull(territory)) {
            jsonObject.put(ContactFields.TERRITORY, territory.getName());
        }

        if (Objects.nonNull(emailSummary)) {
            loadEmailSummaryFields(jsonObject,
                    DealFields.LAST_EMAIL_SENT, dateUtil.getTimeStamp(emailSummary.getLastEmailSent()),
                    DealFields.EMAIL_MESSAGE_COUNT, emailSummary.getEmailMessageCount(),
                    DealFields.LAST_EMAIL_RECEIVED, dateUtil.getTimeStamp(emailSummary.getLastEmailReceived()));
        }

        if (Objects.nonNull(activitySummary)) {
            jsonObject.put(DealFields.NEXT_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getNextActivityDate()));
            jsonObject.put(DealFields.LAST_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getLastActivityDate()));
            jsonObject.put(DealFields.TOTAL_ACTIVITIES, activitySummary.getTotalActivities());
            jsonObject.put(DealFields.LAST_ACTIVITY_TYPE, activitySummary.getLastActivityType());
            jsonObject.put(DealFields.UPCOMING_ACTIVITIES, activitySummary.getUpcomingActivities());
            jsonObject.put(DealFields.DONE_ACTIVITIES, activitySummary.getDoneActivities());
        }

        final JsonArray tagsJsonArray = Json.array();

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags)) {
                for (final Tag tag : tags) {
                    final JsonObject tagJson = Json.object();

                    tagJson.put(TagFields.ID.getName(), tag.id());
                    tagJson.put(TagFields.TAG.getName(), tag.getName());
                    tagsJsonArray.put(tagJson);
                }
            }

            this.dealConverter = Objects.isNull(dealConverter) ? DealConverter.getInstance() : dealConverter;
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.OPEN.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.WON.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.LOST.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, DealFields.CLOSED, jsonObject);
        }
        jsonObject.put(ContactFields.TAGS, tagsJsonArray);

        final JsonArray marketingListsJsonArray = Json.array();
        final Collection<MarketingList> marketingLists = contact.getMarketingLists();

        if (Objects.nonNull(marketingLists)) {
            for (final MarketingList marketingList : marketingLists) {
                final JsonObject marketingListJson = Json.object();

                marketingListJson.put(MarketingListFields.ID, marketingList.id());
                marketingListJson.put(MarketingListFields.NAME, marketingList.getName());
                marketingListsJsonArray.put(marketingListJson);
            }
        }
        jsonObject.put(ContactFields.MARKETING_LIST, marketingListsJsonArray);

        loadValueHolderToJson(jsonObject, valueHolder);
        loadPersistentFieldValues(contact.orgId(), Contact.ENTITY_TYPE, jsonObject);

        return jsonObject;
    }

    /**
     * Maps COMPANY and CONTACT custom-field references for contact merge/preview responses.
     */
    @Override
    protected JsonObject convertEntityToJson(final StoreType storeType, final TlcModel model) {
        return switch (storeType) {
            case COMPANY -> {
                final JsonObject companyJson = Json.object();
                final Company company = (Company) model;

                companyJson.put(FieldKeys.ID.getName(), company.id());
                companyJson.put(FieldKeys.NAME.getName(), company.getName());

                yield companyJson;
            }
            case CONTACT -> {
                final JsonObject contactJson = Json.object();
                final Contact contact = (Contact) model;

                contactJson.put(FieldKeys.ID.getName(), contact.id());
                contactJson.put(FieldKeys.NAME.getName(), contact.getFullName());

                yield contactJson;
            }
            default -> null;
        };
    }

    /**
     * Converts the duplicate contact into {@link JsonObject}.
     *
     * @param duplicateContact Holds the attributes of a duplicate contact
     * @param contact          Holds the attributes of a {@link Contact} payload from request
     * @return {@link JsonObject} of contact
     */
    public JsonObject convertDuplicateContactToJson(final Contact duplicateContact, final Contact contact) {
        final JsonObject duplicateDetails = Json.object();
        final Set<String> emptySet = Collections.emptySet();
        final JsonObject jsonObject = convertPartialData(duplicateContact);
        final Email duplicateContactPrimaryEmail = duplicateContact.getPrimaryEmail();
        final Phone duplicateContactPrimaryPhone = duplicateContact.getPrimaryPhone();
        final Company duplicateContactCompany = duplicateContact.getCompany();
        final Collection<Email> duplicateContactEmails = duplicateContact.getEmails();
        final Set<String> duplicateEmails = Objects.isNull(duplicateContactEmails) ? Collections.emptySet() :
                duplicateContactEmails.stream().map(Email::getValue).collect(Collectors.toSet());
        final Collection<Email> contactEmails = contact.getEmails();
        final Set<String> emails = Objects.isNull(contactEmails) ? Collections.emptySet() : contactEmails.stream().map(
                Email::getValue).collect(Collectors.toSet());
        final Collection<Phone> duplicateContactPhones = duplicateContact.getPhones();
        final Set<String> duplicatePhones = Objects.isNull(duplicateContactPhones) ? emptySet : duplicateContactPhones.stream()
                .map(Phone::getNumber).collect(Collectors.toSet());
        final Collection<Phone> contactPhones = contact.getPhones();
        final Set<String> phones = Objects.isNull(contactPhones) ? emptySet : contactPhones.stream().map(
                Phone::getNumber).filter(duplicatePhones::contains).collect(Collectors.toSet());
        final Collection<String> matchedEmails = emails.isEmpty() ? Collections.emptySet() :
                StringUtil.getMatchedCaseInsensitiveStrings(duplicateEmails, emails);

        if (Objects.nonNull(duplicateContactPrimaryEmail)) {
            jsonObject.put(ContactFields.PRIMARY_EMAIL, duplicateContactPrimaryEmail.getValue());
        }

        if (Objects.nonNull(duplicateContactPrimaryPhone)) {
            jsonObject.put(ContactFields.PRIMARY_PHONE, duplicateContactPrimaryPhone.getNumber());
        }

        if (Objects.nonNull(duplicateContactCompany)) {
            jsonObject.put(ContactFields.COMPANY, duplicateContactCompany.getName());
        }

        if (!duplicateEmails.isEmpty()) {
            jsonObject.put(ContactFields.EMAILS, duplicateEmails);
        } else if (!matchedEmails.isEmpty()) {
            duplicateDetails.put(ContactFields.EMAILS, matchedEmails);
        }

        if (!duplicatePhones.isEmpty()) {
            jsonObject.put(ContactFields.PHONES, duplicatePhones);
        } else if (!phones.isEmpty()) {
            duplicateDetails.put(ContactFields.PHONES, phones);
        }

        duplicateDetails.put(ContactFields.TOTAL_MATCHES, phones.size() + matchedEmails.size());
        jsonObject.put(ContactFields.ORG_ID, duplicateContact.orgId());
        jsonObject.put(ContactFields.MATCHES, duplicateDetails);

        return jsonObject;
    }

    /**
     * <p>
     * Converts the related contact into {@link JsonObject}
     * </P>
     *
     * @param contact         Holds the {@link Contact} attributes
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject}
     */
    public JsonObject convertRelatedContactToJson(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Collection<Email> emails = contact.getEmails();
        final Collection<Phone> phones = contact.getPhones();
        final Long contactId = contact.id();
        final JsonArray emailArray = Json.array();
        final JsonArray phoneArray = Json.array();
        final boolean hasEmails = Objects.nonNull(emails);
        final boolean hasPhones = Objects.nonNull(phones);

        jsonObject.put(ContactFields.ID, contactId);
        jsonObject.put(ContactFields.ORG_ID, contact.orgId());
        jsonObject.put(ContactFields.FULL_NAME, contact.getFullName());
        jsonObject.put(ContactFields.DESIGNATION, contact.getDesignation());
        jsonObject.put(ContactFields.COMPANY, companyConverter.convertPartialData(contact.getCompany()));

        if (converterConfig.isMobileClient()) {
            final DisplayPicture displayPicture = contact.getThumbnail();

            if (Objects.nonNull(displayPicture)) {
                jsonObject.put(ContactFields.THUMBNAIL, companyConverter.convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, displayPicture));
            }

            if (hasEmails) {
                for (final Email email : emails) {
                    final JsonObject emailJson = emailConverter.convert(new ConverterConfig(false, false), email);

                    if (Objects.equals(contact.getPrimaryEmail(), email)) {
                        emailJson.put(ContactFields.IS_PRIMARY, true);
                    }
                    emailArray.put(emailJson);
                }
            }

            if (emailArray.size() > 0) {
                jsonObject.put(ContactFields.EMAILS, emailArray);
            }

            if (hasPhones) {
                for (final Phone phone : phones) {
                    final JsonObject phoneJson = phoneConverter.convertPhone(phone);

                    if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                        phoneJson.put(ContactFields.IS_PRIMARY, true);
                    }
                    phoneArray.put(phoneJson);
                }
            }

            if (phoneArray.size() > 0) {
                jsonObject.put(ContactFields.PHONES, phoneArray);
            }
        } else {
            if (hasEmails && !emails.isEmpty()) {
                jsonObject.put(ContactFields.EMAILS, emails);
            }

            if (hasPhones && !phones.isEmpty()) {
                jsonObject.put(ContactFields.PHONES, phones);
            }
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the collection of {@link Contact} into {@link JsonArray}
     * </p>
     *
     * @param contacts Represents the collection of {@link Contact}
     * @return {@link JsonArray} of contacts.
     */
    public JsonArray convertContactsToJsonArray(final Collection<Contact> contacts) {
        final JsonArray jsonArray = Json.array();

        for (final Contact contact : contacts) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(ContactFields.NAME, contact.getFullName());
            jsonObject.put(ContactFields.ID, contact.id());
            jsonObject.put(ContactFields.EMAIL, contact.getPrimaryEmail().getValue());
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Builds the contact by partial data.
     * </p>
     *
     * @param orgId             Represents an organization id.
     * @param emailMap          Represents the collection of email values.
     * @param lifeCycleStageId  Represents the id of the {@link LifeCycleStage}
     * @param lifeCycleStatusId Represents the id of the {@link LifeCycleStatus}.
     * @return Collection of {@link Contact}
     */
    public ContactRecipients buildContactsFromEmailAddresses(final long orgId, final Long ownerId, final Map<AddressType, Collection<String>> emailMap,
                                                             final Long lifeCycleStageId, final Long lifeCycleStatusId, final EntityEmail entityEmail) {
        final ContactRecipients existingContactRecipients = entityEmail.getContactRecipients();
        final ContactRecipients recipients = new ContactRecipients();
        final ContactRecipients contactRecipients = Objects.isNull(existingContactRecipients) ? new ContactRecipients() : existingContactRecipients;
        final CrmUser crmUser = userService.getUser(orgId, ownerId);
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, ContactFields.SUBSCRIPTION_STATUS,
                ContactFields.SUBSCRIPTION_TYPES, ContactFields.UNSUBSCRIBE_REASON);
        final BiConsumer<AddressType, Collection<String>> getContacts = (addressType, emails) -> {

            if (Objects.nonNull(emails) && !emails.isEmpty()) {

                for (final String email : emails) {
                    final Contact contact = new Contact();
                    final Email contactEmail = new Email();
                    final LifeCycleStage lifeCycleStage = new LifeCycleStage();
                    final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();
                    final Option subscriptionStatus = optionService.getDefaultOption(orgId, fields.get(ContactFields.SUBSCRIPTION_STATUS));
                    final String subscriptionStatusOption = subscriptionStatus.getOption();

                    contactEmail.setOrgId(orgId);
                    contactEmail.setType(Email.Type.WORK);
                    contactEmail.setValue(email);

                    lifeCycleStage.setId(lifeCycleStageId);
                    lifeCycleStatus.setId(lifeCycleStatusId);

                    contact.setFirstName(email.split("@")[0]);
                    contact.addEmail(contactEmail);
                    contact.setLifeCycleStage(lifeCycleStage);
                    contact.setLifeCycleStatus(lifeCycleStatus);
                    contact.setOrgId(orgId);
                    contact.setOwnerId(ownerId);
                    contact.setCreatedBy(crmUser);
                    contact.setSalesOwner(crmUser);
                    contact.setSubscriptionStatus(subscriptionStatus);

                    if (Objects.equals(subscriptionStatusOption, Contact.SubscriptionStatus.SUBSCRIBED.getName())) {
                        contact.setSubscriptionTypes(optionService.getDefaultOptions(orgId, fields.get(ContactFields.SUBSCRIPTION_TYPES)));
                    } else if (Objects.equals(subscriptionStatusOption, Contact.SubscriptionStatus.UNSUBSCRIBED.getName())) {
                        contact.setUnsubscribeReason(optionService.getDefaultOption(orgId, fields.get(ContactFields.UNSUBSCRIBE_REASON)));
                    }
                    contactRecipients.addContact(addressType, contact);
                    recipients.addContact(addressType, contact);
                }
            }
        };

        getContacts.accept(AddressType.FROM, emailMap.get(AddressType.FROM));
        getContacts.accept(AddressType.TO, emailMap.get(AddressType.TO));
        getContacts.accept(AddressType.BCC, emailMap.get(AddressType.BCC));
        getContacts.accept(AddressType.CC, emailMap.get(AddressType.CC));

        entityEmail.setContactRecipients(contactRecipients);

        return recipients;
    }

    /**
     * <p>
     * Builds the contact by partial data.
     * </p>
     *
     * @param orgId             Represents an organization id.
     * @param userId            Represents crm user id.
     * @param phoneNumber       Represents the phone number of the contact.
     * @param lifeCycleStageId  Represents the id of the {@link LifeCycleStage}
     * @param lifeCycleStatusId Represents the id of the {@link LifeCycleStatus}.
     * @return Collection of {@link Contact}
     */
    public Contact buildContactFromPhoneNumber(final Long orgId, final Long userId, final String phoneNumber,
                                               final Long lifeCycleStageId, final Long lifeCycleStatusId) {
        final Contact contact = new Contact();
        final CrmUser crmUser = userService.getUser(orgId, userId);
        final LifeCycleStage lifeCycleStage = new LifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, ContactFields.SUBSCRIPTION_STATUS,
                ContactFields.SUBSCRIPTION_TYPES, ContactFields.UNSUBSCRIBE_REASON, ContactFields.PHONES);
        final Option subscriptionStatus = optionService.getDefaultOption(orgId, fields.get(ContactFields.SUBSCRIPTION_STATUS));
        final String subscriptionStatusOption = subscriptionStatus.getOption();

        lifeCycleStage.setId(lifeCycleStageId);
        lifeCycleStatus.setId(lifeCycleStatusId);
        contact.setFirstName(phoneNumber);
        contact.setLifeCycleStage(lifeCycleStage);
        contact.setLifeCycleStatus(lifeCycleStatus);
        contact.setOrgId(orgId);
        contact.setOwnerId(userId);
        contact.setCreatedBy(crmUser);
        contact.setUpdatedBy(crmUser);
        contact.setSalesOwner(crmUser);
        contact.setSubscriptionStatus(subscriptionStatus);

        final PhoneNumber number = PhoneNumberUtil.parse(phoneNumber);
        final Phone oldPhone = new Phone();
        final ValueHolder valueHolder = new ValueHolder();

        oldPhone.setOrgId(orgId);
        oldPhone.setType(Phone.Type.WORK);
        oldPhone.setNumber(number.getNumberWithCountryCode());
        oldPhone.setCountry(number.getCountry());
        valueHolder.put(fields.get(ContactFields.PHONES).id(), oldPhone);
        contact.setValueHolder(valueHolder);
        contact.addPhone(oldPhone);
        contact.setPrimaryPhone(oldPhone);
        contact.setCreatedViaCall(true);

        if (Objects.equals(subscriptionStatusOption, Contact.SubscriptionStatus.SUBSCRIBED.getName())) {
            contact.setSubscriptionTypes(optionService.getDefaultOptions(orgId, fields.get(ContactFields.SUBSCRIPTION_TYPES)));
        } else if (Objects.equals(subscriptionStatusOption, Contact.SubscriptionStatus.UNSUBSCRIBED.getName())) {
            contact.setUnsubscribeReason(optionService.getDefaultOption(orgId, fields.get(ContactFields.UNSUBSCRIBE_REASON)));
        }

        return contact;
    }

    /**
     * <p>
     * Converts the {@link Contact} into {@link JsonObject},
     * it converts only the partial contact data.
     * </p>
     *
     * @param contact Represents the collection of {@link Contact}
     * @param email   Represents the email address
     * @return {@link JsonObject} of contact.
     */
    public JsonObject buildContactJsonObject(final Contact contact, final String email) {
        final Company company = contact.getCompany();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final JsonArray phoneNumbers = Json.array();
        final JsonObject contactObject = convertPartialData(contact);
        final DisplayPicture thumbnail = contact.getThumbnail();

        contactObject.put(ContactFields.ORG_ID, contact.orgId());
        contactObject.put(ContactFields.DESIGNATION, contact.getDesignation());
        contactObject.put(ContactFields.TWITTER, contact.getTwitter());
        contactObject.put(ContactFields.LINKEDIN, contact.getLinkedIn());
        contactObject.put(ContactFields.FACEBOOK, contact.getFacebook());
        contactObject.put(ContactFields.IS_NEW_CONTACT, false);

        if (Objects.nonNull(company)) {
            contactObject.put(ContactFields.COMPANY, companyConverter.convertPartialData(company));
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {

            for (final Email contactEmail : contactEmails) {

                if (Objects.equals(email, contactEmail.getValue())) {
                    contactObject.put(ContactFields.EMAIL, emailConverter.convert(new ConverterConfig(false, false), contactEmail));

                    break;
                }
            }
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = phoneConverter.convert(new ConverterConfig(false, false), phone);

                phoneNumbers.put(phoneJson);
            }
        }
        contactObject.put(ContactFields.PHONE, phoneNumbers);

        if (Objects.nonNull(thumbnail)) {
            contactObject.put(ContactFields.THUMBNAIL, convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
        }

        return contactObject;
    }

    /**
     * <p>
     * Converts the contact with thumbnail
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return the {@link JsonObject}
     */
    public JsonObject convertContactWithThumbnail(final Contact contact) {
        final JsonObject contactJson = Json.object();

        if (Objects.nonNull(contact)) {
            final DisplayPicture thumbnail = contact.getThumbnail();
            final Long id = contact.id();

            contactJson.put(ContactFields.ID, id);
            contactJson.put(ContactFields.FULL_NAME, contact.getFullName());

            if (Objects.nonNull(thumbnail)) {
                contactJson.put(ContactFields.THUMBNAIL, convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
            }
        }

        return contactJson;
    }

    /**
     * Converts {@link Contact} into {@link JsonObject}
     *
     * @param contact         The contact entity that holds the attributes of contact details.
     * @param converterConfig The configuration for conversion, including locale settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToJson(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final JsonArray emails = Json.array();
        final JsonArray phoneNumbers = Json.array();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
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
        final Option timeZone = contact.getTimeZone();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final SummaryInfo summaryInfo = contact.getSummaryInfo();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final Territory territory = contact.getTerritory();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final CrmUser salesOwner = contact.getSalesOwner();
        final Collection<MarketingList> marketingLists = contact.getMarketingLists();

        final String lastNameAlias = ContactFields.LAST_NAME;
        final String companyAlias = ContactFields.COMPANY;
        final String lifecycleStageAlias = ContactFields.LIFECYCLE_STAGE;
        final String lifecycleStatusAlias = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonAlias = ContactFields.CONTACT_LOST_REASON;
        final String sourceAlias = ContactFields.SOURCE;
        final String addressAlias = ContactFields.ADDRESS;
        final String subscriptionStatusAlias = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesAlias = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonAlias = ContactFields.UNSUBSCRIBE_REASON;
        final String timezoneAlias = ContactFields.TIMEZONE;
        final String createdByAlias = ContactFields.CREATED_BY;
        final String updatedByAlias = ContactFields.UPDATED_BY;
        final String territoryAlias = ContactFields.TERRITORY;
        final String salesOwnerAlias = ContactFields.SALES_OWNER;
        final String designationAlias = ContactFields.DESIGNATION;
        final String departmentAlias = ContactFields.DEPARTMENT;
        final String facebookAlias = ContactFields.FACEBOOK;
        final String twitterAlias = ContactFields.TWITTER;
        final String linkedinAlias = ContactFields.LINKEDIN;
        final String dateOfBirthAlias = ContactFields.DATE_OF_BIRTH;
        final String otherUnsubscribeReasonAlias = ContactFields.OTHER_UNSUBSCRIBE_REASON;
        final String emailsAlias = ContactFields.EMAILS;
        final String phonesAlias = ContactFields.PHONES;
        final String tagsAlias = ContactFields.TAGS;
        final Long orgId = contact.orgId();

        jsonObject.put(idKey, contact.id());
        jsonObject.put(ContactFields.ORG_ID, orgId);
        jsonObject.put(fields.get(ContactFields.FIRST_NAME).idAsString(), contact.getFirstName());

        if (fields.containsKey(lastNameAlias)) {
            jsonObject.put(fields.get(lastNameAlias).idAsString(), contact.getLastName());
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            jsonObject.put(fields.get(companyAlias).idAsString(), companyConverter.convertModelToOptionJson(company));
        }

        if (Objects.nonNull(lifeCycleStage) && fields.containsKey(lifecycleStageAlias)) {
            jsonObject.put(fields.get(lifecycleStageAlias).idAsString(), lifeCycleStageConverter.convertModelToOptionJson(lifeCycleStage));
        }

        if (Objects.nonNull(lifeCycleStatus) && fields.containsKey(lifecycleStatusAlias)) {
            jsonObject.put(fields.get(lifecycleStatusAlias).idAsString(), lifeCycleStatusConverter.convertModelToOptionJson(lifeCycleStatus));
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            jsonObject.put(fields.get(lostReasonAlias).idAsString(), optionConverter.convertModelToOptionJson(lostReason, converterConfig));
        }

        if (Objects.nonNull(source) && fields.containsKey(sourceAlias)) {
            jsonObject.put(fields.get(sourceAlias).idAsString(), optionConverter.convertModelToOptionJson(source, converterConfig));
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).idAsString(), addressConverter.convertModelToJson(address, converterConfig));
        }

        if (Objects.nonNull(subscriptionStatus) && fields.containsKey(subscriptionStatusAlias)) {
            jsonObject.put(fields.get(subscriptionStatusAlias).idAsString(), optionConverter.convertModelToOptionJson(subscriptionStatus, converterConfig));
        }

        if (Objects.nonNull(subscriptionTypes) && fields.containsKey(subscriptionTypesAlias)) {
            final JsonArray jsonArray = Json.array();

            subscriptionTypes.forEach(subscriptionType -> jsonArray.put(optionConverter.convertModelToOptionJson(subscriptionType, converterConfig)));
            jsonObject.put(fields.get(subscriptionTypesAlias).idAsString(), jsonArray);
        }

        if (Objects.nonNull(unsubscribeReason) && fields.containsKey(unsubscribeReasonAlias)) {
            jsonObject.put(fields.get(unsubscribeReasonAlias).idAsString(), optionConverter.convertModelToOptionJson(unsubscribeReason, converterConfig));
        }

        if (Objects.nonNull(timeZone) && fields.containsKey(timezoneAlias)) {
            jsonObject.put(fields.get(timezoneAlias).idAsString(), optionConverter.convertModelToOptionJson(timeZone, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).idAsString(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (fields.containsKey(designationAlias)) {
            jsonObject.put(fields.get(designationAlias).idAsString(), contact.getDesignation());
        }

        if (fields.containsKey(departmentAlias)) {
            jsonObject.put(fields.get(departmentAlias).idAsString(), contact.getDepartment());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).idAsString(), contact.getFacebook());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).idAsString(), contact.getTwitter());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).idAsString(), contact.getLinkedIn());
        }

        if (fields.containsKey(dateOfBirthAlias)) {
            jsonObject.put(fields.get(dateOfBirthAlias).idAsString(), dateUtil.getTimeStamp(contact.getDateOfBirth()));
        }

        if (fields.containsKey(otherUnsubscribeReasonAlias)) {
            jsonObject.put(fields.get(otherUnsubscribeReasonAlias).idAsString(), contact.getOtherUnsubscribeReason());
        }

        if (Objects.nonNull(marketingLists) && !marketingLists.isEmpty()) {
            final JsonArray marketingListArray = Json.array();
            final CrmUser user = userService.getUser(orgId, converterConfig.ownerId());
            final Locale locale = converterConfig.locale();

            for (final MarketingList marketingList : marketingLists) {
                marketingListArray.put(marketingListConverter.convertToJson(new CrmConverterData<>(user, locale, marketingList)));
            }

            jsonObject.put(fields.get(ContactFields.MARKETING_LIST).idAsString(), marketingListArray);
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig);
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty() && fields.containsKey(emailsAlias)) {

            if (Objects.isNull(contact.getPrimaryEmail())) {
                final Optional<Email> primaryEmail = getPrimaryEmail(contact.getEmails());

                contact.setPrimaryEmail(primaryEmail.orElseGet(() -> null));
            }

            for (final Email email : contactEmails) {
                final JsonObject emailJson = emailConverter.convertModelToJson(email, StoreType.EMAILS, converterConfig);

                if (Objects.equals(contact.getPrimaryEmail(), email)) {
                    emailJson.put(ContactFields.IS_PRIMARY, true);
                }
                emails.put(emailJson);
            }
            jsonObject.put(fields.get(emailsAlias).idAsString(), emails);
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty() && fields.containsKey(phonesAlias)) {

            if (Objects.isNull(contact.getPrimaryPhone())) {
                final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
            }

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = phoneConverter.convertModelToJson(phone, StoreType.PHONES, converterConfig);

                if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                    phoneJson.put(ContactFields.IS_PRIMARY, true);
                }
                phoneNumbers.put(phoneJson);
            }
            jsonObject.put(fields.get(phonesAlias).idAsString(), phoneNumbers);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).idAsString(), tagConverter.convertModelToJson(tags));
            }
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact         The contact entity that holds the attributes of contact details.
     * @param converterConfig The configuration for conversion, including locale settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertToRestJson(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final JsonArray emails = Json.array();
        final JsonArray phoneNumbers = Json.array();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
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
        final Option timeZone = contact.getTimeZone();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final SummaryInfo summaryInfo = contact.getSummaryInfo();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();
        final Territory territory = contact.getTerritory();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final CrmUser salesOwner = contact.getSalesOwner();

        final String lastNameAlias = ContactFields.LAST_NAME;
        final String companyAlias = ContactFields.COMPANY;
        final String lifecycleStageAlias = ContactFields.LIFECYCLE_STAGE;
        final String lifecycleStatusAlias = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonAlias = ContactFields.CONTACT_LOST_REASON;
        final String sourceAlias = ContactFields.SOURCE;
        final String addressAlias = ContactFields.ADDRESS;
        final String subscriptionStatusAlias = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesAlias = ContactFields.SUBSCRIPTION_TYPES;
        final String unsubscribeReasonAlias = ContactFields.UNSUBSCRIBE_REASON;
        final String timezoneAlias = ContactFields.TIMEZONE;
        final String createdByAlias = ContactFields.CREATED_BY;
        final String updatedByAlias = ContactFields.UPDATED_BY;
        final String territoryAlias = ContactFields.TERRITORY;
        final String salesOwnerAlias = ContactFields.SALES_OWNER;
        final String designationAlias = ContactFields.DESIGNATION;
        final String departmentAlias = ContactFields.DEPARTMENT;
        final String facebookAlias = ContactFields.FACEBOOK;
        final String twitterAlias = ContactFields.TWITTER;
        final String linkedinAlias = ContactFields.LINKEDIN;
        final String dateOfBirthAlias = ContactFields.DATE_OF_BIRTH;
        final String otherUnsubscribeReasonAlias = ContactFields.OTHER_UNSUBSCRIBE_REASON;
        final String emailsAlias = ContactFields.EMAILS;
        final String phonesAlias = ContactFields.PHONES;
        final String tagsAlias = ContactFields.TAGS;

        jsonObject.put(idKey, contact.id());
        jsonObject.put(fields.get(ContactFields.FIRST_NAME).getInternalName(), contact.getFirstName());

        if (fields.containsKey(lastNameAlias)) {
            jsonObject.put(fields.get(lastNameAlias).getInternalName(), contact.getLastName());
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            jsonObject.put(fields.get(companyAlias).getInternalName(), companyConverter.convertModelToOptionJson(company));
        }

        if (Objects.nonNull(lifeCycleStage) && fields.containsKey(lifecycleStageAlias)) {
            jsonObject.put(fields.get(lifecycleStageAlias).getInternalName(), lifeCycleStageConverter.convertModelToOptionJson(lifeCycleStage));
        }

        if (Objects.nonNull(lifeCycleStatus) && fields.containsKey(lifecycleStatusAlias)) {
            jsonObject.put(fields.get(lifecycleStatusAlias).getInternalName(), lifeCycleStatusConverter.convertModelToOptionJson(lifeCycleStatus));
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            jsonObject.put(fields.get(lostReasonAlias).getInternalName(), optionConverter.convertModelToOptionJson(lostReason, converterConfig));
        }

        if (Objects.nonNull(source) && fields.containsKey(sourceAlias)) {
            jsonObject.put(fields.get(sourceAlias).getInternalName(), optionConverter.convertModelToOptionJson(source, converterConfig));
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).getInternalName(), addressConverter.convertToRestJson(address, converterConfig));
        }

        if (Objects.nonNull(subscriptionStatus) && fields.containsKey(subscriptionStatusAlias)) {
            jsonObject.put(fields.get(subscriptionStatusAlias).getInternalName(), optionConverter.convertModelToOptionJson(subscriptionStatus, converterConfig));
        }

        if (Objects.nonNull(subscriptionTypes) && fields.containsKey(subscriptionTypesAlias)) {
            final JsonArray jsonArray = Json.array();

            subscriptionTypes.forEach(subscriptionType -> jsonArray.put(optionConverter.convertModelToOptionJson(subscriptionType, converterConfig)));
            jsonObject.put(fields.get(subscriptionTypesAlias).getInternalName(), jsonArray);
        }

        if (Objects.nonNull(unsubscribeReason) && fields.containsKey(unsubscribeReasonAlias)) {
            jsonObject.put(fields.get(unsubscribeReasonAlias).getInternalName(), optionConverter.convertModelToOptionJson(unsubscribeReason, converterConfig));
        }

        if (Objects.nonNull(timeZone) && fields.containsKey(timezoneAlias)) {
            jsonObject.put(fields.get(timezoneAlias).getInternalName(), optionConverter.convertModelToOptionJson(timeZone, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).getInternalName(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).getInternalName(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).getInternalName(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).getInternalName(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (fields.containsKey(designationAlias)) {
            jsonObject.put(fields.get(designationAlias).getInternalName(), contact.getDesignation());
        }

        if (fields.containsKey(departmentAlias)) {
            jsonObject.put(fields.get(departmentAlias).getInternalName(), contact.getDepartment());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).getInternalName(), contact.getFacebook());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).getInternalName(), contact.getTwitter());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).getInternalName(), contact.getLinkedIn());
        }

        if (fields.containsKey(dateOfBirthAlias)) {
            jsonObject.put(fields.get(dateOfBirthAlias).getInternalName(), dateUtil.getTimeStamp(contact.getDateOfBirth()));
        }

        if (fields.containsKey(otherUnsubscribeReasonAlias)) {
            jsonObject.put(fields.get(otherUnsubscribeReasonAlias).getInternalName(), contact.getOtherUnsubscribeReason());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig, true);
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty() && fields.containsKey(emailsAlias)) {

            if (Objects.isNull(contact.getPrimaryEmail())) {
                final Optional<Email> primaryEmail = getPrimaryEmail(contact.getEmails());

                contact.setPrimaryEmail(primaryEmail.orElseGet(() -> null));
            }

            for (final Email email : contactEmails) {
                final JsonObject emailJson = emailConverter.convertToRestJson(email, converterConfig);

                if (Objects.equals(contact.getPrimaryEmail(), email)) {
                    emailJson.put(ContactFields.IS_PRIMARY, true);
                }
                emails.put(emailJson);
            }
            jsonObject.put(fields.get(emailsAlias).getInternalName(), emails);
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty() && fields.containsKey(phonesAlias)) {

            if (Objects.isNull(contact.getPrimaryPhone())) {
                final Optional<Phone> primaryPhone = getPrimaryPhone(contact.getPhones());

                contact.setPrimaryPhone(primaryPhone.orElseGet(() -> null));
            }

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = phoneConverter.convertToRestJson(phone, converterConfig);

                if (Objects.equals(contact.getPrimaryPhone(), phone)) {
                    phoneJson.put(ContactFields.IS_PRIMARY, true);
                }
                phoneNumbers.put(phoneJson);
            }
            jsonObject.put(fields.get(phonesAlias).getInternalName(), phoneNumbers);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).getInternalName(), tagConverter.convertModelToJson(tags));
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Contact} into {@link JsonObject}
     *
     * @param contact         The contact entity with the attributes of contact details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Contact contact) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ContactFields.VALUE, contact.id());
        jsonObject.put(ContactFields.NAME, contact.getFullName());

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param contact         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param isHidden        A boolean flag indicating whether to display empty fields or not
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertSummaryIntoJson(final Contact contact, final ConverterConfig converterConfig, final boolean isHidden) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJson(contact, converterConfig);
        final DisplayPicture thumbnail = contact.getThumbnail();
        final Long contactId = contact.id();

        if (Objects.nonNull(thumbnail)) {
            profileJson.put(ContactFields.THUMBNAIL, convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
        }
        json.put(ContactFields.ID, contactId);
        json.put(ContactFields.PROFILE, profileJson);
        json.put(ContactFields.SUMMARY, convertSummaryToJsonArray(contact, isHidden, converterConfig));
        convertSubFieldToJson(contact, converterConfig, json);

        if (converterConfig.isAccessConfigRequired()) {
            json.put(FieldKeys.IS_EDITABLE.getName(), contact.isEditable());
            json.put(FieldKeys.IS_DELETABLE.getName(), contact.isDeletable());
        }

        return json;
    }

    /**
     * <p>
     * Converts the social profile for the given contact
     * </p>
     *
     * @param contact Represent the {@link Contact}
     * @return {@link JsonObject}
     */
    public JsonObject convertSocialProfile(final Contact contact, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Map<String, FieldInfo> fieldsMap = getSocialProfileFields(contact);

        for (final Map.Entry<String, FieldInfo> fieldEntry : fieldsMap.entrySet()) {
            jsonObject.put(fieldEntry.getKey(), convertFieldInfoToJsonObject(fieldEntry.getValue(), contact, converterConfig));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the social profile fields
     * </p>
     *
     * @param contact Represent the {@link Contact}
     * @return All the {@link FieldInfo} of the social profile fields
     */
    private Map<String, FieldInfo> getSocialProfileFields(final Contact contact) {
        return secureFieldService.groupDefaultFieldsByAliasName(contact.orgId(), contact.getEntityType(), List.of(ContactFields.LINKEDIN,
                ContactFields.TWITTER, ContactFields.FACEBOOK));
    }
}