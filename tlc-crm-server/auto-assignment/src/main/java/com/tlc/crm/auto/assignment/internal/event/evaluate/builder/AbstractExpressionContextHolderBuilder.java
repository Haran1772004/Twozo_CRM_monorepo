package com.tlc.crm.auto.assignment.internal.event.evaluate.builder;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.CriteriaConnector;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.resolver.DateUtil;

import com.tlc.validator.TlcModel;
import com.tlc.validator.expression.Criteria;
import com.tlc.validator.expression.model.CriteriaData;
import com.tlc.validator.expression.model.ExpressionContextHolder;
import com.tlc.validator.expression.model.ExpressionDataType;
import com.tlc.validator.expression.service.CriteriaExpression;
import com.tlc.validator.expression.service.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The builder that constructs the expression for an entity with the provided criteria information.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractExpressionContextHolderBuilder<T extends TlcModel> implements ExpressionContextHolderBuilder<T> {

    protected static final String EMPTY_STRING = "";
    protected static final Function<String, String> TEXT_NORMALIZER = (value) -> null != value ? value.toLowerCase() : EMPTY_STRING;
    protected static final Function<Object, Number> NUMBER_PARSER = NumberUtils::parseNumber;

    private static final Function<String, String> TRIMMED_VALUE = value -> null != value ? value.replaceAll("\\s", EMPTY_STRING) : null;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpressionContextHolderBuilder.class);

    protected final DateTimeUtil dateTimeUtil;
    protected final DateUtil dateUtil;
    protected final FieldService fieldService;

    private final ActivityTypeManager activityTypeManager;
    private final Source source;

    protected AbstractExpressionContextHolderBuilder(final Source source) {
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.dateUtil = DateUtil.getInstance();
        this.fieldService = FieldService.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.source = source;
    }

    /**
     * {@inheritDoc}
     *
     * @param model        Refers to the model.
     * @param criteriaSets Refers the {@link AutoAssignmentCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    @Override
    public ExpressionContextHolder buildExpressionContextHolder(final T model, final Collection<AutoAssignmentCriteriaSet> criteriaSets) {
        final Long orgId = model.orgId();

        LOGGER.info("Auto assignment: Expression builder initiated for Org ID: {}, Entity: {}, ID: {}", orgId, source, model.id());

        final Map<String, Object> context = new HashMap<>();
        final BiConsumer<String, Object> valueInjector = (fieldName, value) -> context.put(fieldName, Objects.nonNull(value) ? value : null);
        final CriteriaBuilder criteriaBuilder = new CriteriaBuilder(context, new CriteriaExpression());
        final int criteriaSetSize = criteriaSets.size();
        final long startTime = System.currentTimeMillis();

        Expression expression = null;
        CriteriaConnector setConnector = null;
        int criteriaSetIndex = 1;

        for (final AutoAssignmentCriteriaSet criteriaSet : criteriaSets) {
            final Collection<AutoAssignmentCriteria> criteriaList = criteriaSet.getCriteriaList();
            final int criteriaSize = criteriaList.size();

            Expression criteriaExpression = null;
            CriteriaConnector criteriaConnector = null;
            int criteriaIndex = 1;

            for (final AutoAssignmentCriteria autoAssignmentCriteria : criteriaSet.getCriteriaList()) {
                final FieldInfo fieldInfo = fieldService.getField(orgId, autoAssignmentCriteria.getFieldId());
                final String fieldName = fieldInfo.isPersistent() ? resolvePersistentFieldName(fieldInfo) : fieldInfo.aliasName();
                final Long fieldId = fieldInfo.id();
                final StoreType storeType = Optional.ofNullable(fieldInfo.presetConfig().getAutomationConfig())
                        .map(config -> StoreType.valueOf(config.getString(AutoAssignmentAttributes.FIELD_TYPE))).orElseGet(fieldInfo::storeType);
                final Object fieldValue = getValue(model, fieldId);

                valueInjector.accept(fieldName, fieldValue);
                criteriaBuilder.setCriteriaDetails(autoAssignmentCriteria);

                final Criteria criteria = criteriaBuilder.build(fieldName, fieldValue, storeType);

                if (null != criteria) {

                    if (null == criteriaExpression) {
                        criteriaExpression = new Expression(criteria);
                    } else {
                        if (criteriaIndex++ < criteriaSize) {
                            criteriaExpression = CriteriaConnector.AND == criteriaConnector
                                    ? criteriaExpression.and(criteria) : criteriaExpression.or(criteria);
                        }
                    }
                }
                criteriaConnector = autoAssignmentCriteria.getConnector();
            }

            if (null == expression) {
                expression = criteriaExpression;
            } else {
                if (criteriaSetIndex++ < criteriaSetSize) {
                    expression = CriteriaConnector.AND == setConnector
                            ? expression.and(criteriaExpression) : expression.or(criteriaExpression);
                }
            }
            setConnector = criteriaSet.getConnector();
        }
        LOGGER.debug("Auto Assignment: Conversion of criteria to Expression took {} milliseconds for Org ID: {}, Entity: {}, ID: {}",
                System.currentTimeMillis() - startTime, orgId, source, model.id());

        return new ExpressionContextHolder(expression, context);
    }

    /**
     * <p>
     * Resolves the field name of the custom field and returns it.
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}.
     * @return the resolved field name.
     */
    private String resolvePersistentFieldName(final FieldInfo fieldInfo) {
        final String resolvedFieldName = fieldInfo.fieldName().replaceAll("[^a-zA-Z]", EMPTY_STRING);

        return String.join(EMPTY_STRING, resolvedFieldName.isBlank() ? AutoAssignmentAttributes.PERSISTENT : resolvedFieldName, fieldInfo.idAsString());
    }

    /**
     * Retrieves the necessary {@link Criteria} for the expression identified
     * by various store types and fields.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private class CriteriaBuilder {

        private final Map<String, Object> context;
        private final Criteria expressionCriteria;

        private AutoAssignmentCriteria autoAssignmentCriteria;
        private AutoAssignmentComparator autoAssignmentComparator;
        private Collection<AutoAssignmentCriteriaValue> criteriaValues;

        private CriteriaBuilder(final Map<String, Object> context, final Criteria expressionCriteria) {
            this.context = context;
            this.expressionCriteria = Objects.requireNonNull(expressionCriteria);
        }

        /**
         * Sets the criteria details.
         *
         * @param autoAssignmentCriteria Refers the {@link AutoAssignmentCriteria}.
         */
        private void setCriteriaDetails(final AutoAssignmentCriteria autoAssignmentCriteria) {
            this.autoAssignmentCriteria = autoAssignmentCriteria;
            this.autoAssignmentComparator = autoAssignmentCriteria.getComparator();
            this.criteriaValues = autoAssignmentCriteria.getCriteriaValues();
        }

        /**
         * Retrieves the criteria of a field.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @param storeType  Refers the store type of field.
         * @return The {@link Criteria}.
         */
        private Criteria build(final String fieldName, final Object fieldValue, final StoreType storeType) {
            return switch (storeType) {
                case TEXT, SINGLE_STRING, EMAIL, PHONE, ADDRESS, AUTO_NUMBER ->
                        buildTextFieldCriteria(fieldName, fieldValue, storeType);
                case SINGLE_NUMBER, MONETARY, DEAL_VALUE, PRODUCT, DEAL ->
                        buildNumberFieldCriteria(fieldName, fieldValue);
                case DROPDOWN, OWNER, COMPANY, CONTACT, TERRITORY, COMPANY_ONBOARD, CONTACT_ONBOARD ->
                        buildEntityTypeCriteria(fieldName, fieldValue, storeType);
                case EMAILS, PHONES -> buildArrayTypeCriteria(fieldName, fieldValue);
                case MULTI_SELECT, CONTACTS_ONBOARD, TAGS, MARKETING_LIST -> buildMultiValueCriteria(fieldName, fieldValue);
                case TIME, DATE, DATE_TIME -> buildDateTimeFieldCriteria(fieldName, fieldValue);
                default -> null;
            };
        }

        /**
         * builds the criteria for the text field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildTextFieldCriteria(final String fieldName, final Object fieldValue, final StoreType storeType) {
            final Function<Object, CriteriaData> criteriaDataProvider = criteriaValue -> {
                final String value = TEXT_NORMALIZER.apply(String.valueOf(criteriaValue));

                if (StoreType.ADDRESS == storeType && !value.isEmpty()) {
                    return new CriteriaData.Builder(fieldName, TRIMMED_VALUE.apply(value), ExpressionDataType.STRING).build();
                } else {
                    return new CriteriaData.Builder(fieldName, value, ExpressionDataType.STRING).build();
                }
            };

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.eq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notEqual" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "contains" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.contains(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notContains" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notContains(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "startsWith" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.startsWith(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "endsWith" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.endsWith(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notStartsWith" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notStartsWith(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notEndsWith" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEndsWith(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                default -> null;
            };
        }

        /**
         * builds the criteria for the number field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildNumberFieldCriteria(final String fieldName, final Object fieldValue) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, NumberUtils.parseNumber(criteriaValue), ExpressionDataType.NUMBER).build();

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.eq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notEqual" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "gt" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.gt(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "gte" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.ge(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "lt" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.lt(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "lte" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.le(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                default -> null;
            };
        }

        /**
         * builds the criteria for the date time field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildDateTimeFieldCriteria(final String fieldName, final Object fieldValue) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, NumberUtils.parseNumber(criteriaValue), ExpressionDataType.NUMBER).build();

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.eq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notEqual" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "earlierThan" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.lt(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "laterThan" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.gt(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "exactlyEarlierThan" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.le(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "exactlyLaterThan" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.ge(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                default -> null;
            };
        }

        /**
         * builds the criteria for the multi select field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildMultiValueCriteria(final String fieldName, final Object fieldValue) {
            final String compareFieldKey = String.format("%s_%d", fieldName, autoAssignmentCriteria.getId());
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.COLLECTION)
                            .compareFieldKey(compareFieldKey).build();

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal", "contains" -> {
                    final Collection<Long> values = criteriaValues.stream().filter(Objects::nonNull).map(autoAssignmentCriteriaValue ->
                            NUMBER_PARSER.apply(autoAssignmentCriteriaValue.getValue()).longValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.containsAll(criteriaDataProvider.apply(values));
                }
                case "notEqual", "notContains" -> {
                    final Collection<Long> values = criteriaValues.stream().filter(Objects::nonNull).map(autoAssignmentCriteriaValue ->
                            NUMBER_PARSER.apply(autoAssignmentCriteriaValue.getValue()).longValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.notContainsAll(criteriaDataProvider.apply(values));
                }
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                default -> null;
            };
        }

        /**
         * builds the criteria for the array type field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildArrayTypeCriteria(final String fieldName, final Object fieldValue) {
            final String compareFieldKey = String.format("%s_%d", fieldName, autoAssignmentCriteria.getId());
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.COLLECTION)
                            .compareFieldKey(compareFieldKey).build();

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal", "contains" -> {
                    final Collection<String> values = criteriaValues.stream().filter(Objects::nonNull)
                            .map(criteriaValue -> (String) criteriaValue.getValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.containsAll(criteriaDataProvider.apply(values));
                }
                case "notEqual", "notContains" -> {
                    final Collection<String> values = criteriaValues.stream().filter(Objects::nonNull)
                            .map(criteriaValue -> (String) criteriaValue.getValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.notContainsAll(criteriaDataProvider.apply(values));
                }
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                default -> null;
            };
        }

        /**
         * builds the criteria for the entity type field based on the specified comparator.
         *
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @param storeType  Refers the {@code StoreType}.
         * @return The {@link Criteria}.
         */
        private Criteria buildEntityTypeCriteria(final String fieldName, final Object fieldValue, final StoreType storeType) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.NUMBER).build();

            return switch (autoAssignmentComparator.getAliasName()) {
                case "equal" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.eq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "notEqual" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEq(criteriaDataProvider.apply(criteriaValue.getValue()))).orElseGet(() -> null);
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "any" -> {
                    final String compareFieldKey = String.format("%s_%d", fieldName, autoAssignmentCriteria.getId());
                    final Collection<Number> values = criteriaValues.stream().map(criteriaValue -> NUMBER_PARSER.apply(criteriaValue.getValue())).collect(Collectors.toSet());

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.contains(new CriteriaData.Builder(fieldName, fieldValue, ExpressionDataType.COLLECTION).compareFieldKey(compareFieldKey).build());
                }
                default -> null;
            };
        }
    }

    /**
     * Gets the value of persistent fields.
     *
     * @param value     Refers the value of a field of an entity.
     * @param fieldInfo Refers the {@link FieldInfo}.
     * @return The value of a field.
     */
    protected final Object getPersistentFieldValue(final Object value, final FieldInfo fieldInfo) {
        final StoreType storeType = fieldInfo.storeType();

        switch (storeType) {
            case MULTI_SELECT -> {
                if (null == value) {
                    return List.of();
                } else if (value instanceof final Collection<?> models) {
                    return models.stream().map(model -> (Option) model).map(Option::id)
                            .collect(Collectors.toUnmodifiableSet());
                }
            }
            case COMPANY, CONTACT, OWNER, DROPDOWN, SINGLE_NUMBER, DATE, TIME -> {
                if (null == value) {
                    return null;
                } else if (value instanceof final Number number) {
                    return number;
                } else if (value instanceof final TlcModel tlcModel) {
                    return tlcModel.id();
                } else if (value instanceof final Temporal temporal) {
                    return temporal.getTimeStamp();
                }
            }
            case MONETARY -> {
                if (null == value) {
                    return null;
                } else if (value instanceof final Monetary monetary) {
                    return monetary.getValue();
                }
            }
            case EMAIL, PHONE, ADDRESS, TEXT, SINGLE_STRING, AUTO_NUMBER -> {
                if (null == value) {
                    return "";
                } else if (value instanceof final String string) {
                    return string.toLowerCase();
                } else if (value instanceof TlcModel tlcModel) {
                    if (tlcModel instanceof final Email email) {
                        return email.getValue();
                    } else if (tlcModel instanceof final Phone phone) {
                        return phone.getNumber();
                    } else if (tlcModel instanceof final Address address) {
                        return constructAddressString(address);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Constructs a string representation of the given {@link Address}.
     *
     * @param address Refers the {@link Address}.
     * @return The formatted address string.
     */
    protected final String constructAddressString(final Address address) {
        final StringBuilder addressBuilder = new StringBuilder();

        Optional.ofNullable(TRIMMED_VALUE.apply(address.getAddressLine1())).ifPresent(addressBuilder::append);
        Optional.ofNullable(TRIMMED_VALUE.apply(address.getAddressLine2())).ifPresent(addressBuilder::append);
        Optional.ofNullable(TRIMMED_VALUE.apply(address.getCity())).ifPresent(addressBuilder::append);
        Optional.ofNullable(TRIMMED_VALUE.apply(address.getState())).ifPresent(addressBuilder::append);
        Optional.ofNullable(TRIMMED_VALUE.apply(address.getZipcode())).ifPresent(addressBuilder::append);

        return addressBuilder.toString().toLowerCase();
    }

    /**
     * Retrieves the ID of the most recent activity type for the given organization.
     *
     * @param orgId           The ID of the organization.
     * @param activitySummary The summary containing activity data.
     * @return The ID of the last activity type, or null if not found.
     */
    protected final Long getLastActivityTypeId(final long orgId, final ActivitySummary activitySummary) {
        final String lastActivityType = activitySummary.getLastActivityType();

        if (Objects.nonNull(lastActivityType)) {
            return activityTypeManager.fetchActivityTypeByName(orgId, List.of(activitySummary.getLastActivityType()))
                    .stream().findFirst().map(ActivityType::id).orElseGet(() -> null);
        }
        return null;
    }

    /**
     * Retrieves the value of the specified field from a model.
     *
     * @param model   Refers the model from which the value is fetched.
     * @param fieldId Refers the field id.
     * @return The field value.
     */
    protected abstract Object getValue(final T model, final Long fieldId);

    /**
     * Converts a given long value to a {@link Long}, returning {@code null} if the value is zero.
     *
     * @param value The input long value.
     * @return {@code null} if the input is zero; otherwise, returns the input as a {@link Long}.
     */
    protected final Long nullifyZero(final long value) {
        return 0 == value ? null : value;
    }

}
