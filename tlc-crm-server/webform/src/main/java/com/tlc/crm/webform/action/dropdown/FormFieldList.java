package com.tlc.crm.webform.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.field.WebFormFields;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages {@link FormField} related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "formFields")
public class FormFieldList extends AbstractDropdownAction {

    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;

    public FormFieldList() {
        fieldService = FieldService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the form fields.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final int start = dropdownLimit.start();
        final Collection<FieldInfo> fields = applyCriteria(getFormFields(dropdownRequest.getOrgId()),
                dropdownRequest.getCriteria(), List.of(fieldInfo -> getFieldName(fieldInfo, i18NResolutionAdapter)), Optional.empty());
        final long totalRecords = fields.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final String emailsKey = WebFormFields.EMAILS.getName();
            final String phonesKey = WebFormFields.PHONES.getName();

            for (final FieldInfo fieldInfo : applyLimit(fields, start, dropdownLimit.limit())) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(WebFormFields.FIELD_ID.getName(), fieldInfo.id());
                jsonObject.put(WebFormFields.NAME.getName(), getFieldName(fieldInfo, i18NResolutionAdapter));
                jsonObject.put(WebFormFields.FIELD_TYPE.getName(), getFieldTypeJson(fieldInfo.storeType(), i18NResolutionAdapter));

                Optional.ofNullable(fieldInfo.aliasName()).ifPresent(aliasName -> {
                    if (Objects.equals(aliasName, emailsKey) || Objects.equals(aliasName, phonesKey)) {
                        jsonObject.put(WebFormFields.IS_SYSTEM_FIELD.getName(), true);
                    }
                });
                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }

    /**
     * Fetches the fields list for form field dropdown.
     *
     * @param orgId Refers the organization Id.
     * @return The {@link Collection} of {@link FieldInfo}.
     */
    private Collection<FieldInfo> getFormFields(final Long orgId) {
        final String source = Source.CONTACT.getName();
        final Collection<FieldInfo> fields = new ArrayList<>();
        final Map<String, Collection<FieldInfo>> orgFields = fieldService.getOrgFieldsBySource(orgId);

        if (!orgFields.isEmpty()) {
            final Collection<FieldInfo> contactOrgFields = orgFields.get(source);

            if (Objects.nonNull(contactOrgFields) && !contactOrgFields.isEmpty()) {
                fields.addAll(contactOrgFields);
            }
        }
        final Map<String, FieldInfo> defaultFields = fieldService.groupDefaultFieldsByAliasName(source,
                List.of(WebFormFields.FIRST_NAME.getName(), WebFormFields.LAST_NAME.getName(), WebFormFields.EMAILS.getName(),
                        WebFormFields.PHONES.getName(), WebFormFields.COMPANY.getName(), WebFormFields.ADDRESS.getName()));

        fields.addAll(defaultFields.values());

        return fields.stream()
                .filter(fieldInfo -> {
                    final StoreType storeType = fieldInfo.storeType();

                    return (!fieldInfo.isPersistent() || (fieldInfo.isActive() && isFormFieldStore(storeType)))
                            && fieldInfo.hasPresetConfig();
                })
                .sorted(Comparator.comparing(fieldInfo -> fieldInfo.presetConfig().order()))
                .toList();
    }

    /**
     * Checks if the field store type is form field store or not.
     *
     * @param storeType Refers the {@link StoreType}.
     * @return true, if the storeType is {@link WebForm}
     */
    private boolean isFormFieldStore(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_STRING, SINGLE_NUMBER, TEXT, DATE, ADDRESS, EMAIL, PHONE, MONETARY, DROPDOWN, MULTI_SELECT -> true;
            default -> false;
        };
    }

    /**
     * Gets the field name from the {@link FieldInfo}.
     *
     * @param fieldInfo             Refers the {@link FieldInfo} details of a field.
     * @param i18nResolutionAdapter Refers the {@link I18nResolutionAdapter} to resolve the i18nKey.
     * @return The field name.
     */
    private String getFieldName(final FieldInfo fieldInfo, final I18nResolutionAdapter i18nResolutionAdapter) {
        final String fieldName = i18nResolutionAdapter.get(fieldInfo.i18nName());
        final String aliasName = fieldInfo.aliasName();

        return switch (Objects.isNull(aliasName) ? fieldName : aliasName) {
            case "emails" -> i18nResolutionAdapter.get(new I18nKey("i18n.entity.field.type.email"));
            case "phones" -> i18nResolutionAdapter.get(new I18nKey("i18n.entity.field.type.phone"));
            default -> fieldName;
        };
    }

    /**
     * Gets the {@link JsonObject} of field type.
     *
     * @param storeType             Refers the {@link StoreType} of the field.
     * @param i18nResolutionAdapter Refers the {@link I18nResolutionAdapter} to resolve the i18nKey.
     * @return The {@link JsonObject} which holds the field type details.
     */
    private JsonObject getFieldTypeJson(final StoreType storeType, final I18nResolutionAdapter i18nResolutionAdapter) {
        final JsonObject fieldTypeJson = Json.object();
        final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(
                switch (storeType) {
                    case COMPANY_ONBOARD -> StoreType.SINGLE_STRING;
                    case EMAILS -> StoreType.EMAIL;
                    case PHONES -> StoreType.PHONE;
                    default -> storeType;
                }
        );

        fieldTypeJson.put(WebFormFields.NAME.getName(), fieldType.name());
        fieldTypeJson.put(WebFormFields.DISPLAY_NAME.getName(), i18nResolutionAdapter.get(fieldType.displayName()));

        return fieldTypeJson;
    }
}
