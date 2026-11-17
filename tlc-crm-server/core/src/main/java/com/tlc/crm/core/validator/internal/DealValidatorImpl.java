package com.tlc.crm.core.validator.internal;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.validator.annotation.DealValidator;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.validator.DefaultValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.crm.entity.models.type.Option;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * It validates {@link Deal} entity and returns appropriate message
 *
 * @author Sundar
 * @version 1.0
 */
public class DealValidatorImpl extends DefaultValidator implements ConstraintValidator<DealValidator, Deal> {

    private final FieldService fieldService = FieldService.getSecureInstance(true);
    private final SourceSettingsService sourceSettingsService = SourceSettingsService.getInstance();

    /**
     * Checks whether the all the entity fields are having the valid values or not
     *
     * @param deal    which contains the contact values has to be validated
     * @param context Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final Deal deal, final ConstraintValidatorContext context) {
        final Long orgId = deal.orgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, Deal.ENTITY_TYPE);
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final String dealValueKey = DealFields.DEAL_VALUE;
        final FieldInfo contactField = fields.get(DealFields.PRIMARY_CONTACT);
        final FieldInfo participantsField = fields.get(DealFields.RELATED_CONTACTS);
        final FieldInfo companyField = fields.get(DealFields.COMPANY);
        final FieldInfo territoryField = fields.get(DealFields.TERRITORY);
        final FieldInfo dealValueField = fields.get(dealValueKey);
        final FieldInfo tagField = fields.get(DealFields.TAGS);
        final FieldInfo wonReasonField = fields.get(DealFields.WON_REASON);
        final FieldInfo lostReasonField = fields.get(DealFields.LOST_REASON);
        final Predicate<PresetConfig> presetConfigPredicate = (presetConfig -> presetConfig.immutable() || presetConfig.required());

        if (Objects.nonNull(wonReasonField)) {
            final Option wonReason = deal.getWonReason();

            if (Objects.nonNull(wonReason)) {
                validateDropdownOption(wonReasonField, wonReason, validatorContext);
            }
        }

        if (Objects.nonNull(lostReasonField)) {
            final Option lostReason = deal.getLostReason();

            if (Objects.nonNull(lostReason)) {
                validateDropdownOption(lostReasonField, lostReason, validatorContext);
            }
        }

        if (Objects.nonNull(contactField)) {
            final PresetConfig presetConfig = contactField.presetConfig();
            final Contact contact = deal.getPrimaryContact();

            if (Objects.isNull(contact)) {
                addConstraintViolationMessage(validatorContext, "i18n.deal.invalid.notnull.contact", contactField.aliasName());
            } else {
                contact.setOrgId(orgId);
                validateEntity(contactField, contact, validatorContext, presetConfig);
            }
        }

//        if (Objects.nonNull(deal.getForecastCategory()) && Objects.isNull(deal.getExpectedCloseDate())) {
//            addConstraintViolationMessage(validatorContext, "i18n.deal.field.expected.close.date.required", fields.get(DealFields.FORECAST_CATEGORY).aliasName());
//        }

        if (Objects.nonNull(participantsField)) {
            final Collection<Participant> participants = deal.getParticipants();
            final PresetConfig presetConfig = participantsField.presetConfig();

            if (Objects.nonNull(participants) && !participants.isEmpty()) {
                for (final Participant participant : participants) {
                    final Contact contact = participant.getContact();

                    contact.setOrgId(orgId);
                    validateEntity(contactField, contact, validatorContext, presetConfig);
                }
            } else if (presetConfigPredicate.test(presetConfig)) {
                addConstraintViolationMessage(validatorContext, "i18n.deal.field.related.contacts.required", participantsField.aliasName());
            }
        }

        if (Objects.nonNull(companyField)) {
            final PresetConfig presetConfig = companyField.presetConfig();
            final Company company = deal.getCompany();

            if (Objects.nonNull(company)) {
                company.setOrgId(orgId);
                validateEntity(companyField, company, validatorContext, presetConfig);
            } else if (presetConfigPredicate.test(presetConfig)) {
                addConstraintViolationMessage(validatorContext, "i18n.deal.field.company.required", companyField.aliasName());
            }
        }

        if (Objects.nonNull(dealValueField)) {
            final DealValue dealValue = deal.getDealValue();
            final PresetConfig presetConfig = dealValueField.presetConfig();
            final boolean isRequired = presetConfig.required();

            if (Objects.nonNull(dealValue)) {
                final BigDecimal value = dealValue.getValue();
                final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                validateTlcModel(dealValueField, dealValue, validatorContext, presetConfig);
                validateMonetaryPrecision(value, validatorContext);

                if (Objects.nonNull(productDetailsHolder)) {
                    if (sourceSettingsService.isSourceEnabled(orgId, Product.ENTITY_TYPE)) {
                        validateTlcModel(null, productDetailsHolder, validatorContext, null);
                    } else {
                        addConstraintViolationMessage(validatorContext, "i18n.deal.productDetailsHolder.disabled", dealValueKey);
                    }
                }
            } else if (isRequired) {
                addConstraintViolationMessage(validatorContext, "i18n.deal.field.deal.value.required", dealValueKey);
            }
        }

        if (Objects.nonNull(tagField)) {
            final PresetConfig presetConfig = tagField.presetConfig();
            final boolean required = presetConfig.required();
            final SummaryInfo summaryInfo = deal.getSummaryInfo();

            if (Objects.nonNull(summaryInfo)) {
                validateTags(orgId, summaryInfo.getTags(), validatorContext, required);
            } else if (required) {
                addConstraintViolationMessage(validatorContext, "i18n.deal.field.tags.required", tagField.aliasName());
            }
        }
        validateCreatedTime(deal, validatorContext, fields.get(DealFields.DEAL_CREATED));
        validate(validatorContext, deal, fields);
        validateTerritoryField(territoryField, Objects.isNull(deal.getTerritory()), validatorContext);
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
    }
}