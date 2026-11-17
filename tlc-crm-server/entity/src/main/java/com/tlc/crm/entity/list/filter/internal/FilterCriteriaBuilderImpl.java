package com.tlc.crm.entity.list.filter.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.AdditionalOptionFields;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;
import com.tlc.crm.entity.filter.internal.DefaultFilter;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.filter.resolver.FilterValueResolver;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.entity.service.filter.FilterSourceService;
import com.tlc.sql.common.api.dml.Operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implements of the {@link FilterCriteriaBuilder} interface to provides the builder of the {@link FilterCriteria}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterCriteriaBuilderImpl implements FilterCriteriaBuilder {

    private final FilterFieldService filterFieldService;
    private final ComparatorService comparatorService;
    private final FilterSourceService filterSourceService;
    private final FilterManager filterManager;
    private final FieldService fieldService;

    private static class Instance {
        private static final FilterCriteriaBuilderImpl INSTANCE = new FilterCriteriaBuilderImpl();
    }

    private FilterCriteriaBuilderImpl() {
        filterFieldService = FilterFieldService.getSecureInstance();
        comparatorService = ComparatorService.getInstance();
        filterSourceService = FilterSourceService.getInstance();
        filterManager = FilterManager.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * <p>
     * Gets the {@link FilterCriteriaBuilderImpl} instance
     * </p>
     *
     * @return The {@link FilterCriteriaBuilderImpl}
     */
    public static FilterCriteriaBuilderImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param criteriaJson holds the values of criteria
     * @return The {@link FilterCriteria}
     */
    @Override
    public FilterCriteria build(final long orgId, final JsonObject criteriaJson) {
        final FilterCriteria filterCriteria = new FilterCriteria();
        final String fieldKey = CriteriaFields.FIELD.getName();
        final String comparatorIdKey = CriteriaFields.COMPARATOR_ID.getName();
        final String comparatorKey = CriteriaFields.COMPARATOR.getName();
        final String connectorIdKey = CriteriaFields.CONNECTOR_ID.getName();
        final String sourceIdKey = CriteriaFields.SOURCE_ID.getName();
        final String sourceKey = CriteriaFields.SOURCE.getName();
        final String valuesKey = CriteriaFields.VALUES.getName();

        if (criteriaJson.containsKey(sourceIdKey)) {
            filterCriteria.setFieldSource(filterSourceService.get(criteriaJson.getLong(sourceIdKey)));
        } else if (criteriaJson.containsKey(sourceKey)) {
            filterCriteria.setFieldSource(filterSourceService.getByName(criteriaJson.getString(sourceKey)));
        } else {
            throw ErrorCode.get(FilterErrorCodes.FIELD_SOURCE_ID_NOT_FOUND);
        }

        if (criteriaJson.containsKey(fieldKey)) {
            final JsonObject fieldJson = criteriaJson.optJsonObject(fieldKey);

            if (Objects.isNull(fieldJson)) {
                final FieldSource fieldSource = filterCriteria.getFieldSource();
                final Collection<CriteriaField> fields = filterFieldService.getFieldsByName(orgId, criteriaJson.getString(fieldKey), fieldSource.getName());
                final Optional<CriteriaField> optionalField = fields.stream().filter(field ->
                        Objects.equals(field.getSourceId(), fieldSource.id())).findFirst();

                filterCriteria.setCriteriaField(optionalField.orElseThrow(() -> ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND)));
            } else {
                final String fieldIdKey = FilterFieldAttributes.FIELD_ID.getName();
                final String additionalOptionKey = FilterFieldAttributes.ADDITIONAL_OPTION.getName();
                final CriteriaField criteriaField = fieldJson.containsKey(fieldIdKey) ? filterFieldService.getFieldById(orgId,
                        fieldJson.getLong(fieldIdKey), true, false) : filterFieldService.getFieldById(orgId,
                        fieldJson.getLong(FilterFieldAttributes.OPTION_ID.getName()), false, false);

                if (fieldJson.containsKey(additionalOptionKey)) {
                    criteriaField.setAdditionalOption(constructAdditionalOption(fieldJson.getJsonObject(additionalOptionKey)));
                }
                filterCriteria.setCriteriaField(criteriaField);
            }
        } else {
            throw ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND);
        }

        if (criteriaJson.containsKey(comparatorIdKey)) {
            filterCriteria.setComparator(comparatorService.get(criteriaJson.getLong(comparatorIdKey)));
        } else if (criteriaJson.containsKey(comparatorKey)) {
            filterCriteria.setComparator(comparatorService.getByName(criteriaJson.getString(comparatorKey)));
        } else {
            throw ErrorCode.get(FilterErrorCodes.COMPARATOR_NOT_FOUND);
        }

        if (criteriaJson.containsKey(connectorIdKey)) {
            filterCriteria.setConnector(FilterConnector.get(criteriaJson.getInt(connectorIdKey)));
        }

        if (criteriaJson.containsKey(valuesKey)) {
            filterCriteria.setCriteriaValues(constructFilterValues(criteriaJson.optJsonArray(valuesKey)));
        }

        return filterCriteria;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param provider     Represents the {@link Provider}
     * @param criteriaJson Which holds the values of criteria
     * @return The {@link Collection} of {@link FilterCriteria}
     */
    @Override
    public Collection<FilterCriteria> constructFilterCriteria(final long orgId, final long userId,
                                                              final Provider provider, final JsonObject criteriaJson) {
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final String filterIdKey = CriteriaFields.FILTER_ID.getName();
        final String filterCriteriaKey = CriteriaFields.CRITERIA.getName();
        final String additionalCriteriaKey = CriteriaFields.ADDITIONAL_CRITERIA.getName();
        final String ownerIdKey = CriteriaFields.OWNER_ID.getName();

        if (criteriaJson.containsKey(filterIdKey)) {
            final long filterId = criteriaJson.getLong(filterIdKey);
            final Filter filter = filterManager.get(orgId, filterId);
            final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();

            if (filter.isDefault() && Filter.Category.COMMON == filter.getCategory()) {
                final FilterValueResolver valueResolver = new FilterValueResolver(orgId, userId,
                        DefaultFilter.valueOf(filter.getAliasName()));

                for (final FilterCriteria filterCriteria : criteriaList) {
                    valueResolver.resolve(filterCriteria);
                    filterCriteriaList.add(filterCriteria);
                }
            } else {
                filterCriteriaList.addAll(criteriaList);
            }
            filterManager.updateRecentlyAppliedFilter(orgId, userId, filter.getFilterProvider().getId(), filterId);
        } else if (criteriaJson.containsKey(filterCriteriaKey)) {
            applyCriteria(orgId, provider, criteriaJson.getJsonArray(filterCriteriaKey), filterCriteriaList);
        } else if (criteriaJson.containsKey(ownerIdKey)) {
            final String salesOwnerKey = CriteriaFields.SALES_OWNER.getName();
            final FieldSource fieldSource = filterSourceService.getByName(provider.getSource().getName());
            final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(fieldSource.getName(),
                    salesOwnerKey);
            final FieldInfo fieldInfo = fieldInfoMap.get(salesOwnerKey);
            final JsonObject fieldJson = Json.object().put(CriteriaFields.FIELD_ID.getName(), fieldInfo.id());
            final JsonObject valueJson = Json.object().put(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(),
                    criteriaJson.getLong(ownerIdKey));
            final JsonArray jsonArray = Json.array().put(valueJson);
            final JsonObject jsonObject = Json.object();

            jsonObject.put(CriteriaFields.CONNECTOR_ID.getName(), FilterConnector.AND.getId());
            jsonObject.put(CriteriaFields.SOURCE_ID.getName(), fieldSource.id());
            jsonObject.put(CriteriaFields.COMPARATOR_ID.getName(), Operator.EQUAL.getId());
            jsonObject.put(CriteriaFields.VALUES.getName(), jsonArray);
            jsonObject.put(CriteriaFields.FIELD.getName(), fieldJson);

            filterCriteriaList.add(build(orgId, jsonObject));
        }

        if (criteriaJson.containsKey(additionalCriteriaKey)) {
            applyCriteria(orgId, provider, criteriaJson.getJsonArray(additionalCriteriaKey), filterCriteriaList);
        }

        return filterCriteriaList;
    }

    /**
     * <p>
     * Applies the criteria in the filter criteria list
     * </p>
     *
     * @param orgId              Represents an org id
     * @param provider           Represents the {@link Provider}
     * @param criteria           Which holds the values of criteria
     * @param filterCriteriaList Which contains the collection of {@link FilterCriteria}
     */
    private void applyCriteria(final long orgId, final Provider provider, final JsonArray criteria,
                               final Collection<FilterCriteria> filterCriteriaList) {
        final String sourceKey = CriteriaFields.SOURCE.getName();
        final String sourceName = provider.getSource().getName();
        final int size = criteria.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = criteria.getJsonObject(index);

            jsonObject.put(sourceKey, sourceName);
            filterCriteriaList.add(build(orgId, jsonObject));
        }
    }

    /**
     * <p>
     * Constructs the additional option from additional option json.
     * </p>
     *
     * @param additionalOptionJson which holds the values of additional option.
     * @return {@link AdditionalOption} instance.
     */
    private AdditionalOption constructAdditionalOption(final JsonObject additionalOptionJson) {
        final String nameKey = AdditionalOptionFields.NAME.getName();
        final String entityIdKey = AdditionalOptionFields.ENTITY_ID.getName();

        if (additionalOptionJson.containsKey(entityIdKey) && additionalOptionJson.containsKey(nameKey)) {
            final AdditionalOption additionalOption = new AdditionalOption();
            final String idKey = AdditionalOptionFields.ID.getName();
            final String additionalOptionKey = FilterFieldAttributes.ADDITIONAL_OPTION.getName();

            additionalOption.setOptionEntityId(additionalOptionJson.getLong(entityIdKey));
            additionalOption.setOptionName(additionalOptionJson.getString(nameKey));

            if (additionalOptionJson.containsKey(idKey)) {
                additionalOption.setId(additionalOptionJson.getLong(idKey));
            }

            if (additionalOptionJson.containsKey(additionalOptionKey)) {
                final JsonObject childAdditionalOptionJson = additionalOptionJson.getJsonObject(additionalOptionKey);

                additionalOption.setAdditionalOption(constructAdditionalOption(childAdditionalOptionJson));
            }

            return additionalOption;
        } else {
            throw ErrorCode.get(FilterErrorCodes.INVALID_ADDITIONAL_OPTION);
        }
    }

    /**
     * <p>
     * Constructs the {@link CriteriaValue} from values json array.
     * </p>
     *
     * @param values holds the values of criteria value object.
     * @return {@link CriteriaValue} instance.
     */
    private List<CriteriaValue> constructFilterValues(final JsonArray values) {
        final List<CriteriaValue> criteriaValues = new ArrayList<>();
        final String valueKey = CriteriaValueAttributes.VALUE.getName();
        final String valueEntityIdKey = CriteriaValueAttributes.VALUE_ENTITY_ID.getName();

        for (int index = 0; index < values.size(); index++) {
            final JsonObject value = values.getJsonObject(index);
            final CriteriaValue criteriaValue = new CriteriaValue();

            if (value.containsKey(valueEntityIdKey)) {
                criteriaValue.setValueEntityId(value.getLong(valueEntityIdKey));
            }

            if (value.containsKey(valueKey)) {
                criteriaValue.setValue(value.get(valueKey));
            }
            criteriaValues.add(criteriaValue);
        }

        return criteriaValues;
    }
}
