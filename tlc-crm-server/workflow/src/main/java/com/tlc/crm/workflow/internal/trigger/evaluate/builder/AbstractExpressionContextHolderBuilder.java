package com.tlc.crm.workflow.internal.trigger.evaluate.builder;

import com.tlc.commons.pair.Pair;

import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.validator.TlcModel;
import com.tlc.validator.expression.Criteria;
import com.tlc.validator.expression.model.ExpressionContextHolder;
import com.tlc.validator.expression.model.ExpressionDataType;
import com.tlc.validator.expression.model.CriteriaData;
import com.tlc.validator.expression.service.Expression;
import com.tlc.validator.expression.service.CriteriaExpression;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;

import com.tlc.crm.core.features.api.activity.model.ActivitySummary;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;

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
    private static final String NON_ALPHABETIC_PATTERN = "[^a-zA-Z]";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpressionContextHolderBuilder.class);

    protected final FieldService fieldService;
    protected final WorkflowFieldService workflowFieldService;
    protected final ActivityTypeManager activityTypeManager;

    private final TriggerSource triggerSource;

    protected AbstractExpressionContextHolderBuilder(final TriggerSource triggerSource) {
        this.triggerSource = triggerSource;
        this.fieldService = FieldService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
    }

    /**
     * Fetches the {@link ExpressionContextHolder} by constructing the expression of an entity.
     *
     * @param modelPair    Refers to the {@link Pair} consisting of an old and current model of the same entity.
     * @param criteriaSets Refers the {@link WorkflowCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    protected final ExpressionContextHolder build(final Pair<T, T> modelPair,
                                                  final Collection<WorkflowCriteriaSet> criteriaSets) {
        final T model = modelPair.getKey();
        final Long orgId = model.orgId();

        LOGGER.info("Workflow Execution: Expression builder initiated for Org ID: {}, Entity: {}, ID: {}", orgId, triggerSource, model.id());

        final Map<String, Object> context = new HashMap<>();
        final BiConsumer<String, Object> valueInjector = (fieldName, value) -> {
            context.put(fieldName, Objects.nonNull(value) ? value : null);
        };
        final CriteriaBuilder criteriaBuilder = new CriteriaBuilder(modelPair.getValue(), context, new CriteriaExpression());
        final boolean unregisteredSource = TriggerSource.ACTIVITY == triggerSource;
        final int criteriaSetSize = criteriaSets.size();
        final long startTime = System.currentTimeMillis();

        Expression expression = null;
        CriteriaConnector setConnector = null;
        int criteriaSetIndex = 1;

        for (final WorkflowCriteriaSet criteriaSet : criteriaSets) {
            final Collection<WorkflowCriteria> criteriaList = criteriaSet.getCriteriaList();
            final int criteriaSize = criteriaList.size();

            Expression criteriaExpression = null;
            CriteriaConnector criteriaConnector = null;
            int criteriaIndex = 1;

            for (final WorkflowCriteria workflowCriteria : criteriaSet.getCriteriaList()) {
                final WorkflowCriteriaField workflowCriteriaField = workflowCriteria.getCriteriaField();
                final String fieldName;
                final Long fieldId;
                final StoreType storeType;

                if (unregisteredSource) {
                    final WorkflowField workflowField = workflowFieldService.get(workflowCriteriaField.getFieldId());

                    fieldName = workflowField.getAliasName();
                    fieldId = workflowField.getId();
                    storeType = workflowField.getFieldType().storeType();
                } else {
                    final FieldInfo fieldInfo = fieldService.getField(orgId, workflowCriteriaField.getFieldId());

                    fieldName = fieldInfo.isPersistent() ? resolvePersistentFieldName(fieldInfo) : fieldInfo.aliasName();
                    fieldId = fieldInfo.id();
                    storeType = Optional.ofNullable(fieldInfo.presetConfig().getAutomationConfig())
                            .map(config -> StoreType.valueOf(config.getString(WorkflowAttributes.FIELD_TYPE))).orElseGet(fieldInfo::storeType);
                }
                final Object fieldValue = getValue(model, fieldId);

                valueInjector.accept(fieldName, fieldValue);
                criteriaBuilder.setCriteriaDetails(workflowCriteria);

                final Criteria criteria = criteriaBuilder.build(fieldId, fieldName, fieldValue, storeType);

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
                criteriaConnector = workflowCriteria.getConnector();
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
        LOGGER.debug("Workflow Execution: Conversion of criteria to Expression took {} milliseconds for Org ID: {}, Entity: {}, ID: {}",
                System.currentTimeMillis() - startTime, orgId, triggerSource, model.id());

        return new ExpressionContextHolder(expression, context);
    }

    /**
     * <p>
     * Resolves the field name of the custom field and returns it
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return the resolved field name
     */
    private String resolvePersistentFieldName(final FieldInfo fieldInfo) {
        final String resolvedFieldName = fieldInfo.fieldName().replaceAll(NON_ALPHABETIC_PATTERN, EMPTY_STRING);

        return String.join(EMPTY_STRING, resolvedFieldName.isBlank() ? WorkflowAttributes.PERSISTENT : resolvedFieldName, fieldInfo.idAsString());
    }

    /**
     * Retrieves the necessary {@link Criteria} for the expression identified
     * by various store types and fields.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private class CriteriaBuilder {

        private final T oldModel;
        private final Map<String, Object> context;
        private final Criteria expressionCriteria;

        private WorkflowCriteria workflowCriteria;
        private WorkflowComparator workflowComparator;
        private Collection<WorkflowCriteriaValue> criteriaValues;

        private CriteriaBuilder(final T oldModel, final Map<String, Object> context,
                                final Criteria expressionCriteria) {
            this.oldModel = oldModel;
            this.context = context;
            this.expressionCriteria = Objects.requireNonNull(expressionCriteria);
        }

        /**
         * Sets the criteria details.
         *
         * @param workflowCriteria Refers the {@link WorkflowCriteria}.
         */
        private void setCriteriaDetails(final WorkflowCriteria workflowCriteria) {
            this.workflowCriteria = workflowCriteria;
            this.workflowComparator = workflowCriteria.getComparator();
            this.criteriaValues = workflowCriteria.getCriteriaValues();
        }

        /**
         * Retrieves the criteria of a field.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @param storeType  Refers the store type of field.
         * @return The {@link Criteria}.
         */
        private Criteria build(final Long fieldId, final String fieldName, final Object fieldValue, final StoreType storeType) {
            return switch (storeType) {
                case TEXT, SINGLE_STRING, EMAIL, PHONE, ADDRESS, AUTO_NUMBER ->
                        buildTextFieldCriteria(fieldId, fieldName, fieldValue, storeType);
                case SINGLE_NUMBER, MONETARY, DEAL_VALUE, PRODUCT, DEAL ->
                        buildNumberFieldCriteria(fieldId, fieldName, fieldValue);
                case DROPDOWN, OWNER, COMPANY, CONTACT, TERRITORY, COMPANY_ONBOARD, CONTACT_ONBOARD ->
                        buildEntityTypeCriteria(fieldId, fieldName, fieldValue, storeType);
                case EMAILS, PHONES -> buildArrayTypeCriteria(fieldId, fieldName, fieldValue);
                case MULTI_SELECT, CONTACTS_ONBOARD, TAGS, MARKETING_LIST-> buildMultiValueCriteria(fieldId, fieldName, fieldValue);
                case TIME, DATE -> buildDateTimeFieldCriteria(fieldId, fieldName, fieldValue);
                default -> null;
            };
        }

        /**
         * builds the criteria for the text field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildTextFieldCriteria(final Long fieldId, final String fieldName,
                                                final Object fieldValue, final StoreType storeType) {
            final Function<Object, CriteriaData> criteriaDataProvider = criteriaValue -> {
                final String value = TEXT_NORMALIZER.apply(String.valueOf(criteriaValue));

                if (StoreType.ADDRESS == storeType && !value.isEmpty()) {
                    return new CriteriaData.Builder(fieldName, TRIMMED_VALUE.apply(value), ExpressionDataType.STRING).build();
                } else {
                    return new CriteriaData.Builder(fieldName, value, ExpressionDataType.STRING).build();
                }
            };

            return switch (workflowComparator.getAliasName()) {
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
                case "changed" -> expressionCriteria.changed(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                case "notChanged" ->
                        expressionCriteria.notChanged(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                default -> null;
            };
        }

        /**
         * builds the criteria for the number field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildNumberFieldCriteria(final Long fieldId, final String fieldName, final Object fieldValue) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, NumberUtils.parseNumber(criteriaValue), ExpressionDataType.NUMBER).build();

            return switch (workflowComparator.getAliasName()) {
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
                case "changed" -> expressionCriteria.changed(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                case "notChanged" ->
                        expressionCriteria.notChanged(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                default -> null;
            };
        }

        /**
         * builds the criteria for the date time field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildDateTimeFieldCriteria(final Long fieldId, final String fieldName, final Object fieldValue) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, NumberUtils.parseNumber(criteriaValue), ExpressionDataType.NUMBER).build();

            return switch (workflowComparator.getAliasName()) {
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "changed" -> expressionCriteria.changed(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                case "notChanged" ->
                        expressionCriteria.notChanged(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                default -> null;
            };
        }

        /**
         * builds the criteria for the multi select field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildMultiValueCriteria(final Long fieldId, final String fieldName, final Object fieldValue) {
            final String compareFieldKey = String.format("%s_%d", fieldName, workflowCriteria.getId());
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.COLLECTION)
                            .compareFieldKey(compareFieldKey).build();

            return switch (workflowComparator.getAliasName()) {
                case "equal", "contains" -> {
                    final Collection<Long> values = criteriaValues.stream().filter(Objects::nonNull).map(workflowCriteriaValue ->
                            NUMBER_PARSER.apply(workflowCriteriaValue.getValueId()).longValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.containsAll(criteriaDataProvider.apply(values));
                }
                case "notEqual", "notContains" -> {
                    final Collection<Long> values = criteriaValues.stream().filter(Objects::nonNull).map(workflowCriteriaValue ->
                            NUMBER_PARSER.apply(workflowCriteriaValue.getValueId()).longValue()).toList();

                    context.put(compareFieldKey, values);
                    yield expressionCriteria.notContainsAll(criteriaDataProvider.apply(values));
                }
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "changed" -> {
                    final Object value = getValue(oldModel, fieldId);

                    context.put(compareFieldKey, value);
                    yield expressionCriteria.changed(criteriaDataProvider.apply(value));
                }
                case "notChanged" -> {
                    final Object value = getValue(oldModel, fieldId);

                    context.put(compareFieldKey, value);
                    yield expressionCriteria.notChanged(criteriaDataProvider.apply(value));
                }
                default -> null;
            };
        }

        /**
         * builds the criteria for the array type field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @return The {@link Criteria}.
         */
        private Criteria buildArrayTypeCriteria(final Long fieldId, final String fieldName, final Object fieldValue) {
            final String compareFieldKey = String.format("%s_%d", fieldName, workflowCriteria.getId());
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.COLLECTION)
                            .compareFieldKey(compareFieldKey).build();

            return switch (workflowComparator.getAliasName()) {
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
                case "changed" -> {
                    final Object value = getValue(oldModel, fieldId);

                    context.put(compareFieldKey, value);
                    yield expressionCriteria.changed(criteriaDataProvider.apply(value));
                }
                case "notChanged" -> {
                    final Object value = getValue(oldModel, fieldId);

                    context.put(compareFieldKey, value);
                    yield expressionCriteria.notChanged(criteriaDataProvider.apply(value));
                }
                default -> null;
            };
        }

        /**
         * builds the criteria for the entity type field based on the specified comparator.
         *
         * @param fieldId    Refers the field id.
         * @param fieldName  Refers the field name to set in the map context of an expression.
         * @param fieldValue Refers the field value to set in the map context of an expression.
         * @param storeType  Refers the {@code StoreType}.
         * @return The {@link Criteria}.
         */
        private Criteria buildEntityTypeCriteria(final Long fieldId, final String fieldName,
                                                 final Object fieldValue, final StoreType storeType) {
            final Function<Object, CriteriaData> criteriaDataProvider = (criteriaValue) ->
                    new CriteriaData.Builder(fieldName, criteriaValue, ExpressionDataType.NUMBER).build();

            final Function<WorkflowCriteriaValue, Long> resolveCriteriaValue = (criteria) ->
                    NUMBER_PARSER.apply(StoreType.OWNER == storeType ? criteria.getValue() : criteria.getValueId()).longValue();

            return switch (workflowComparator.getAliasName()) {
                case "equal" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.eq(criteriaDataProvider.apply(resolveCriteriaValue.apply(criteriaValue)))).orElseGet(() -> null);
                case "notEqual" -> criteriaValues.stream().findFirst().map(criteriaValue ->
                        expressionCriteria.notEq(criteriaDataProvider.apply(resolveCriteriaValue.apply(criteriaValue)))).orElseGet(() -> null);
                case "empty" -> expressionCriteria.empty(criteriaDataProvider.apply(fieldValue));
                case "notEmpty" -> expressionCriteria.notEmpty(criteriaDataProvider.apply(fieldValue));
                case "changed" -> expressionCriteria.changed(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
                case "notChanged" ->
                        expressionCriteria.notChanged(criteriaDataProvider.apply(getValue(oldModel, fieldId)));
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
}
