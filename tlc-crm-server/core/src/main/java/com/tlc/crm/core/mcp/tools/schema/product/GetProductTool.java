package com.tlc.crm.core.mcp.tools.schema.product;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import java.util.List;
import java.util.Locale;

/**
 * MCP Tool for retrieving product details from CRM.
 * Accepts a product ID and returns all details of the product.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_product",
        title = "Get Product",
        description = "Retrieve detailed information about a specific product from the CRM system by its ID",
        version = "1.0",
        readOnly = true,
        idempotent = true
)
public class GetProductTool extends CrmSecureMcpTool<Product> {

    private final ProductManager productManager;
    private final ProductConverter productConverter;

    public GetProductTool() {
        this.productManager = ProductManager.getInstance();
        this.productConverter = ProductConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Product> getConfigManager() {
        return productManager;
    }

    /**
     * Execute product retrieval.
     * Fetches product details by ID.
     *
     * @param ctx MCP tool context containing identity
     * @param id  Product ID to retrieve (required)
     * @return Tool result with product details
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(description = "Product ID to retrieve") final Long id) {
        try {
            if (id == null || id <= 0) {
                return errorResult("Invalid product ID");
            }
            final CrmUser crmUser = ctx.identity().crmUser();
            final Long orgId = crmUser.orgId();
            final Product product = productManager.get(orgId, id, false);

            if (product == null) {
                return errorResult("Product not found");
            }

            final ConverterConfig converterConfig = new ConverterConfig(orgId, crmUser.id(), Locale.getDefault(), false, null);
            final JsonObject jsonObject = productConverter.convertToRestJson(product, converterConfig);

            return McpToolResult.json(jsonObject);
        } catch (Exception e) {
            LOGGER.error("MCP: Product get action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to retrieve product: " + e.getMessage());
        }
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