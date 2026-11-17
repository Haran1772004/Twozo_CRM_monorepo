package com.tlc.crm.core.validator.internal;

import com.tlc.commons.json.JsonObject;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.field.product.ProductFields;

import com.tlc.crm.core.validator.annotation.ProductValidator;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.entity.validator.DefaultValidator;
import com.tlc.crm.entity.validator.ValidatorContext;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * It validates {@link Product} entity and returns appropriate message
 *
 * @author Sundar
 * @version 1.0
 */
public class ProductValidatorImpl extends DefaultValidator implements ConstraintValidator<ProductValidator, Product> {

    private final FieldService fieldService = FieldService.getSecureInstance(true);
    private final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
    private final SourceSettingsService sourceSettingsService = SourceSettingsService.getInstance();
    private final BaseCurrencyService baseCurrencyService = BaseCurrencyService.getInstance();

    /**
     * Checks whether the all the entity fields are having the valid values or not
     *
     * @param product                    which contains the product values has to be validated
     * @param constraintValidatorContext Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final Product product, final ConstraintValidatorContext constraintValidatorContext) {
        final Long orgId = product.orgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, Product.ENTITY_TYPE);
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettingsBySource(orgId, Product.ENTITY_TYPE);

        final Variation defaultVariation = product.getDefaultVariation();

        if (Objects.nonNull(defaultVariation)) {
            final Collection<PriceTag> priceTags = defaultVariation.getPriceTags();
            final Set<Currency> currencies = new HashSet<>();

            for (final PriceTag priceTag : priceTags) {

                if (!currencies.add(priceTag.getCurrency())) {
                    addConstraintViolationMessage(validatorContext, "i18n.product.variation.currency.duplication.restricted", ProductFields.VARIATION);
                }
            }
        }
        if (sourceSettings.isEnabled()) {
            final FieldInfo priceTagsField = fields.get(ProductFields.PRICE_TAGS);

            if (Objects.nonNull(priceTagsField)) {
                final Variation variation = product.getDefaultVariation();
                final PresetConfig presetConfig = priceTagsField.presetConfig();
                final boolean isVariationRequired = presetConfig.immutable() || presetConfig.required();

                if (Objects.nonNull(variation)) {
                    final Collection<PriceTag> priceTags = variation.getPriceTags();

                    for (final PriceTag priceTag : priceTags) {
                        priceTag.setOrgId(orgId);
                        validateTlcModel(priceTagsField, priceTag, validatorContext, presetConfig);
                    }
                    final Collection<Currency> currencies = priceTags.stream().map(PriceTag::getCurrency).collect(Collectors.toUnmodifiableSet());

                    if (!baseCurrencyService.getActiveCurrencies(orgId).containsAll(currencies)) {
                        addConstraintViolationMessage(validatorContext, "i18n.entity.currency.not.supported", ProductFields.VARIATION);
                    }
                } else if (isVariationRequired) {
                    addConstraintViolationMessage(validatorContext, "i18n.product.unitPrice.required", ProductFields.UNIT_PRICE);
                }
            }
            validateCreatedTime(product, validatorContext, fields.get(ProductFields.CREATED_ON));
            validate(validatorContext, product, fields);
            disableDefaultConstraintViolation(validatorContext);
        }
        return !validatorContext.isErrorsFound();
    }

    /**
     * {@inheritDoc}
     *
     * @param field            Represents the field data
     * @param isRequired       Represents to indicate whether the field value is required or not
     * @param model            Represents the {@link TlcModel}
     * @param validatorContext Represents the error flag and validation context
     * @param presetConfig     Represents the fieldInfo metadata
     * @param createEvent      Represents the validation for create or update action
     */
    protected void validateModel(final FieldInfo field, final boolean isRequired, final Object model,
                                 final ValidatorContext validatorContext, final JsonObject presetConfig, final boolean createEvent) {
        if (isRequired && (Objects.isNull(model))) {
            final String fieldName = field.fieldName();
            addConstraintViolationMessage(validatorContext, String.format("%s is required", fieldName), fieldName);
        }

        if (model instanceof final TlcModel tlcModel) {
            final Set<ConstraintViolation<TlcModel>> errors = validator.validate(tlcModel, createEvent
                    ? Group.Create.class : Group.Update.class);

            if (model instanceof final PriceTag priceTag) {
                validatePriceTag(priceTag, validatorContext);
            }

            if (Objects.nonNull(errors) && !errors.isEmpty()) {
                errors.forEach(error -> addConstraintViolationMessage(validatorContext, error.getMessage(), field.fieldName()));
            }
        }
    }

    /**
     * Validates the priceTag
     *
     * @param priceTag         Holds the fields of priceTag
     * @param validatorContext Represents the error flag and validation context
     */
    private void validatePriceTag(final PriceTag priceTag, final ValidatorContext validatorContext) {
        final Double unitPrice = priceTag.getPrice();
        final String unitPriceKey = ProductFields.UNIT_PRICE;

        if (Objects.isNull(unitPrice)) {
            addConstraintViolationMessage(validatorContext, "i18n.product.unit.price.invalid", unitPriceKey);
        }

        if (Objects.isNull(priceTag.getCurrency())) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.currency.id.invalid", unitPriceKey);
        }

        final Double tax = priceTag.getTax();

        if (Objects.isNull(tax)) {
            addConstraintViolationMessage(validatorContext, "i18n.product.tax.invalid", unitPriceKey);
        }
    }
}