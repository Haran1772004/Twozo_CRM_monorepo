package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for building MCP tool field schemas.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
abstract class AbstractFieldSchemaBuilder {

    protected final FieldService fieldService;
    protected final OptionService optionService;

    protected AbstractFieldSchemaBuilder() {
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
    }

    /**
     * Builds the schema definition for a persistent CRM field and adds it to
     * the provided list of field definitions.
     *
     * @param orgId        The organization identifier.
     * @param fieldInfo    Metadata describing the CRM field.
     * @param fieldDefList The collection to which the generated field definition is added.
     * @param i18nResolver The internationalization resolver used to localize field labels and descriptions.
     */
    protected void buildPersistentField(final long orgId, final FieldInfo fieldInfo, final List<FieldDef> fieldDefList, final I18NCRMResolutionAdapter i18nResolver) {
        final StoreType storeType = fieldInfo.storeType();
        final PresetConfig presetConfig = fieldInfo.presetConfig();

        switch (storeType) {
            case SINGLE_STRING, TEXT -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(fieldInfo.fieldName())
                    .required(presetConfig.required())
                    .minLength(presetConfig.minLength())
                    .maxLength(presetConfig.maxLength())
                    .type("string")
                    .build());
            case SINGLE_NUMBER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Numeric value.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("number")
                    .min(String.valueOf(presetConfig.min()))
                    .max(String.valueOf(presetConfig.max()))
                    .build());
            case MONETARY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Monetary value.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("object")
                    .nestedFields(List.of(
                            FieldDef.builder("currency")
                                    .description("Currency of the monetary value.")
                                    .options(Currency.getCurrencies().stream().map(currency -> i18nResolver.get(currency.getI18nKey())).toList())
                                    .build(),
                            FieldDef.builder("value")
                                    .description("Monetary amount. Example 50000.00")
                                    .type("number")
                                    .required(true)
                                    .build()
                    ))
                    .additionalProperties(false)
                    .build());
            case DROPDOWN -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Dropdown field. Call get_field_options with fieldName='<field_name>' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Pass the option ID.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .build());
            case OWNER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Reference field. Pass user ID. Resolve via search_users with optional query. Paginate with cursor if hasMore.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("integer")
                    .build());
            case COMPANY, CONTACT -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Reference field. Pass ID (integer). Call get_field_options with fieldName='<field_name>' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("integer")
                    .build());
            case EMAIL -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description("Email address. Each item: {email_type, email_value}. If invalid type is provided, defaults to Work.")
                    .required(presetConfig.required())
                    .type("object")
                    .nestedFields(List.of(
                            FieldDef.builder("email_type")
                                    .description("Email type. Accepts ID or name. If invalid type is provided, defaults to Work.")
                                    .options(List.of("work", "personal", "home", "other"))
                                    .required(true)
                                    .build(),
                            FieldDef.builder("email_value")
                                    .description("Email address. Example: sample@aus.in")
                                    .type("string")
                                    .required(true)
                                    .build()))
                    .additionalProperties(false)
                    .build());
            case PHONE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description("Phone number. Each item: {phone_type, phone_value}. If invalid type is provided, defaults to Work.")
                    .required(presetConfig.required())
                    .type("object")
                    .nestedFields(List.of(
                            FieldDef.builder("phone_type")
                                    .description("Phone type. Accepts ID or name. If invalid type is provided, defaults to Work.")
                                    .options(List.of("work", "phone", "mobile", "whatsapp", "telegram", "personal", "other"))
                                    .required(true)
                                    .build(),
                            FieldDef.builder("phone_value")
                                    .description("Phone number. Example: 4453-237-832")
                                    .required(true)
                                    .type("string")
                                    .build()))
                    .additionalProperties(false)
                    .build());
            case ADDRESS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description("Mailing or physical address.")
                    .required(presetConfig.required())
                    .type("object")
                    .nestedFields(List.of(
                            FieldDef.builder("address_line1")
                                    .description("Primary street address line. Example: 123 Maple Street")
                                    .type("string").required(false).build(),
                            FieldDef.builder("address_line2")
                                    .description("Secondary street address line. Example: Suite 405, Downtown District")
                                    .type("string").required(false).build(),
                            FieldDef.builder("city")
                                    .description("City. Example: Austin")
                                    .type("string").required(false).build(),
                            FieldDef.builder("state")
                                    .description("State or province. Example: Texas")
                                    .type("string").required(false).build(),
                            FieldDef.builder("country")
                                    .description("Country ID or name. Refer to Countries reference list. Example: 165 or United States")
                                    .required(false)
                                    .options(Arrays.stream(Country.values()).map(country -> i18nResolver.get(country.getName())).toList())
                                    .build(),
                            FieldDef.builder("zipcode")
                                    .description("Postal/ZIP code. Stored as string to preserve leading zeros. Example: 78701")
                                    .type("string").required(false).build()
                    ))
                    .additionalProperties(false)
                    .build());
            case MULTI_SELECT -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Multi-select. Call get_field_options with fieldName='<field_name>' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Pass an array of option IDs.", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("array")
                    .itemType("string")
                    .multiSelect(true)
                    .build());
            case DATE, TIME -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                    .description(String.format("%s — Date/Time. Format: ISO8601. Example: 2026-07-01T12:55:00+05:30", fieldInfo.fieldName()))
                    .required(presetConfig.required())
                    .type("string")
                    .build());
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE ->
                    fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description(String.format("%s — Range. Format: {range: {from: ISO8601, to: ISO8601}}. Example: {range: {from: 2026-07-01T12:55:00+05:30, to: 2026-07-02T12:50:00+05:30}}", fieldInfo.fieldName()))
                            .required(presetConfig.required())
                            .type("object")
                            .nestedFields(List.of(
                                    FieldDef.builder("from")
                                            .description("Start date in ISO 8601 format")
                                            .type("string")
                                            .required(true)
                                            .build(),
                                    FieldDef.builder("to")
                                            .description("End date in ISO 8601 format")
                                            .type("string")
                                            .required(true)
                                            .build()
                            ))
                            .additionalProperties(false)
                            .build());
        }
    }

    /**
     * Builds the MCP tool schema for the specified action.
     *
     * @param orgId        The organization identifier.
     * @param action       The CRM action for which the schema is being generated.
     * @param i18nResolver The internationalization resolver.
     * @return The list of field definitions representing the tool input schema.
     */
    protected abstract List<FieldDef> build(final long orgId, final Action action, final I18nResolver i18nResolver);
}
