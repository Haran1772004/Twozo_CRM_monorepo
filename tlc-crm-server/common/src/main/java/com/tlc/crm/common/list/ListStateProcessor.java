package com.tlc.crm.common.list;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.list.pagination.PaginationState;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.pagination.ListPaginationStateManager;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.ListSortConfig;

import java.util.Objects;

/**
 * <p>
 * A service class that provides methods to process pagination and sort configurations for list views.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public final class ListStateProcessor {

    private static final int DEFAULT_START = NumericalConstant.ONE;
    private static final int DEFAULT_COUNT = NumericalConstant.TWENTY_FIVE;

    private final ListPaginationStateManager listPaginationStateManager;
    private final SortManager sortManager;

    private ListStateProcessor() {
        this.listPaginationStateManager = ListPaginationStateManager.getInstance();
        this.sortManager = SortManager.getInstance();
    }

    public static ListStateProcessor getInstance() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final ListStateProcessor INSTANCE = new ListStateProcessor();
    }

    /**
     * <p>
     * Processes the pagination state and returns the updated limit JSON object.
     * </p>
     *
     * @param requestData         the input JSON object containing pagination data
     * @param listPaginationState the current pagination state
     * @return the updated limit JSON object
     */
    public JsonObject processAndGetLimitJson(final JsonObject requestData, final ListPaginationState listPaginationState) {
        final JsonObject limitJson = requestData.optJsonObject(ListActionFields.LIMIT);
        final PaginationState storedPaginationState = listPaginationStateManager.get(listPaginationState);
        final JsonObject processedLimitJson = Json.object();

        if (Objects.isNull(limitJson)) {
            processedLimitJson.put(ListActionFields.START, storedPaginationState.getStart());
            processedLimitJson.put(ListActionFields.COUNT, storedPaginationState.getCount());
        } else {
            final int start = limitJson.optInt(ListActionFields.START, DEFAULT_START);
            final int count = limitJson.optInt(ListActionFields.COUNT, DEFAULT_COUNT);

            if (storedPaginationState.getStart() != start
                    || storedPaginationState.getCount() != count) {
                listPaginationState.setStart(start);
                listPaginationState.setCount(count);

                listPaginationStateManager.update(listPaginationState);
            }
            processedLimitJson.put(ListActionFields.START, start);
            processedLimitJson.put(ListActionFields.COUNT, count);
        }

        return processedLimitJson;
    }

    /**
     * <p>
     * Processes the sort configuration and returns the updated sort JSON object.
     * </p>
     *
     * @param data           the input JSON object containing sort data
     * @param listSortConfig the current sort configuration
     * @return the updated sort JSON object
     */
    public JsonObject processAndGetSortJson(final JsonObject data, final ListSortConfig listSortConfig) {
        final JsonObject sortJson = data.optJsonObject(ListActionFields.SORT);

        if (Objects.isNull(sortJson)) {

            if (data.containsKey(ListActionFields.IS_SORT_ENABLED)
                    && !data.getBoolean(ListActionFields.IS_SORT_ENABLED)) {
                sortManager.remove(listSortConfig);
            } else {
                return sortManager.fetchCriteriaList(listSortConfig).stream().findFirst().map(
                        criteria -> Json.object()
                                .put(ListActionFields.FIELD, criteria.getFieldName())
                                .put(ListActionFields.TYPE, criteria.getSortType().getOrderTypeName()))
                        .orElse(null);
            }
        } else {
            listSortConfig.setConfig(Json.array().put(sortJson).toString());

            sortManager.update(listSortConfig);
        }

        return sortJson;
    }
}