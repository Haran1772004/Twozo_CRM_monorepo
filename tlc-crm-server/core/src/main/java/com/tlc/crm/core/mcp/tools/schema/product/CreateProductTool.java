package com.tlc.crm.core.mcp.tools.schema.product;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.formatter.ProductRestReqFormatter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;
import com.tlc.crm.core.mcp.tools.schema.builder.ProductFieldSchemaBuilder;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.Action.Type;

import com.tlc.i18n.I18nResolver;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Tool — Create Product. Field names and dropdown values are org-specific and change over time.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "create_product",
        title = "Create Product",
        description = "Create a new product in the CRM. Pass a single `input` object containing default fields and org-specific custom fields"
)
public class CreateProductTool extends CrmSecureMcpTool<Product> {

    private final ProductManager productManager;
    private final ProductConverter productConverter;
    private final ProductRestReqFormatter productRestReqFormatter;

    public CreateProductTool() {
        this.productManager = ProductManager.getInstance();
        this.productConverter = ProductConverter.getInstance();
        this.productRestReqFormatter = ProductRestReqFormatter.getInstance();
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
        final CrmUser crmUser = ctx.identity().crmUser();
        final long orgId = crmUser.orgId();
        final long userId = crmUser.id();

        try {
            final JsonObject requestJson = buildRequestJson(input);
            final Locale locale = Locale.getDefault();
            final I18nResolver i18nResolver = i18nService.getResolver(locale);
            final Product product = convert(crmUser, requestJson, i18nResolver);
            final Collection<ErrorDescriptor> errorDescriptors = validate(List.of(product), Group.Create.class);

            if (!errorDescriptors.isEmpty()) {
                final List<String> errors = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                return errorResult("Validation failed:\n• " + String.join("\n• ", errors));
            }

            final JsonObject jsonObject = create(orgId, userId, locale, List.of(product));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Create product failed for Org ID: {}, User ID: {}, Reason: {}", orgId, userId, e.getMessage(), e);
            return errorResult("Failed to create product: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser      The authenticated CRM user.
     * @param jsonObject   The input JSON payload.
     * @param i18nResolver The internationalization resolver.
     * @return
     */
    protected Product convert(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        final JsonObject productJson = productRestReqFormatter.format(crmUser.orgId(), jsonObject, i18nResolver);
        final ConverterConfig converterConfig = new ConverterConfig(crmUser.orgId(), crmUser.id(), Locale.getDefault(), false, productJson);

        return productConverter.convert(converterConfig, productJson);
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx the MCP tool context
     * @return
     */
    @Override
    public boolean isVisible(final McpToolContext<CrmMcpIdentity> ctx) {
        return isAuthorized(ctx) && isSourceEnabled(ctx.identity(), Source.PRODUCT) && hasPermission(ctx, SubCategory.Type.PRODUCTS, List.of(Type.CREATE));
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
            final ProductFieldSchemaBuilder fieldSchemaBuilder = ProductFieldSchemaBuilder.getInstance();
            final List<FieldDef> fieldDefList = fieldSchemaBuilder.build(orgId, Action.CREATE, i18nService.getResolver(Locale.getDefault()));

            return SchemaDefinition.of(fieldDefList);
        } catch (Exception e) {
            LOGGER.error("MCP: Dynamic schema generation failed for create_product: {}", e.getMessage());
            return generated;
        }
    }

}
