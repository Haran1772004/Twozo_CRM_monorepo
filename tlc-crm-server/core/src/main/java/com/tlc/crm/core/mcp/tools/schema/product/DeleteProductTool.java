package com.tlc.crm.core.mcp.tools.schema.product;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MCP Tool for deleting product from CRM.
 * Accepts a product ID and removes the product from the system.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "delete_product",
        title = "Delete Product",
        description = "Delete a product from the CRM system. This action is permanent and cannot be undone.",
        version = "1.0",
        destructive = true
)
public class DeleteProductTool extends CrmSecureMcpTool<Product> {

    private final ProductManager productManager;

    public DeleteProductTool() {
        this.productManager = ProductManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Product> getConfigManager() {
        return productManager;
    }

    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(description = "Product ID to delete", required = true) final Long id,
                                 @ToolParam(description = "Must be true to confirm deletion (safety check)", required = true,
                                         options = {"true", "false"}) final boolean confirmDelete) {
        try {
            if (id == null || id <= 0) {
                return errorResult("Invalid product ID");
            }

            if (!confirmDelete) {
                return errorResult("Deletion must be explicitly confirmed. Set confirmDelete to true.");
            }

            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();

            if (!productManager.exists(orgId, id)) {
                return errorResult("Product not found");
            }

            final EventData eventData = new SecureEventData(orgId, userId);
            final Product product = productManager.get(orgId, id);

            eventData.setLocale(Locale.getDefault());
            eventData.setPreviousValues(List.of(product));
            final JsonObject jsonObject = delete(orgId, userId, eventData, List.of(product));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Product delete action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to delete product");
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
        return isAuthorized(ctx) && isSourceEnabled(ctx.identity(), Source.PRODUCT) && hasPermission(ctx, SubCategory.Type.PRODUCTS, List.of(Type.DELETE, Type.VIEW));
    }
}
