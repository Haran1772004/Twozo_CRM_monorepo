package com.tlc.crm.core.mcp.tools.schema.product;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.formatter.ProductRestReqFormatter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;
import com.tlc.crm.core.mcp.tools.schema.builder.ProductFieldSchemaBuilder;

import com.tlc.i18n.I18nResolver;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * MCP tool for updating a product in the CRM.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "update_product",
        title = "Update Product", description = "Update a product in the CRM. Pass a single `input` object containing default fields and org-specific custom fields."
)
public class UpdateProductTool extends CrmSecureMcpTool<Product> {

    private final ProductManager productManager;
    private final ProductConverter productConverter;
    private final ProductRestReqFormatter productRestReqFormatter;
    private final ProductFieldSchemaBuilder fieldSchemaBuilder;

    public UpdateProductTool() {
        this.productManager = ProductManager.getInstance();
        this.productConverter = ProductConverter.getInstance();
        this.productRestReqFormatter = ProductRestReqFormatter.getInstance();
        this.fieldSchemaBuilder = ProductFieldSchemaBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Product> getConfigManager() {
        return productManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx Refers the {@code McpToolContext<CrmMcpIdentity>}.
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx, final Map<String, Object> input) {
        try {
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();
            final JsonObject requestJson = buildRequestJson(input);
            final Locale locale = Locale.getDefault();
            final I18nResolver i18nResolver = i18nService.getResolver(locale);
            final Product product = convertPartialData(crmUser, requestJson, i18nResolver);
            final Collection<ErrorDescriptor> errorDescriptors = validate(List.of(product), Group.Update.class);

            if (!errorDescriptors.isEmpty()) {
                final List<String> errors = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                return errorResult("Validation failed:\n• " + String.join("\n• ", errors));
            }
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setLocale(locale);
            eventData.setPreviousValues(List.of(productManager.get(orgId, product.id())));
            final JsonObject jsonObject = update(orgId, userId, eventData, List.of(product));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Update product failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to update product: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser      The authenticated CRM user.
     * @param jsonObject   The partial input JSON payload.
     * @param i18nResolver The internationalization resolver.
     * @return
     */
    protected Product convertPartialData(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        final JsonObject productJson = productRestReqFormatter.format(crmUser.orgId(), jsonObject, i18nResolver);
        final ConverterConfig converterConfig = new ConverterConfig(crmUser.orgId(), crmUser.id(), Locale.getDefault(), true, productJson);

        return productConverter.convert(converterConfig, productJson);
    }

    /**
     * {@inheritDoc}
     *
     * @param generated the reflection-generated base schema
     * @param ctx       the MCP tool context
     * @return
     */
    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmMcpIdentity> ctx) {
        try {
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final List<SchemaDefinition.FieldDef> fieldDefList = fieldSchemaBuilder.build(orgId, Action.UPDATE, i18nService.getResolver(Locale.getDefault()));

            return SchemaDefinition.of(fieldDefList);
        } catch (Exception e) {
            LOGGER.error("MCP: Dynamic schema generation failed for update_product: {}", e.getMessage());
            return generated;
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
        return isAuthorized(ctx) && isSourceEnabled(ctx.identity(), Source.PRODUCT) && hasPermission(ctx, SubCategory.Type.PRODUCTS, List.of(Type.EDIT, Type.VIEW));
    }

}
