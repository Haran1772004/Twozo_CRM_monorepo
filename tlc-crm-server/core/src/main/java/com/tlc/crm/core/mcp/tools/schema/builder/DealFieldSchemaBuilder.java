package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.field.deal.DealFields;

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
 * Builds MCP tool field schemas for the deal module.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class DealFieldSchemaBuilder extends AbstractFieldSchemaBuilder {

    private DealFieldSchemaBuilder() {
    }

    private static class Instance {
        private static final DealFieldSchemaBuilder INSTANCE = new DealFieldSchemaBuilder();
    }

    public static DealFieldSchemaBuilder getInstance() {
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
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.DEAL.getName());
        final List<FieldDef> fieldDefList = new ArrayList<>(fields.size());
        final I18NCRMResolutionAdapter i18nAdapter = new I18NCRMResolutionAdapter(i18nResolver, orgId);

        for (final FieldInfo fieldInfo : fields) {
            if (fieldInfo.isPersistent()) {
                buildPersistentField(orgId, fieldInfo, fieldDefList, i18nAdapter);
            } else {
                final PresetConfig presetConfig = fieldInfo.presetConfig();
                final boolean required = Action.UPDATE == action ? false : presetConfig.required();

                switch (fieldInfo.aliasName()) {
                    case DealFields.ID -> {
                        if (Action.UPDATE == action) {
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Unique identifier for the contact. Example: 12345")
                                    .required(true)
                                    .type("integer")
                                    .build());
                        }
                    }
                    case DealFields.TITLE, DealFields.DESCRIPTION ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description(switch (fieldInfo.aliasName()) {
                                        case DealFields.TITLE -> "Title of the deal";
                                        case DealFields.DESCRIPTION -> "Description of the deal";
                                        default -> "Deal " + i18nAdapter.get(fieldInfo.fieldName());
                                    })
                                    .required(required)
                                    .minLength(presetConfig.minLength())
                                    .maxLength(presetConfig.maxLength())
                                    .type("string")
                                    .build());
                    case DealFields.SALES_OWNER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Sales owner of the deal. Pass user ID. Resolve via search_users with optional query. Paginate with cursor if hasMore.")
                            .required(false)
                            .type("integer")
                            .build());
                    case DealFields.TERRITORY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("The sales territory assigned to this deal. Call get_field_options with fieldName='territory' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case DealFields.TAGS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of tag IDs to categorize or label the contact. Call get_field_options with fieldName='tags' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the IDs of the selected option in the request payload for this field.")
                            .required(required)
                            .type("array")
                            .multiSelect(true)
                            .itemType("integer")
                            .build());
                    case DealFields.PIPELINE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("The sales pipeline assigned to this deal. Call get_field_options with fieldName='pipeline' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case DealFields.PIPELINE_STAGE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("""
                                    The sales pipeline stage assigned to this deal. Call get_field_options with fieldName='pipeline_stage' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.
                                    Note: When set to 'Lost/lost', the lost_reason and deal_closes_on fields becomes mandatory.
                                    When set to 'Won/won', the won_reason and deal_closes_on fields becomes mandatory.""")
                            .required(required)
                            .type("integer")
                            .build());
                    case DealFields.PRIMARY_CONTACT -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Primary contact ID associated with the deal. Call get_field_options with fieldName='primary_contact' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case DealFields.COMPANY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Company ID associated with the deal. Call get_field_options with fieldName='company' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case DealFields.WON_REASON, DealFields.LOST_REASON, DealFields.DEAL_SOURCE,
                         DealFields.TYPE, DealFields.PAYMENT_STATUS -> {
                        final Collection<Option> options = optionService.get(orgId, fieldInfo);

                        fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                .description(switch (fieldInfo.aliasName()) {
                                    case DealFields.WON_REASON -> "Reason for winning the deal.";
                                    case DealFields.LOST_REASON -> "Reason for losing the deal.";
                                    case DealFields.DEAL_SOURCE -> "Source of the deal.";
                                    case DealFields.TYPE -> "Type of the deal.";
                                    case DealFields.PAYMENT_STATUS -> "Payment status of the deal";
                                    default -> "Deal " + i18nAdapter.get(fieldInfo.fieldName());
                                })
                                .required(required)
                                .type("string")
                                .options(options.stream().map(option -> i18nAdapter.get(option.getOption())).toList())
                                .build());
                    }
                    case DealFields.DEAL_CLOSED_ON, DealFields.EXPECTED_CLOSE_DATE ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description(switch (fieldInfo.aliasName()) {
                                        case DealFields.DEAL_CLOSED_ON ->
                                                "Date when the deal was closed. Format: ISO 8601 timestamp. Example: 2023-08-15T14:30:00Z";
                                        case DealFields.EXPECTED_CLOSE_DATE ->
                                                "Expected date for the deal to close. Format: ISO 8601 timestamp. Example: 2023-09-01T00:00:00Z";
                                        default -> "Deal " + i18nAdapter.get(fieldInfo.fieldName());
                                    })
                                    .required(required)
                                    .type("string")
                                    .build());
                    case DealFields.RELATED_CONTACTS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of contact IDs related to the deal. Call get_field_options with fieldName='related_contacts' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the IDs of the selected option in the request payload for this field.")
                            .required(required)
                            .type("array")
                            .multiSelect(true)
                            .itemType("integer")
                            .build());
                    case DealFields.DEAL_VALUE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Monetary value of the deal, represented as an object containing currency and amount.")
                            .required(required)
                            .type("object")
                            .nestedFields(List.of(
                                    FieldDef.builder("currency")
                                            .description("Currency of the monetary value.")
                                            .options(Currency.getCurrencies().stream().map(currency -> i18nResolver.get(currency.getI18nKey())).toList())
                                            .build(),
                                    FieldDef.builder("value")
                                            .description("Monetary amount")
                                            .type("number")
                                            .required(true)
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
