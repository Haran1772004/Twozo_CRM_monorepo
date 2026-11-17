package com.tlc.crm.zapier.internal.service;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.zapier.api.models.DropdownInfo;
import com.tlc.crm.zapier.api.models.FieldGroup;
import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapActionField;
import com.tlc.crm.zapier.api.models.ZapDropdownKeys;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class responsible for handling field-related operations
 * for Zapier actions. This includes retrieving, filtering, and managing
 * dynamic input fields used in Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapierActionFieldService {

    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final ZapierFieldService zapierFieldService;
    private final OptionService optionService;

    private ZapierActionFieldService() {
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.zapierFieldService = ZapierFieldService.getInstance();
        this.optionService = OptionService.getInstance();
    }

    private static class Instance {
        private static final ZapierActionFieldService ZAPIER_ACTION_FIELD_SERVICE = new ZapierActionFieldService();
    }

    /**
     * Gets the {@link ZapierActionFieldService} instance.
     *
     * @return {@link ZapierActionFieldService}.
     */
    public static ZapierActionFieldService getInstance() {
        return Instance.ZAPIER_ACTION_FIELD_SERVICE;
    }

    /**
     * Retrieves the collection of {@link ZapActionField} associated with the given
     * organization and Zapier action.
     *
     * @param orgId        The ID of the organization.
     * @param i18nResolver Refers the {@code I18nResolver}.
     * @param zapAction    The Zapier action for which fields are to be fetched.
     * @return The collection of fields relevant to the specified Zapier action and organization.
     */
    public Collection<ZapActionField> getFields(final long orgId, final I18nResolver i18nResolver, final ZapAction zapAction) {
        return switch (zapAction) {
            case ADD_NOTE_TO_COMPANY, ADD_NOTE_TO_CONTACT, ADD_NOTE_TO_DEAL,
                 ADD_FOLLOWER_TO_COMPANY, ADD_FOLLOWER_TO_CONTACT, ADD_FOLLOWER_TO_DEAL,
                 ADD_TAGS_TO_COMPANY, ADD_TAGS_TO_CONTACT, ADD_TAGS_TO_DEAL, ADD_PRODUCT_TO_DEAL -> {
                final Collection<ZapierField> fields = zapierFieldService.getFieldsByActionId(zapAction.getId());

                yield buildZapierFields(fields);
            }
            default -> {
                final Optional<Source> optionalSource = ZapAction.getSource(zapAction);

                if (optionalSource.isPresent()) {
                    final boolean isCreateAction = zapAction.isCreateAction();
                    final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, optionalSource.get().getName())
                            .stream().filter(field -> field.isActive() && (!field.presetConfig().readOnly()
                                    || Objects.equals(Optional.ofNullable(field.aliasName()).orElseGet(() -> ""), ZapAttributes.ID))).collect(Collectors.toUnmodifiableSet());

                    yield buildSourceFields(orgId, fields, isCreateAction, i18nResolver);
                }
                yield Collections.emptyList();
            }
        };
    }

    /**
     * Converts a collection of {@link ZapierField} objects into a collection of
     * {@link ZapActionField} objects for use in Zapier actions.
     *
     * @param fields The input collection of {@link ZapierField} objects.
     * @return The collection of {@link ZapActionField} built from the input fields.
     */
    private Collection<ZapActionField> buildZapierFields(final Collection<ZapierField> fields) {
        final Collection<ZapActionField> zapActionFields = new ArrayList<>();

        for (final ZapierField zapierField : fields) {
            final ZapActionField zapActionField = new ZapActionField();

            zapActionField.setDropdownInfo(getDropdownInfo(zapierField));
            zapActionField.setName(zapierField.getName());
            zapActionField.setFieldId(zapierField.getId());
            zapActionField.setAliasName(zapierField.getAliasName());
            zapActionField.setFieldGroup(FieldGroup.ZAP_RELATED_FIELD);
            zapActionField.setFieldType(zapierField.getFieldType());
            zapActionField.setConfig(zapierField.getConfig());

            zapActionFields.add(zapActionField);
        }

        return zapActionFields;
    }

    /**
     * Builds a collection of {@link ZapActionField} objects from the given {@link FieldInfo}
     * based on organization context and action type.
     *
     * @param orgId          The ID of the organization.
     * @param fields         The collection of {@link FieldInfo} objects to process.
     * @param isCreateAction Flag indicating if the action is a create-type operation.
     * @param i18nResolver   Refers the {@code I18nResolver}.
     * @return The collection of {@link ZapActionField} objects ready for Zapier use
     */
    private Collection<ZapActionField> buildSourceFields(final long orgId, final Collection<FieldInfo> fields,
                                                         final boolean isCreateAction, final I18nResolver i18nResolver) {
        final Collection<ZapActionField> zapActionFields = new ArrayList<>();
        final Collection<FieldInfo> fieldList = isCreateAction ? fields.stream()
                .filter(field -> !Objects.equals(Optional.ofNullable(field.aliasName()).orElseGet(() -> ""), ZapAttributes.ID)).toList() : fields;

        for (final FieldInfo fieldInfo : fieldList) {
            final StoreType storeType = fieldInfo.storeType();

            if (storeType.isRangeType()) {
                setRangeFields(fieldInfo, zapActionFields, i18nResolver);
            } else {
                final ZapActionField zapActionField = new ZapActionField();
                final boolean persistent = fieldInfo.isPersistent();
                final JsonObject presetConfigJson = fieldInfo.presetConfig().presetConfigAsJson();
                final String aliasName = fieldInfo.aliasName();

                zapActionField.setDropdownInfo(getDropdownInfo(orgId, fieldInfo));
                zapActionField.setName(persistent ? fieldInfo.i18nName() : getI18nName(fieldInfo));
                zapActionField.setFieldId(fieldInfo.id());
                zapActionField.setAliasName(persistent ? fieldInfo.fieldName() : aliasName);
                zapActionField.setFieldGroup(FieldGroup.STANDARD_FIELD);
                zapActionField.setFieldType(getFieldType(fieldInfo, persistent));
                zapActionField.setConfig(getConfigJson(presetConfigJson, aliasName, isCreateAction));

                zapActionFields.add(zapActionField);
            }
        }

        return zapActionFields;
    }

    /**
     * Populates the collection of Zap action fields with range-specific fields based on the provided field information.
     *
     * @param fieldInfo       The metadata about the field, including type and configuration.
     * @param zapActionFields The collection to which the generated range fields will be added.
     * @param i18nResolver    Utility to resolve internationalized field names or labels.
     */
    private void setRangeFields(final FieldInfo fieldInfo, final Collection<ZapActionField> zapActionFields, final I18nResolver i18nResolver) {
        final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

        for (final Map.Entry<StoreSubType, FieldSubType> subtype : fieldSubTypes.entrySet()) {
            final FieldSubType fieldSubType = subtype.getValue();
            final ZapActionField zapActionField = new ZapActionField();
            final JsonObject presetConfigJson = fieldInfo.presetConfig().presetConfigAsJson();

            zapActionField.setName(fieldInfo.i18nName());
            zapActionField.setFieldId(fieldInfo.id());
            zapActionField.setAliasName(String.join(" - ", fieldInfo.fieldName(), fieldSubType.name()));
            zapActionField.setFieldGroup(FieldGroup.STANDARD_FIELD);
            zapActionField.setFieldType(getFieldType(fieldInfo, true));
            zapActionField.setConfig(presetConfigJson);

            zapActionFields.add(zapActionField);
        }
    }

    /**
     * Retrieves the configuration JSON based on the provided preset configuration and alias name.
     *
     * @param presetConfigJson The preset configuration JSON object.
     * @param aliasName        The alias name used to locate or modify specific configurations.
     * @param isCreateAction   Flag indicating if the action is a create-type operation.
     * @return A JsonObject containing the configuration for the alias name.
     */
    private JsonObject getConfigJson(final JsonObject presetConfigJson, final String aliasName, final boolean isCreateAction) {
        if (Objects.equals(aliasName, ZapAttributes.ID)) {
            return presetConfigJson.put(ZapAttributes.REQUIRED, true);
        }

        if (!isCreateAction) {
            return presetConfigJson.put(ZapAttributes.REQUIRED, false);
        }
        return presetConfigJson;
    }

    /**
     * Retrieves the type of the field based on the provided FieldInfo and persistence flag.
     *
     * @param fieldInfo    The FieldInfo object that contains details about the field.
     * @param isPersistent A boolean flag indicating whether the field is persistent.
     * @return The {@code FieldType} of the field.
     */
    private FieldType getFieldType(final FieldInfo fieldInfo, final boolean isPersistent) {
        final StoreType storeType = fieldInfo.storeType();

        return fieldTypeService.getFieldTypeByStoreType(isPersistent ? storeType
                : Objects.equals(fieldInfo.aliasName(), ZapAttributes.ID) ? StoreType.SINGLE_STRING : storeType);
    }

    /**
     * Retrieves the {@code I18nKey} for a given field.
     *
     * @param fieldInfo The FieldInfo object that contains details about the field.
     * @return The I18nKey corresponding to the field.
     */
    private I18nKey getI18nName(final FieldInfo fieldInfo) {
        final I18nKey i18nName = fieldInfo.i18nName();

        return switch (fieldInfo.aliasName()) {
            case ZapAttributes.ID -> switch (Source.get(fieldInfo.fieldSource().getName())) {
                case DEAL -> new I18nKey("i18n.zapier.field.deal.placeholder");
                case CONTACT -> new I18nKey("i18n.zapier.field.contact.placeholder");
                case COMPANY -> new I18nKey("i18n.zapier.field.company.placeholder");
                case ACTIVITY -> new I18nKey("i18n.zapier.field.activity");
                case PRODUCT -> new I18nKey("i18n.zapier.field.product.placeholder");
                default -> i18nName;
            };
            default -> i18nName;
        };
    }

    /**
     * Builds and returns a {@link DropdownInfo} object for the given field within
     * the context of the specified organization.
     *
     * @param zapierField The field for which dropdown info is to be generated.
     * @return The {@link DropdownInfo} representing the dropdown configuration for the field.
     */
    private DropdownInfo getDropdownInfo(final ZapierField zapierField) {
        final DropdownInfo dropdownInfo = new DropdownInfo();
        final Collection<Option> choices = new ArrayList<>();
        final StoreType storeType = zapierField.getFieldType().storeType();

        switch (zapierField.getAliasName()) {
            case "contact" -> dropdownInfo.setName(ZapDropdownKeys.CONTACT_LIST);
            case "company" -> dropdownInfo.setName(ZapDropdownKeys.COMPANY_LIST);
            case "deal" -> dropdownInfo.setName(ZapDropdownKeys.DEAL_LIST);
            case "product" -> dropdownInfo.setName(ZapDropdownKeys.PRODUCT_LIST);
            case "follower" -> dropdownInfo.setName(ZapDropdownKeys.USER_LIST);
            case "tag" -> dropdownInfo.setName(ZapDropdownKeys.TAG_LIST);
        }
        dropdownInfo.setChoices(choices);
        dropdownInfo.setMultiSelect(storeType.isMultiSelectType() || storeType.isArrayType());

        return dropdownInfo;
    }

    /**
     * Builds and returns a {@link DropdownInfo} object for the given field within
     * the context of the specified organization. Typically used to prepare dropdown metadata
     * for fields that support selection-based input in Zapier actions or UI components.
     *
     * @param orgId     The ID of the organization.
     * @param fieldInfo The field for which dropdown info is to be generated.
     * @return The {@link DropdownInfo} representing the dropdown configuration for the field.
     */
    private DropdownInfo getDropdownInfo(final long orgId, final FieldInfo fieldInfo) {
        final StoreType storeType = fieldInfo.storeType();
        final boolean isPersistent = fieldInfo.isPersistent();
        final DropdownInfo dropdownInfo = new DropdownInfo();
        final Collection<Option> choices = new ArrayList<>();

        if (isPersistent && StoreType.isSelectorType(storeType)) {
            choices.addAll(optionService.get(orgId, fieldInfo));
        } else if (!isPersistent) {
            switch (fieldInfo.aliasName()) {
                case "id" -> {
                    switch (Source.get(fieldInfo.fieldSource().getName())) {
                        case DEAL -> dropdownInfo.setName(ZapDropdownKeys.DEAL_LIST);
                        case CONTACT -> dropdownInfo.setName(ZapDropdownKeys.CONTACT_LIST);
                        case COMPANY -> dropdownInfo.setName(ZapDropdownKeys.COMPANY_LIST);
                        case ACTIVITY -> dropdownInfo.setName(ZapDropdownKeys.ACTIVITY_LIST);
                        case PRODUCT -> dropdownInfo.setName(ZapDropdownKeys.PRODUCT_LIST);
                    }
                }
                case "contactIds", "primaryContact", "relatedContacts" ->
                        dropdownInfo.setName(ZapDropdownKeys.CONTACT_LIST);
                case "company", "companyId", "parentCompany" -> dropdownInfo.setName(ZapDropdownKeys.COMPANY_LIST);
                case "salesOwner", "collaborators", "assigneeId" -> dropdownInfo.setName(ZapDropdownKeys.USER_LIST);
                case "lifeCycleStage" -> dropdownInfo.setName(ZapDropdownKeys.LIFECYCLE_STAGE_LIST);
                case "lifeCycleStatus" -> dropdownInfo.setName(ZapDropdownKeys.LIFECYCLE_STATUS_LIST);
                case "contactLostReason" -> dropdownInfo.setName(ZapDropdownKeys.CONTACT_LOST_REASON_LIST);
                case "contactSource" -> dropdownInfo.setName(ZapDropdownKeys.CONTACT_SOURCE_LIST);
                case "territory" -> dropdownInfo.setName(ZapDropdownKeys.TERRITORY_LIST);
                case "tags" -> dropdownInfo.setName(ZapDropdownKeys.TAG_LIST);
                case "dealId" -> dropdownInfo.setName(ZapDropdownKeys.DEAL_LIST);
                case "typeId" -> dropdownInfo.setName(ZapDropdownKeys.ACTIVITY_TYPE_LIST);
                case "organizationStatus" -> dropdownInfo.setName(ZapDropdownKeys.ORGANIZATION_STATUS_LIST);
                case "industryType" -> dropdownInfo.setName(ZapDropdownKeys.INDUSTRY_TYPE_LIST);
                case "businessType" -> dropdownInfo.setName(ZapDropdownKeys.BUSINESS_TYPE_LIST);
                case "pipeline" -> dropdownInfo.setName(ZapDropdownKeys.PIPELINE_LIST);
                case "pipelineStage" -> dropdownInfo.setName(ZapDropdownKeys.PIPELINE_STAGE_LIST);
                case "wonReason" -> dropdownInfo.setName(ZapDropdownKeys.WON_REASON_LIST);
                case "lostReason" -> dropdownInfo.setName(ZapDropdownKeys.LOST_REASON_LIST);
                case "dealSource" -> dropdownInfo.setName(ZapDropdownKeys.DEAL_SOURCE_LIST);
                case "category" -> dropdownInfo.setName(ZapDropdownKeys.CATEGORY_LIST);
                case "timeZone" -> dropdownInfo.setName(ZapDropdownKeys.TIMEZONE_LIST);
                case "type" -> {
                    if (Source.DEAL == Source.get(fieldInfo.fieldSource().getName())) {
                        dropdownInfo.setName(ZapDropdownKeys.DEAL_TYPE_LIST);
                    } else {
                        choices.addAll(optionService.get(orgId, fieldInfo));
                    }
                }
                case "paymentStatus" -> dropdownInfo.setName(ZapDropdownKeys.PAYMENT_STATUS_LIST);
                case "reminder" -> {
                    for (final Reminder reminder : Reminder.values()) {
                        final Option option = new Option();

                        option.setId((long) reminder.getId());
                        option.setOption(reminder.getDisplayName());

                        choices.add(option);
                    }
                }
                case "availability" -> {
                    for (final Availability availability : Availability.values()) {
                        final Option option = new Option();

                        option.setId((long) availability.getId());
                        option.setOption(availability.getDisplayName());

                        choices.add(option);
                    }
                }
                case "priority" -> {
                    for (final Priority priority : Priority.values()) {
                        final Option option = new Option();

                        option.setId((long) priority.getId());
                        option.setOption(priority.getDisplayName());

                        choices.add(option);
                    }
                }
                case "subscriptionStatus", "subscriptionTypes", "unsubscribeReason", "activeStatus", "taxable" -> {
                    choices.addAll(optionService.get(orgId, fieldInfo));
                }
            }
        }
        dropdownInfo.setChoices(choices);
        dropdownInfo.setMultiSelect(storeType.isMultiSelectType() || storeType.isArrayType());

        return dropdownInfo;
    }

}
