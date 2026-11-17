package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.country.Country;

import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Builds MCP tool field schemas for the contact module.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ContactFieldSchemaBuilder extends AbstractFieldSchemaBuilder {

    private ContactFieldSchemaBuilder() {
    }

    private static class Instance {
        private static final ContactFieldSchemaBuilder INSTANCE = new ContactFieldSchemaBuilder();
    }

    public static ContactFieldSchemaBuilder getInstance() {
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
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.CONTACT.getName());
        final List<FieldDef> fieldDefList = new ArrayList<>(fields.size());
        final I18NCRMResolutionAdapter i18nAdapter = new I18NCRMResolutionAdapter(i18nResolver, orgId);

        for (final FieldInfo fieldInfo : fields) {
            if (fieldInfo.isPersistent()) {
                buildPersistentField(orgId, fieldInfo, fieldDefList, i18nAdapter);
            } else {
                final PresetConfig presetConfig = fieldInfo.presetConfig();
                final boolean required = Action.UPDATE == action ? false : presetConfig.required();

                switch (fieldInfo.aliasName()) {
                    case ContactFields.ID -> {
                        if (Action.UPDATE == action) {
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Unique identifier for the contact. Example: 12345")
                                    .required(true)
                                    .type("integer")
                                    .build());
                        }
                    }
                    case ContactFields.FIRST_NAME, ContactFields.LAST_NAME, ContactFields.FACEBOOK,
                         ContactFields.TWITTER, ContactFields.LINKEDIN, ContactFields.DESIGNATION,
                         ContactFields.DEPARTMENT, ContactFields.OTHER_UNSUBSCRIBE_REASON ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description(switch (fieldInfo.aliasName()) {
                                        case ContactFields.FIRST_NAME -> "First name of the contact. Example: John";
                                        case ContactFields.LAST_NAME -> "Last name of the contact. Example: Doe";
                                        case ContactFields.FACEBOOK ->
                                                "Facebook profile URL of the contact. Example: https://www.facebook.com/johndoe";
                                        case ContactFields.TWITTER ->
                                                "Twitter handle of the contact. Example: @johndoe";
                                        case ContactFields.LINKEDIN ->
                                                "LinkedIn profile URL of the contact. Example: https://www.linkedin.com/in/johndoe";
                                        case ContactFields.DESIGNATION ->
                                                "Job title or designation of the contact. Example: Software Engineer";
                                        case ContactFields.DEPARTMENT ->
                                                "Department of the contact within the organization. Example: Engineering";
                                        case ContactFields.OTHER_UNSUBSCRIBE_REASON ->
                                                "Reason for unsubscribing from communications. Example: No longer interested";
                                        default -> "Contact " + i18nAdapter.get(fieldInfo.fieldName());
                                    })
                                    .required(required)
                                    .minLength(presetConfig.minLength())
                                    .maxLength(presetConfig.maxLength())
                                    .type("string")
                                    .build());

                    case ContactFields.SALES_OWNER -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Sales owner of the contact. Pass user ID. Resolve via search_users with optional query. Paginate with cursor if hasMore.")
                            .required(false)
                            .type("integer")
                            .build());
                    case ContactFields.TERRITORY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("The sales territory assigned to this contact. Call get_field_options with fieldName='territory' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case ContactFields.TAGS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of tag IDs to categorize or label the contact. Call get_field_options with fieldName='tags' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the IDs of the selected option in the request payload for this field.")
                            .required(required)
                            .type("array")
                            .multiSelect(true)
                            .itemType("integer")
                            .build());
                    case ContactFields.LIFECYCLE_STAGE -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Lifecycle stage ID to categorize or label the contact. Call get_field_options with fieldName='lifecycle_stage' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field. Note: When set to 'Lost', the lost_reason field becomes mandatory.")
                            .required(required)
                            .type("integer")
                            .build());
                    case ContactFields.LIFECYCLE_STATUS ->
                            fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                    .description("Lifecycle status ID to categorize or label the contact. Call get_field_options with fieldName='lifecycle_status' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                                    .required(required)
                                    .type("integer")
                                    .build());
                    case ContactFields.COMPANY -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Company ID associated with the contact. Call get_field_options with fieldName='company' and query='<desired_name>' to search for the required option. Paginate with cursor if hasMore. Use the option ID in the request payload for this field.")
                            .required(required)
                            .type("integer")
                            .build());
                    case ContactFields.EMAILS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of email addresses. Each item: {email_type, email_value}. If invalid type is provided, defaults to Work.")
                            .required(required)
                            .type("array")
                            .itemType("object")
                            .options(List.of("work", "personal", "home", "other"))
                            .build());
                    case ContactFields.PHONES -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("List of phone numbers. Each item: {phone_type, phone_number}. If invalid type is provided, defaults to Work.")
                            .required(required)
                            .type("array")
                            .itemType("object")
                            .options(List.of("work", "phone", "mobile", "whatsapp", "telegram", "personal", "other"))
                            .build());
                    case ContactFields.CONTACT_SOURCE, ContactFields.CONTACT_LOST_REASON,
                         ContactFields.TIMEZONE, ContactFields.SUBSCRIPTION_STATUS,
                         ContactFields.UNSUBSCRIBE_REASON -> {
                        final Collection<Option> options = optionService.get(orgId, fieldInfo);

                        fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                .description(switch (fieldInfo.aliasName()) {
                                    case ContactFields.CONTACT_SOURCE -> "Source of the contact";
                                    case ContactFields.CONTACT_LOST_REASON -> "Reason for losing the contact";
                                    case ContactFields.TIMEZONE -> "Timezone of the contact";
                                    case ContactFields.SUBSCRIPTION_STATUS -> "Subscription status of the contact";
                                    case ContactFields.UNSUBSCRIBE_REASON ->
                                            "Reason for unsubscribing from communications.";
                                    default -> "Contact " + i18nAdapter.get(fieldInfo.fieldName());
                                })
                                .required(required)
                                .type("string")
                                .options(options.stream().map(option -> i18nAdapter.get(option.getOption())).toList())
                                .build());
                    }
                    case ContactFields.ADDRESS -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Address of the contact.")
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
                                            .description("Country ID or name. Example: 165 or United States")
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
                    case ContactFields.DATE_OF_BIRTH -> fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                            .description("Date of birth in ISO 8601 format. Example: 2026-07-01T12:55:00Z")
                            .required(required)
                            .type("string")
                            .build());
                    case ContactFields.SUBSCRIPTION_TYPES -> {
                        final Collection<Option> options = optionService.get(orgId, fieldInfo);

                        fieldDefList.add(FieldDef.builder(fieldInfo.getInternalName())
                                .description("Subscription types the contact is enrolled in")
                                .required(required)
                                .type("array")
                                .multiSelect(true)
                                .itemType("string")
                                .options(options.stream().map(option -> i18nAdapter.get(option.getOption())).toList())
                                .build());
                    }
                }
            }
        }
        return fieldDefList;
    }
}
