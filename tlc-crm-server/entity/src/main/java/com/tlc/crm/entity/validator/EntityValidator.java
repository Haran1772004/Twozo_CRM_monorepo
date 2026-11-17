package com.tlc.crm.entity.validator;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Range;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Provides services to validate the fields of {@link com.tlc.crm.entity.models.Entity}
 *
 * @author Sundar
 * @version 1.0
 */
public abstract class EntityValidator {

    private static final int TWO_HUNDRED_FIFTY_FIVE = 255;
    private static final Integer MAX_TEXT_SIZE = 32000;
    private static final int PRECISION_SIZE = 13;
    private final EntityViolationsCache entityViolationsCache;
    private final ModelValidator validator;
    private final EntityManagerHolder entityManagerHolder;
    private final CrmUserManager crmUserManager;
    private final OptionService optionService;
    private final BaseCurrencyService baseCurrencyService;
    private final TagManager tagManager;
    private final MarketingListManager marketingListManager;

    protected EntityValidator() {
        this.entityViolationsCache = EntityViolationsCache.getInstance();
        this.validator = ValidatorServiceProvider.get().modelValidator();
        this.entityManagerHolder = EntityManagerHolder.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
        this.optionService = OptionService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.tagManager = TagManager.getInstance();
        this.marketingListManager = MarketingListManager.getInstance();
    }

    /**
     * <p>
     * Validates the string value by using the preset configuration
     * </p>
     *
     * @param field            Holds field attributes information
     * @param presetConfig     FieldInfo meta data
     * @param value            Value of the field
     * @param validatorContext which contains the error flag and validation context which contains the error flag and validation context
     * @param isSearchable     A flag which indicates whether the given field is searchable or not
     */
    protected void validateStringValue(final FieldInfo field, final PresetConfig presetConfig, @NotNull final Object value,
                                       final ValidatorContext validatorContext, final boolean isSearchable) {
        final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);

        if (!(value instanceof String)) {
            addConstraintViolationMessage(validatorContext, String.format("%s must hold string value only", fieldName), fieldName);
        } else {
            final int length = value.toString().length();
            final int minLength = presetConfig.minLength();
            final long maxLength = isSearchable ? presetConfig.maxLength() : MAX_TEXT_SIZE;

            if (minLength > 0 && length < minLength) {
                addConstraintViolationMessage(validatorContext, String.format("Minimum characters accepted for %s is %d", fieldName, minLength), fieldName);
            }

            if (length > maxLength) {
                addConstraintViolationMessage(validatorContext, String.format("Maximum characters accepted for %s is %d", fieldName, maxLength), fieldName);
            }
        }
    }

    /**
     * <p>
     * Validates the text field value
     * </p>
     *
     * @param field            Holds field information
     * @param isRequired       Flag to indicate whether the field value is required or not
     * @param presetConfig     Holds field meta data
     * @param value            Field value
     * @param validatorContext which contains the error flag and validation context
     * @param isSearchable     Flag which indicates whether the given field values are searchable or not
     */
    protected void validateStringField(final FieldInfo field, final boolean isRequired, final PresetConfig presetConfig,
                                       @NotNull final Object value, final ValidatorContext validatorContext,
                                       final boolean isSearchable) {
        if (isRequired && value.toString().isBlank()) {
            final String fieldName = field.fieldName();

            addConstraintViolationMessage(validatorContext, String.format("%s is required",
                    StringUtil.capitalizeFirstLetter(fieldName).orElseGet(() -> null)), fieldName);
        } else {
            validateStringValue(field, presetConfig, value, validatorContext, isSearchable);
        }
    }

    /**
     * <p>
     * Validates the field values of non decimal field
     * </p>
     *
     * @param field            Holds field information
     * @param isRequired       Flag to indicate whether the field value is required or not
     * @param value            Field value
     * @param validatorContext which contains the error flag and validation context
     * @param presetConfig     FieldInfo meta data
     */
    protected void validateNumberField(final FieldInfo field, final boolean isRequired, @NotNull final Object value,
                                       final ValidatorContext validatorContext, final PresetConfig presetConfig) {

        if (isRequired && value.toString().isBlank()) {
            final String fieldName = field.fieldName();

            addConstraintViolationMessage(validatorContext,
                    String.format("%s is required", StringUtil.capitalizeFirstLetter(fieldName).orElseGet(() -> null)), fieldName);
        } else {
            validateNonDecimalValue(field, presetConfig, value, validatorContext);
        }
    }

    /**
     * <p>
     * Validates the field values of non decimal field
     * </p>
     *
     * @param field            Holds field information
     * @param value            Field value
     * @param validatorContext which contains the error flag and validation context
     * @param presetConfig     FieldInfo meta data
     */
    protected void validateTemporalField(final FieldInfo field, final Object value, final ValidatorContext validatorContext,
                                         final PresetConfig presetConfig) {
        validateTimeStamp(field, presetConfig, value, validatorContext);
    }

    /**
     * Validates the collection of {@link TlcModel}.
     *
     * @param field            Holds field information
     * @param models           represents the collection of {@link TlcModel}
     * @param validatorContext which contains the error flag and validation context which contains the error flag and validation context
     * @param presetConfig     FieldInfo meta data
     */
    protected void validateTlcModels(final FieldInfo field, @NotNull final Collection<?> models, final ValidatorContext validatorContext,
                                     final PresetConfig presetConfig) {

        if (!models.isEmpty()) {
            models.forEach(model -> validateTlcModel(field, (TlcModel) model, validatorContext, presetConfig));
        } else if (presetConfig.required()) {
            final String fieldName = field.fieldName();

            addConstraintViolationMessage(validatorContext,
                    StringUtil.capitalizeFirstLetter(fieldName).orElseGet(() -> null), fieldName);
        }
    }

    /**
     * Validates only if the model is child of {@link TlcModel}.
     *
     * @param field            Holds field information
     * @param tlcModel         represents the {@link TlcModel}
     * @param validatorContext Which contains the error flag and validation context which contains the error flag and validation context
     * @param presetConfig     FieldInfo meta data
     */
    protected void validateTlcModel(final FieldInfo field, @NotNull final TlcModel tlcModel, final ValidatorContext validatorContext,
                                    final PresetConfig presetConfig) {
        final boolean isCreateEvent = Objects.isNull(tlcModel.id());
        Collection<ConstraintViolation<TlcModel>> errors;

        if (entityViolationsCache.isEnabled() && tlcModel instanceof final Entity entity) {
            errors = entityViolationsCache.get(entity);

            if (Objects.isNull(errors)) {
                errors = validator.validate(tlcModel, isCreateEvent ? Group.Create.class : Group.Update.class);
                entityViolationsCache.put(entity, errors);
            }
        } else {
            errors = validator.validate(tlcModel, isCreateEvent ? Group.Create.class : Group.Update.class);
        }

        if (tlcModel instanceof final Monetary monetary) {
            validateMonetary(monetary, validatorContext);
        } else if (tlcModel instanceof final Range range) {
            validateRange(field, range, validatorContext, presetConfig);
        }

        if (Objects.nonNull(errors)) {
            errors.forEach(error -> addConstraintViolationMessage(validatorContext, error.getMessage(), field.fieldName()));
        }
    }

    /**
     * Validates the entity type value
     *
     * @param orgId            Represents the organisation ID
     * @param value            Field value
     * @param field            Holds field information
     * @param validatorContext which contains the error flag and validation context
     */
    protected void validateEntityType(final Long orgId, final Object value, final FieldInfo field, final ValidatorContext validatorContext) {

        if (value instanceof final Long id) {
            final StoreType storeType = field.storeType();
            final ConfigManager<?> manager = entityManagerHolder.getManager(storeType);

            if (Objects.nonNull(manager) && !manager.exists(orgId, id)) {
                addConstraintViolationMessage(validatorContext, String.format("%s Not Exists", field.storeType().getName()), field.fieldName());
            }
        }
    }

    /**
     * <p>
     * Validates the precision of a monetary value.
     * </p>
     *
     * @param value            Represents the monetary value
     * @param validatorContext Which contains the error flag and validation context
     */
    protected void validateMonetaryPrecision(final BigDecimal value, final ValidatorContext validatorContext) {
        final int digitCountBeforeDecimal = value.precision() - value.scale();

        if (digitCountBeforeDecimal > PRECISION_SIZE) {
            addConstraintViolationMessage(validatorContext, "i18n.monetary.value.digit.decimal.point.exceeds", StoreType.MONETARY.getName());
        }
    }

    /**
     * <p>
     * Adds constraint violation message to the validation context
     * </p>
     *
     * @param message Violation message
     */
    protected void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }

    /**
     * <p>
     * Validates the dropdown field option
     * </p>
     *
     * @param field            Holds field information
     * @param value            Field value
     * @param validatorContext which contains the error flag and validation context
     */
    protected void validateDropdownOption(final FieldInfo field, @NotNull final Object value, final ValidatorContext validatorContext) {
        final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);

        if (value instanceof ArrayList) {
            addConstraintViolationMessage(validatorContext, String.format("%s must be a String", fieldName), fieldName);
        }
        final Long fieldId = field.id();
        final Long orgId;
        final Long optionId;

        if (value instanceof final Option option) {
            optionId = option.id();
            orgId = option.orgId();
        } else {
            optionId = (value instanceof final Long id) ? id : null;
            orgId = field.orgId();
        }

        if ((Objects.nonNull(optionId) && !optionService.exists(orgId, fieldId, optionId))
                || (value instanceof final String optionName && !optionService.exists(orgId, fieldId, optionName))) {
            addConstraintViolationMessage(validatorContext, String.format("Invalid option given for %s", fieldName), fieldName);
        }
    }

    /**
     * <p>
     * Validates the multiselect field option
     * </p>
     *
     * @param field            Which Holds field information
     * @param isRequired       The Flag which indicates whether the field value is required or not
     * @param value            Represents the field value
     * @param validatorContext Which contains the error flag and validation context
     */
    protected void validateMultiSelectField(final Long orgId, final FieldInfo field, final boolean isRequired, final Object value,
                                            final ValidatorContext validatorContext) {
        final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);

        if (value instanceof final Collection<?> multiSelectValues) {
            if (multiSelectValues.isEmpty() && isRequired) {
                addConstraintViolationMessage(validatorContext, String.format("%s is required", fieldName), fieldName);
            } else {
                final Collection<Long> optionIds = new ArrayList<>();

                for (final Object multiSelectValue : multiSelectValues) {

                    if (Objects.isNull(multiSelectValue)) {
                        addConstraintViolationMessage(validatorContext, String.format("Values of %s should not be null", fieldName), fieldName);
                    } else if (multiSelectValue instanceof final Long optionId) {
                        optionIds.add(optionId);
                    } else if (multiSelectValue instanceof final Option option) {
                        optionIds.add(option.id());
                    } else {
                        addConstraintViolationMessage(validatorContext, String.format("Invalid %s format ", fieldName), fieldName);
                    }
                }

                if (!optionIds.isEmpty() && !optionService.exists(orgId, field.id(), optionIds, false)) {
                    addConstraintViolationMessage(validatorContext, String.format("Invalid option given for %s", fieldName), fieldName);
                }
            }
        } else {
            addConstraintViolationMessage(validatorContext, String.format("%s must be a list", fieldName), fieldName);
        }
    }

    /**
     * <p>
     * Validates the owner field type value.
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param value            Field value
     * @param field            Holds field information
     * @param isRequired       Flag to indicate whether the field value is required or not
     * @param validatorContext which contains the error flag and validation context
     */
    protected void validateOwnerField(final Long orgId, final Object value, final FieldInfo field,
                                      final boolean isRequired, final ValidatorContext validatorContext) {

        if (isRequired && Objects.isNull(value)) {
            final String fieldName = field.aliasName();

            addConstraintViolationMessage(validatorContext, String.format("%s is required",
                    StringUtil.capitalizeFirstLetter(fieldName).orElseGet(() -> null)), fieldName);
        }

        if (value instanceof final Long id) {

            if (!crmUserManager.exists(orgId, List.of(id))) {
                addConstraintViolationMessage(validatorContext, String.format("%s Not Exists", field.storeType().getName()), field.aliasName());
            }
        }
    }

    /**
     * <p>
     * Validates the {@link Tag}
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param tags             which holds the {@link Collection} of {@link Tag}
     * @param validatorContext Which contains the error flag and validation context
     * @param required         Represents whether the field value is required or not
     */
    protected void validateTags(final long orgId, final Collection<Tag> tags,
                                final ValidatorContext validatorContext, final boolean required) {
        final String tagKey = TagFields.TAG.getName();

        if (Objects.nonNull(tags) && !tags.isEmpty()) {

            for (final Tag tag : tags) {
                final Long id = tag.id();
                final String name = tag.getName();
                final boolean isNameNotNull = Objects.nonNull(name);

                if (isNameNotNull && name.length() > TWO_HUNDRED_FIFTY_FIVE) {
                    addConstraintViolationMessage(validatorContext, "i18n.tag.name.max.size", tagKey);
                }

                if (Objects.nonNull(id) && !tagManager.exists(orgId, id)) {
                    addConstraintViolationMessage(validatorContext, "i18n.tag.not.found", tagKey);
                } else if (Objects.isNull(id) && isNameNotNull && tagManager.isTagNameExists(orgId, name)) {
                    addConstraintViolationMessage(validatorContext, "i18n.tag.name.already.exists", tagKey);
                }
            }
        } else if (required) {
            addConstraintViolationMessage(validatorContext, "i18n.contact.field.tags.required", tagKey);
        }
    }

    /**
     * <p>
     * Validates the {@link MarketingList}
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param marketingLists   which holds the {@link Collection} of {@link MarketingList}
     * @param validatorContext Which contains the error flag and validation context
     * @param fieldName        Represents the field alias name
     */
    protected void validateMarketingLists(final long orgId, final Collection<MarketingList> marketingLists, final ValidatorContext validatorContext, final String fieldName) {
        for (final MarketingList marketingList : marketingLists) {
            final Long marketingListId = marketingList.id();

            if (Objects.nonNull(marketingListId) && !marketingListManager.exists(orgId, marketingListId)) {
                addConstraintViolationMessage(validatorContext, "i18n.marketing.list.not.found", fieldName);
            }
        }
    }

    /**
     * <p>
     * Validates the time stamp.
     * </p>
     *
     * @param field            Holds field information.
     * @param presetConfig     FieldInfo meta data.
     * @param value            Field value
     * @param validatorContext which contains the error flag and validation context.
     */
    private void validateTimeStamp(final FieldInfo field, final PresetConfig presetConfig, @NotNull final Object value,
                                   final ValidatorContext validatorContext) {
        if (Objects.nonNull(value)) {
            final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);

            if (value instanceof final Temporal temporal) {
                final Long timeStamp = temporal.getTimeStamp();

                if (Objects.nonNull(timeStamp) && !TypeChecker.isNonDecimalNumber(timeStamp)) {
                    addConstraintViolationMessage(validatorContext, String.format("%s timestamp must hold a long value", fieldName), fieldName);
                } else {
                    validateNonDecimalValue(fieldName, presetConfig, timeStamp, validatorContext);
                }
            }
        }
    }

    /**
     * <p>
     * Validates the non decimal value of a field by using the preset configuration
     * </p>
     *
     * @param fieldName        Holds field name.
     * @param presetConfig     {@link PresetConfig}.
     * @param validatorContext {@link PresetConfig}.
     */
    private void validateNonDecimalValue(final String fieldName, final PresetConfig presetConfig, @NotNull final Long nonDecimalValue,
                                         final ValidatorContext validatorContext) {
        final Long maxValue = getNonDecimalValue(presetConfig.max());
        final Long minValue = getNonDecimalValue(presetConfig.min());

        if (Objects.nonNull(minValue) && nonDecimalValue.compareTo(minValue) < 0) {
            addConstraintViolationMessage(validatorContext, String.format("Min value of %s is %s", fieldName, minValue), fieldName);
        }

        if (Objects.nonNull(maxValue) && nonDecimalValue.compareTo(maxValue) > 0) {
            addConstraintViolationMessage(validatorContext, String.format("Min value of %s is %s", fieldName, maxValue), fieldName);
        }
    }

    /**
     * <p>
     * Validates the non decimal value of a field by using the preset configuration
     * </p>
     *
     * @param field            Holds field information
     * @param presetConfig     FieldInfo meta data
     * @param value            FieldInfo value
     * @param validatorContext which contains the error flag and validation context
     */
    private void validateNonDecimalValue(final FieldInfo field, final PresetConfig presetConfig, @NotNull final Object value,
                                         final ValidatorContext validatorContext) {
        final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);

        if (!TypeChecker.isNonDecimalNumber(value)) {
            addConstraintViolationMessage(validatorContext, String.format("%s must hold a long value", fieldName), fieldName);
        } else {
            validateNonDecimalValue(fieldName, presetConfig, (Long) value, validatorContext);
        }
    }

    /**
     * <p>
     * Converts the given object to long
     * </p>
     *
     * @param value An object that represents a non-decimal number
     * @return Appropriate long value
     */
    private Long getNonDecimalValue(final Object value) {
        return Objects.isNull(value) ? null : NumberUtils.parseNumber(value).longValue();
    }

    /**
     * <p>
     * Validates the time range field value
     * </p>
     *
     * @param field            Holds field information
     * @param range            Which contains the range data
     * @param validatorContext which contains the error flag and validation context
     * @param presetConfig     FieldInfo meta data
     */
    private void validateRange(final FieldInfo field, final Range range, final ValidatorContext validatorContext,
                               final PresetConfig presetConfig) {
        final String fieldName = StringUtil.capitalizeFirstLetter(field.fieldName()).orElseGet(() -> null);
        final long minValue = presetConfig.min();
        final long maxValue = presetConfig.max();
        final Long startValue = range.getFrom().getTimeStamp();
        final Long endValue = range.getTo().getTimeStamp();
        final boolean startValueIsNull = Objects.isNull(startValue);
        final boolean endValueIsNull = Objects.isNull(endValue);

        if (startValueIsNull || startValue.compareTo(minValue) < 0) {
            addConstraintViolationMessage(validatorContext, String.format("Min value of %s is %d", fieldName, minValue), fieldName);
        }

        if (endValueIsNull || endValue.compareTo(maxValue) > 0) {
            addConstraintViolationMessage(validatorContext, String.format("Max value of %s is %d", fieldName, maxValue), fieldName);
        }

        if (!startValueIsNull && !endValueIsNull && !(StoreType.TIME_RANGE == field.storeType()) && startValue > endValue) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.range.invalid", fieldName);
        }
    }

    /**
     * Validates the {@link Monetary}
     *
     * @param monetary         Which contains the monetary data
     * @param validatorContext Which contains the error flag and validation context
     */
    private void validateMonetary(final Monetary monetary, final ValidatorContext validatorContext) {
        final Currency currency = monetary.getCurrency();
        final BigDecimal value = monetary.getValue();
        final String monetaryKey = StoreType.MONETARY.getName();

        if (Objects.isNull(currency)) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.currency.id.invalid", monetaryKey);
        } else if (!baseCurrencyService.getAllCurrencies(monetary.orgId()).contains(currency)) {
            addConstraintViolationMessage(validatorContext, "i18n.currency.unsupported", monetaryKey);
        } else if (Objects.isNull(value)) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.value.invalid", monetaryKey);
        } else {
            validateMonetaryPrecision(value, validatorContext);
        }
    }
}