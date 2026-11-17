package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.field.contact.CompanyFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Builds MCP tool field schemas for the company module.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CompanyFieldSchemaBuilder extends AbstractFieldSchemaBuilder {

    private CompanyFieldSchemaBuilder() {
    }

    private static class Instance {
        private static final CompanyFieldSchemaBuilder INSTANCE = new CompanyFieldSchemaBuilder();
    }

    public static CompanyFieldSchemaBuilder getInstance() {
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
    public List<FieldDef> build(final long orgId, final Action action, final I18nResolver i18nResolver) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.COMPANY.getName());
        final List<FieldDef> fieldDefList = new ArrayList<>(fields.size());
        final I18NCRMResolutionAdapter i18nAdapter = new I18NCRMResolutionAdapter(i18nResolver, orgId);

        for (final FieldInfo fieldInfo : fields) {
            if (fieldInfo.isPersistent()) {
                buildPersistentField(orgId, fieldInfo, fieldDefList, i18nAdapter);
            } else {
                final PresetConfig presetConfig = fieldInfo.presetConfig();
                final boolean required = Action.UPDATE == action ? false : presetConfig.required();

                switch (fieldInfo.aliasName()) {
                    case CompanyFields.ID -> {
                        if (Action.UPDATE == action) {
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Unique identifier for the company. Example: 12345")
                                    .required(true)
                                    .type("integer")
                                    .build());
                        }
                    }
                    case CompanyFields.NAME, CompanyFields.WEBSITE, CompanyFields.FACEBOOK, CompanyFields.TWITTER,
                         CompanyFields.LINKEDIN, CompanyFields.DESCRIPTION ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description(switch (fieldInfo.aliasName()) {
                                        case CompanyFields.NAME -> "Company name. Example: Acme Corporation";
                                        case CompanyFields.WEBSITE ->
                                                "Company website URL. Example: https://www.acme.com";
                                        case CompanyFields.FACEBOOK ->
                                                "Company Facebook page URL. Example: https://www.facebook.com/acme";
                                        case CompanyFields.TWITTER -> "Company Twitter handle. Example: '@acme'";
                                        case CompanyFields.LINKEDIN ->
                                                "Company LinkedIn page URL. Example: https://www.linkedin.com/company/acme";
                                        case CompanyFields.DESCRIPTION ->
                                                "Company description. Example: Acme Corporation is a leading provider of innovative solutions in the tech industry.";
                                        default -> "Company " + i18nAdapter.get(fieldInfo.fieldName());
                                    })
                                    .required(required)
                                    .minLength(presetConfig.minLength())
                                    .maxLength(presetConfig.maxLength())
                                    .type("string")
                                    .build());
                    case CompanyFields.SALES_OWNER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Sales owner of the company. Pass user ID. Resolve via search_users with optional query. Paginate with cursor if hasMore.")
                            .required(false)
                            .type("integer")
                            .build());
                    case CompanyFields.PARENT_COMPANY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Parent company. Call get_field_options with fieldName='parent_company' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .type("integer")
                            .required(required)
                            .build());
                    case CompanyFields.TERRITORY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("The sales territory assigned to this company. Call get_field_options with fieldName='territory' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case CompanyFields.TAGS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of tag IDs to categorize or label the company. Call get_field_options with fieldName='tags' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option IDs in the request payload for this field.")
                            .required(required)
                            .type("array")
                            .multiSelect(true)
                            .itemType("integer")
                            .build());
                    case CompanyFields.ANNUAL_REVENUE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Annual revenue of the company.")
                            .required(required)
                            .type("object")
                            .nestedFields(List.of(
                                    FieldDef.builder("currency")
                                            .description("Currency of the monetary value.")
                                            .required(true)
                                            .options(Currency.getCurrencies().stream().map(currency -> i18nResolver.get(currency.getI18nKey())).toList())
                                            .build(),
                                    FieldDef.builder("value")
                                            .required(true)
                                            .description("Monetary amount")
                                            .type("number")
                                            .build()
                            ))
                            .additionalProperties(false)
                            .build());
                    case CompanyFields.ADDRESS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Address of the company.")
                            .required(required)
                            .type("object")
                            .nestedFields(List.of(
                                    FieldDef.builder("address_line1")
                                            .description("Primary street address line. Example: 123 Maple Street")
                                            .type("string").required(false).build(),
                                    FieldDef.builder("address_line2")
                                            .description("Secondary street address line. Example: Suite 405, Downtown")
                                            .type("string").required(false).build(),
                                    FieldDef.builder("city")
                                            .description("City. Example: Austin")
                                            .type("string").required(false).build(),
                                    FieldDef.builder("state")
                                            .description("State or province. Example: Texas")
                                            .type("string").required(false).build(),
                                    FieldDef.builder("country")
                                            .description("Country ID or name. Refer to Countries reference list. Example: 165 or \"United States\"")
                                            .required(false)
                                            .options(Arrays.stream(Country.values()).map(country -> i18nResolver.get(country.getName())).toList())
                                            .build(),
                                    FieldDef.builder("zipcode")
                                            .description("Postal/ZIP code. Stored as string to preserve leading zeros. Example: 78701")
                                            .type("string")
                                            .required(false).build()
                            ))
                            .additionalProperties(false)
                            .build());
                    case CompanyFields.SIC_CODE, CompanyFields.EMPLOYEE_COUNT ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description(switch (fieldInfo.aliasName()) {
                                        case CompanyFields.SIC_CODE ->
                                                "Standard Industrial Classification code for the company. Example: 1234";
                                        case CompanyFields.EMPLOYEE_COUNT ->
                                                "Number of employees in the company. Example: 250";
                                        default -> "Company " + i18nAdapter.get(fieldInfo.fieldName());
                                    })
                                    .required(required)
                                    .min(String.valueOf(presetConfig.min()))
                                    .max(String.valueOf(presetConfig.max()))
                                    .type("integer")
                                    .build());
                    case CompanyFields.ORGANIZATION_STATUS, CompanyFields.INDUSTRY_TYPE,
                         CompanyFields.BUSINESS_TYPE -> {
                        final Collection<Option> options = optionService.get(orgId, fieldInfo);

                        fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                .description(switch (fieldInfo.aliasName()) {
                                    case CompanyFields.ORGANIZATION_STATUS -> "Organization status.";
                                    case CompanyFields.INDUSTRY_TYPE -> "Industry type.";
                                    case CompanyFields.BUSINESS_TYPE -> "Business type.";
                                    default -> "Company " + i18nAdapter.get(fieldInfo.fieldName());
                                })
                                .required(required)
                                .type("string")
                                .options(options.stream().map(option -> i18nAdapter.get(option.getOption())).toList())
                                .build());
                    }
                }
            }
        }
        return fieldDefList;
    }

}
