package com.tlc.crm.core.validator.internal;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.validator.annotation.ContactValidator;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.DefaultValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.l10n.temporal.model.Date;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * It validates {@link Contact} entity and returns appropriate message
 *
 * @author Sundar
 * @version 1.0
 */
public class ContactValidatorImpl extends DefaultValidator implements ConstraintValidator<ContactValidator, Contact> {

    private final FieldService fieldService = FieldService.getSecureInstance(true);

    /**
     * Checks whether the all the entity are having the valid values or not
     *
     * @param contact                    which the contact values has to be validated
     * @param constraintValidatorContext Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final Contact contact, final ConstraintValidatorContext constraintValidatorContext) {
        final Long orgId = contact.orgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE);
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final FieldInfo companyField = fields.get(ContactFields.COMPANY);
        final FieldInfo territoryField = fields.get(ContactFields.TERRITORY);
        final FieldInfo tagField = fields.get(ContactFields.TAGS);
        final FieldInfo subscriptionTypesField = fields.get(ContactFields.SUBSCRIPTION_TYPES);
        final FieldInfo unsubscribeReasonField = fields.get(ContactFields.UNSUBSCRIBE_REASON);
        final FieldInfo dobField = fields.get(ContactFields.DATE_OF_BIRTH);
        final FieldInfo otherUnsubscribeReasonField = fields.get(ContactFields.OTHER_UNSUBSCRIBE_REASON);
        final FieldInfo marketingListField = fields.get(ContactFields.MARKETING_LIST);
        final Option subscriptionStatus = contact.getSubscriptionStatus();
        final String subscriptionStatusOption = Objects.nonNull(subscriptionStatus) ? subscriptionStatus.getOption() : null;
        final Collection<Option> subscriptionTypes = contact.getSubscriptionTypes();
        final Option unsubscribeReason = contact.getUnsubscribeReason();
        final String otherUnsubscribeReason = contact.getOtherUnsubscribeReason();
        final Collection<MarketingList> marketingLists = contact.getMarketingLists();

        if (Objects.nonNull(companyField)) {
            final Company company = contact.getCompany();
            final PresetConfig presetConfig = companyField.presetConfig();

            if (Objects.nonNull(company)) {
                validateEntity(companyField, company, validatorContext, presetConfig);
            } else if ((presetConfig.immutable() || presetConfig.required())){
                addConstraintViolationMessage(validatorContext, "i18n.contact.field.company.required", companyField.aliasName());
            }
        }

        if (Objects.nonNull(subscriptionTypesField) && (Objects.nonNull(subscriptionTypes) && !subscriptionTypes.isEmpty())) {
            validateMultiSelectField(orgId, subscriptionTypesField, false, subscriptionTypes, validatorContext);
        }

        if (Objects.nonNull(otherUnsubscribeReasonField) && Objects.nonNull(otherUnsubscribeReason)) {
            validateStringField(otherUnsubscribeReasonField, false, otherUnsubscribeReasonField.presetConfig(), otherUnsubscribeReason, validatorContext, true);
        }

        if (Objects.nonNull(unsubscribeReasonField) && Objects.nonNull(unsubscribeReason)) {
            validateDropdownOption(unsubscribeReasonField, unsubscribeReason, validatorContext);
        }

        if (Objects.nonNull(subscriptionStatusOption)) {

            if (Objects.equals(Contact.SubscriptionStatus.SUBSCRIBED.getName(), subscriptionStatusOption)) {

                if (Objects.isNull(subscriptionTypes) || subscriptionTypes.isEmpty()) {
                    addConstraintViolationMessage(validatorContext, "i18n.contact.subscription.types.not.found", subscriptionTypesField.aliasName());
                }
            } else if (Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatusOption)) {

                if (Objects.isNull(unsubscribeReason)) {
                    addConstraintViolationMessage(validatorContext, "i18n.contact.unsubscribeReason.not.found", unsubscribeReasonField.aliasName());
                } else if (Objects.equals(Contact.UnsubscribeReason.OTHER_UNSUBSCRIBE_REASON.getName(), unsubscribeReason.getOption())
                        && Objects.isNull(otherUnsubscribeReason)) {
                    addConstraintViolationMessage(validatorContext, "i18n.contact.otherUnsubscribeReason.not.found", otherUnsubscribeReasonField.aliasName());
                }
            }
        }

        validate(validatorContext, contact, fields);
        validateTerritoryField(territoryField, Objects.isNull(contact.getTerritory()), validatorContext);
        final Date dateOfBirth = contact.getDateOfBirth();

        if (Objects.nonNull(dateOfBirth) && dateOfBirth.getTimeStamp() > Instant.now().toEpochMilli()) {
            addConstraintViolationMessage(validatorContext, "i18n.contact.date.of.birth.invalid", dobField.aliasName());
        }

        if (Objects.nonNull(tagField)) {
            final PresetConfig presetConfig = tagField.presetConfig();
            final boolean required = presetConfig.required();
            final SummaryInfo summaryInfo = contact.getSummaryInfo();

            if (Objects.nonNull(summaryInfo)) {
                validateTags(orgId, summaryInfo.getTags(), validatorContext, required);
            } else if (required) {
                addConstraintViolationMessage(validatorContext, "i18n.contact.field.tags.required", tagField.aliasName());
            }
        }

        if (Objects.nonNull(marketingListField)) {
            final PresetConfig presetConfig = marketingListField.presetConfig();
            final boolean required = presetConfig.required();
            final String aliasName = marketingListField.aliasName();

            if (Objects.nonNull(marketingLists) && !marketingLists.isEmpty()) {
                validateMarketingLists(orgId, marketingLists, validatorContext, aliasName);
            } else if (required) {
                addConstraintViolationMessage(validatorContext, "i18n.contact.field.marketing.list.required", aliasName);
            }
        }

        if (Objects.nonNull(dobField) && (dobField.presetConfig().required() && Objects.isNull(dateOfBirth))) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.date.of.birth.required", dobField.aliasName());
        }
        validateCreatedTime(contact, validatorContext, fields.get(ContactFields.CREATED_AT));
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
    }
}
