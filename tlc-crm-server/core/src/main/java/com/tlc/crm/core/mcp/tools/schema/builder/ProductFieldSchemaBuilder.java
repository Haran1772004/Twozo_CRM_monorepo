package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.field.product.ProductFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds MCP tool field schemas for the product module.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ProductFieldSchemaBuilder extends AbstractFieldSchemaBuilder {

    private ProductFieldSchemaBuilder() {
    }

    private static class Instance {
        private static final ProductFieldSchemaBuilder INSTANCE = new ProductFieldSchemaBuilder();
    }

    public static ProductFieldSchemaBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        The organization identifier.
     * @param action       The CRM action for which the schema is being generated.
     * @param i18nResolver The internationalization resolver.
     * @return
     */
    @Override
    public List<FieldDef> build(long orgId, final Action action, final I18nResolver i18nResolver) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.PRODUCT.getName());
        final List<FieldDef> fieldDefList = new ArrayList<>(fields.size());
        final I18NCRMResolutionAdapter i18nAdapter = new I18NCRMResolutionAdapter(i18nResolver, orgId);

        for (final FieldInfo fieldInfo : fields) {
            if (fieldInfo.isPersistent()) {
                buildPersistentField(orgId, fieldInfo, fieldDefList, i18nAdapter);
            } else {
                final PresetConfig presetConfig = fieldInfo.presetConfig();
                final boolean required = Action.UPDATE == action ? false : presetConfig.required();

                switch (fieldInfo.aliasName()) {
                    case ProductFields.ID -> {
                        if (Action.UPDATE == action) {
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Unique identifier for the contact. Example: 12345")
                                    .required(true)
                                    .type("integer")
                                    .build());
                        }
                    }
                    case ProductFields.NAME, ProductFields.DESCRIPTION, ProductFields.SKU_NUMBER,
                         ProductFields.PRODUCT_CODE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description(switch (fieldInfo.aliasName()) {
                                case ProductFields.NAME -> "Product name. Max length: " + presetConfig.maxLength();
                                case ProductFields.DESCRIPTION ->
                                        "Product description. Max length: " + presetConfig.maxLength();
                                case ProductFields.SKU_NUMBER ->
                                        "Product SKU number. Max length: " + presetConfig.maxLength();
                                case ProductFields.PRODUCT_CODE ->
                                        "Product code. Max length: " + presetConfig.maxLength();
                                default -> "Product " + i18nAdapter.get(fieldInfo.fieldName());
                            })
                            .required(required)
                            .minLength(presetConfig.minLength())
                            .maxLength(presetConfig.maxLength())
                            .type("string")
                            .build());
                    case ProductFields.SALES_OWNER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Sales owner of the product. Pass user ID. Resolve via search_users with optional query. Paginate with cursor if hasMore.")
                            .required(false)
                            .type("integer")
                            .build());
                    case ProductFields.QUANTITY_ORDERED, ProductFields.QUANTITY_IN_STOCK ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Product quantity. Must be a non-negative integer.")
                                    .required(required)
                                    .min(String.valueOf(presetConfig.min()))
                                    .max(String.valueOf(presetConfig.max()))
                                    .type("integer")
                                    .build());
                    case ProductFields.CATEGORY, ProductFields.ACTIVE_STATUS, ProductFields.TAXABLE,
                         ProductFields.TYPE -> {
                        final Collection<Option> options = optionService.get(orgId, fieldInfo);

                        fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                .description(switch (fieldInfo.aliasName()) {
                                    case ProductFields.CATEGORY -> "Product category";
                                    case ProductFields.ACTIVE_STATUS -> "Active status";
                                    case ProductFields.TAXABLE -> "Taxable status.";
                                    case ProductFields.TYPE -> "Product type.";
                                    default -> "Product " + i18nAdapter.get(fieldInfo.fieldName());
                                })
                                .required(required)
                                .type("string")
                                .options(options.stream().map(option -> i18nAdapter.get(option.getOption())).toList())
                                .build());
                    }
                    case ProductFields.VALID_TILL -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Valid till date-time in ISO 8601 (e.g. 2026-07-01T12:55:00Z)")
                            .required(required)
                            .type("string")
                            .build());
                    case ProductFields.PRICE_TAGS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Price tags array. Each entry contains currency, unit_price, tax, and discount.")
                            .required(required)
                            .type("array")
                            .itemType("object")
                            .nestedFields(List.of(
                                    FieldDef.builder("currency")
                                            .description("Currency code.")
                                            .required(false)
                                            .options(Currency.getCurrencies().stream().map(currency -> i18nResolver.get(currency.getI18nKey())).toList())
                                            .build(),
                                    FieldDef.builder("unit_price")
                                            .description("Unit price. Example: 22345")
                                            .required(true)
                                            .type("string")
                                            .build(),
                                    FieldDef.builder("tax")
                                            .description("Tax percentage. Example: 12")
                                            .required(false)
                                            .type("integer")
                                            .build(),
                                    FieldDef.builder("discount")
                                            .description("Discount percentage. Example: 21")
                                            .required(false)
                                            .type("integer")
                                            .build()
                            ))
                            .additionalProperties(false)
                            .build());
                }

            }
        }
        return fieldDefList;
    }

}
