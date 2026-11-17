package com.tlc.crm.core.action.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.ListRowListener;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ListDataContainer;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the product list action in the deal summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.PRODUCTS, validateFieldPermission = true)
@WebAction(path = "/deal/product/list")
public final class DealSummaryProductListAction extends DealSummaryListAction {

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.PRODUCT;
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    @Override
    protected ListType getListType() {
        return ListType.DEAL_SUMMARY_PRODUCT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@code ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     * @param crmResponse     Holds the {@link CrmResponse}.
     * @param provider        Refers the {@code Provider}.
     */
    @Override
    protected void loadResponse(final ListRequestData listRequestData, final EntityListInterpreter listInterpreter,
                                final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final ListDataContainer listDataContainer = fetchCrmListResponse(listRequestData, listInterpreter);
        final Collection<Long> userIds = listDataContainer.getUserIds();
        final Map<String, Map<String, Object>> rows = groupRowsByPk(listInterpreter, listDataContainer, getRowListener());
        final Collection<Map<String, Object>> entityRows = rows.values();

        loadDynamicFields(listInterpreter, rows);
        postProcess(provider, listRequestData, response);

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            response.put(ListActionFields.ROWS, entityRows);
            crmResponse.put(ListActionFields.TABLE, response);
        } else {
            dispatchEntities(listRequestData, userIds, entityRows, response, crmResponse, provider);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Deal product rows are keyed by product detail id, so the dynamic fields are matched using the product id of each row
     * </p>
     *
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param entityRows      Represents the deal product rows keyed by product detail id
     */
    @Override
    protected void loadDynamicFields(final EntityListInterpreter listInterpreter, final Map<String, Map<String, Object>> entityRows) {
        final Map<String, Map<String, Object>> dynamicFieldsByProductId = listInterpreter.fetchDynamicFields();

        if (dynamicFieldsByProductId.isEmpty()) {
            return;
        }

        for (final Map<String, Object> entityRow : entityRows.values()) {
            final Object productId = entityRow.get(ListActionFields.ID);

            if (Objects.nonNull(productId)) {
                final Map<String, Object> dynamicFieldsRow = dynamicFieldsByProductId.get(String.valueOf(NumberUtils.parseNumber(productId).longValue()));

                if (Objects.nonNull(dynamicFieldsRow) && !dynamicFieldsRow.isEmpty()) {
                    entityRow.putAll(dynamicFieldsRow);
                }
            }
        }
    }

    /**
     * Groups the rows from the provided list container by their primary key values.
     *
     * @param listInterpreter   interpreter used to resolve entity metadata and
     *                          extract primary key information from rows
     * @param listDataContainer container holding the list rows to be processed
     * @param rowListener       listener invoked during row processing to handle
     *                          row-specific transformations or callbacks
     * @return a map where:
     * <ul>
     *     <li>Key = primary key value of the row</li>
     *     <li>Value = map containing field names and their corresponding values</li>
     * </ul>
     */
    private Map<String, Map<String, Object>> groupRowsByPk(final EntityListInterpreter listInterpreter, final ListDataContainer listDataContainer, final ListRowListener rowListener) {
        final Map<String, Map<String, Object>> data = new LinkedHashMap<>();
        final ListStructure structure = listInterpreter.structure();
        final long orgId = listDataContainer.getOrgId();

        for (final Map<String, Object> row : listDataContainer.getData()) {
            final Long pk = (Long) row.get(ListActionFields.PRODUCT_DETAIL_ID);
            final Map<String, Object> convertedRow = new HashMap<>();

            row.forEach((key, value) -> {
                final FieldTransformer transformer = structure.getTransformer(key);

                if (transformer != null) {
                    convertedRow.put(key, listDataContainer.resolveValue(transformer.transform(orgId, key, row)));
                } else if (!key.contains("_")) {
                    convertedRow.put(key, listDataContainer.resolveValue(value));
                }
            });

            if (rowListener != null) {
                rowListener.call(pk, Json.object(convertedRow));
            }
            data.put(pk.toString(), convertedRow);
        }

        return data;
    }
}