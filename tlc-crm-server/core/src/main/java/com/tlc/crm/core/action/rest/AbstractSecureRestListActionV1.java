package com.tlc.crm.core.action.rest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.action.rest.SecureRestListAction;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Deque;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * V1 implementation of {@link SecureRestListAction} that converts legacy {@code filter} query
 * parameters into the JSON criteria format expected by the list/filter infrastructure.
 * <p>
 * Filter tokens are expected to follow the syntax:
 * <pre>
 *     &lt;field&gt;:&lt;comparator&gt;(&lt;value&gt;[, &lt;value&gt;]*)
 * </pre>
 * where {@code field} can be either:
 * <ul>
 *     <li>a numeric id (treated as a field id), or</li>
 *     <li>an internal name resolved against the current {@link Source}.</li>
 * </ul>
 * <p>
 * This parser is intentionally lenient: unresolvable fields do not contribute to the generated
 * {@link ListActionFields#CRITERIA} array.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public abstract class AbstractSecureRestListActionV1 extends SecureRestListAction {

    /**
     * {@inheritDoc}
     *
     * @param orgId        Organization id used while resolving numeric field ids.
     * @param requestJson  request JSON to be mutated.
     * @param filterParams Raw filter tokens from the query string.
     */
    @Override
    protected void formatToFilterJson(final Long orgId, final JsonObject requestJson, final Deque<String> filterParams) {
        final JsonArray array = Json.array();

        for (final String filterParam : filterParams) {
            final JsonObject jsonObject = Json.object();
            final String[] split = filterParam.split(":");
            final String field = split[0].trim();
            final String typeWithValue = split[1].trim();
            final int openBracketIndex = typeWithValue.indexOf('(');
            final String comparatorKey = typeWithValue.substring(0, openBracketIndex);
            final Source source = getSource();
            final Optional<FieldInfo> optionalFieldInfo = TypeChecker.isNumber(field)
                    ? Optional.of(fieldService.getField(orgId, NumberUtils.parseNumber(field).longValue()))
                    : fieldService.getDefaultFieldByInternalName(source, field);

            if (optionalFieldInfo.isPresent()) {
                final FieldInfo fieldInfo = optionalFieldInfo.get();
                final Comparator comparator = comparatorService.getByName(getComparatorName(comparatorKey));
                final String[] values = typeWithValue.substring(openBracketIndex + 1, typeWithValue.indexOf(')')).split("\\s*,\\s*");

                jsonObject.put(ListActionFields.FIELD, Json.object().put(ListActionFields.FIELD_ID, fieldInfo.id()));
                jsonObject.put(ListActionFields.SOURCE_ID, fieldSourceService.getSource(getSource().getName()).id());
                jsonObject.put(ListActionFields.CONNECTOR_ID, FilterConnector.AND.getId());
                jsonObject.put(ListActionFields.COMPARATOR_ID, comparator.getId());

                if (values.length >= 1) {
                    final JsonArray valueArray = Json.array();

                    for (final String value : values) {
                        if (!value.equalsIgnoreCase("null")) {
                            final JsonObject valueJson = Json.object();

                            switch (fieldInfo.storeType()) {
                                case DROPDOWN, MULTI_SELECT -> valueJson.put(ListActionFields.VALUE_ENTITY_ID, value);
                                default -> valueJson.put(ListActionFields.VALUE, value);
                            }

                            valueArray.put(valueJson);
                        }
                    }

                    if (valueArray.size() > 0) {
                        jsonObject.put(ListActionFields.VALUES, valueArray);
                    }
                }
                array.put(jsonObject);
            }
        }
        requestJson.put(ListActionFields.CRITERIA, array);
    }

    /**
     * Applies the sort details to the request json.
     *
     * @param sort        Represents the sort details in the format "field:sortType,field:sortType".
     * @param requestJson Represents the request json to which the sort details need to be applied.
     */
    protected void applySort(final long orgId, final String sort, final JsonObject requestJson) {
        final JsonArray sortArray = Json.array();
        final String[] sortList = sort.split(",");
        final Collection<String> fieldInternalNames = new HashSet<>();

        Arrays.stream(sortList).forEach(sortData -> {
            final String[] sortField = sortData.trim().split(":");

            fieldInternalNames.add(sortField[0].trim());
        });

        final Map<String, FieldInfo> fields = fieldService.getFieldsByInternalNames(orgId, fieldInternalNames).stream()
                .collect(Collectors.toMap(FieldInfo::getInternalName, Function.identity()));

        for (final String sortData : sortList) {
            final String[] sortField = sortData.trim().split(":");
            final JsonObject jsonObject = Json.object();
            final FieldInfo fieldInfo = fields.get(sortField[0].trim());

            jsonObject.put(ApiKeys.FIELD, fieldInfo.isPersistent() ? fieldInfo.id() : fieldInfo.aliasName());
            jsonObject.put(ApiKeys.TYPE, sortField[1].trim());

            sortArray.put(jsonObject);
        }
        requestJson.put(ApiKeys.SORT, sortArray);
    }
}
