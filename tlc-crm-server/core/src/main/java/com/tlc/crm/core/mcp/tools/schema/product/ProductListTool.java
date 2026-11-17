package com.tlc.crm.core.mcp.tools.schema.product;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.mcp.tools.AbstractEntityListTool;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool — List Products.
 *
 * <p>Lists product records from the CRM with optional filter criteria, sorting,
 * and pagination. Built on the same {@link com.tlc.crm.entity.list.EntitySQLListInterpreter}
 * and {@link com.tlc.crm.entity.list.EntityListAction} infrastructure used by the
 * standard Product List REST API.</p>
 *
 * <p><b>IMPORTANT for calling LLMs:</b>
 * <ul>
 *   <li>For relative date filters, use {@code dateInterval} directly (optional {@code dateField}, default createdTime).</li>
 *   <li>When using other filter criteria, call {@code get_list_schema(module='product')} once — includes dateIntervals.</li>
 *   <li>For dropdown/owner fields, call {@code get_field_options} or {@code search_users} to resolve value IDs.</li>
 * </ul>
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "list_products",
        title = "List Products",
        description = "Search and list product records from the CRM with optional filter criteria, sorting, and pagination. "
                + "For unfiltered lists, call directly with start/count. "
                + "For relative date filters (today, yesterday, this week, this month), use dateInterval directly (optional dateField, default created_at). "
                + "When using other filter criteria, call get_list_schema(module='product') first — it includes dateIntervals. "
                + "For dropdown/owner fields, call get_field_options or search_users to resolve value IDs. "
                + "Supports sorting by any sortable field and pagination with start/count.",
        readOnly = true,
        idempotent = true
)
public class ProductListTool extends AbstractEntityListTool {

    private final ProductConverter productConverter;
    private final ProductManager productManager;

    public ProductListTool() {
        this.productConverter = ProductConverter.getInstance();
        this.productManager = ProductManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.PRODUCT_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModuleName() {
        return "product";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonArray fetchDataRows(final long orgId, final long userId, final Collection<Long> entityIds) {
        final Map<Long, Product> products = productManager.get(orgId, entityIds).stream().collect(Collectors.toMap(Product::id, Function.identity()));
        final JsonArray array = Json.array();
        final ConverterConfig converterConfig = new ConverterConfig(orgId, userId, Locale.getDefault(), false, null);

        for (final Long modelId : entityIds) {
            final Product product = products.get(modelId);

            array.put(productConverter.convertToRestJson(product, converterConfig));
        }
        return array;
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx the MCP tool context
     * @return
     */
    @Override
    public boolean isVisible(final McpToolContext<CrmMcpIdentity> ctx) {
        return isAuthorized(ctx) && isSourceEnabled(ctx.identity(), Source.PRODUCT) && hasPermission(ctx, SubCategory.Type.PRODUCTS, List.of(Action.Type.VIEW));
    }
}