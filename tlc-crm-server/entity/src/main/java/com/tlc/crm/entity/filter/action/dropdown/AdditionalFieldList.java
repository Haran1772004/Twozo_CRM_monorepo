package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.FieldTypeKeys;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages the additional option related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "additionalFields")
public class AdditionalFieldList extends AbstractDropdownAction {

    private static final String DROPDOWN = "DROPDOWN";

    private final FieldTypeService fieldTypeService;

    public AdditionalFieldList() {
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested additional option values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long fieldTypeId = dropdownRequest.data().getLong(FilterFieldAttributes.FIELD_TYPE_ID.getName());
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Collection<FieldSubType> fieldSubTypes = applyCriteria(fieldTypeService.getFilterableFieldSubType(fieldTypeId),
                dropdownRequest.getCriteria(), List.of(FieldSubType::name), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = fieldSubTypes.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final FieldSubType fieldSubType : applyLimit(fieldSubTypes, start, dropdownLimit.limit())) {
                final String config = fieldSubType.config();
                final JsonObject configJson = Objects.nonNull(config) ? Json.object(config) : Json.object();
                final JsonObject jsonObject = Json.object();
                final String inputType = fieldSubType.inputType();
                final boolean hasDropdown = Objects.equals(DROPDOWN, inputType);
                final String dropdownKey = FieldTypeKeys.DROPDOWN.getName();

                jsonObject.put(FieldTypeKeys.ID.getName(), fieldSubType.id());
                jsonObject.put(FieldTypeKeys.DISPLAY_NAME.getName(), dropdownRequest.i18nResolver().get(fieldSubType.displayName()));
                jsonObject.put(FieldTypeKeys.NAME.getName(), fieldSubType.name());
                jsonObject.put(FieldTypeKeys.HAS_DROPDOWN.getName(), hasDropdown);
                jsonObject.put(FieldTypeKeys.FIELD_TYPE.getName(), convertFieldTypeToJson(fieldTypeService.getFieldTypeByStoreType(StoreType.valueOf(inputType))));

                if (hasDropdown && configJson.containsKey(dropdownKey)) {
                    jsonObject.put(FieldTypeKeys.VALUE_DROPDOWN.getName(), Json.object().put(FieldTypeKeys.VALUE_DROPDOWN_NAME.getName(), configJson.get(dropdownKey)));
                }
                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }

    /**
     * <p>
     * Converts the {@link FieldType} into {@link JsonObject}.
     * </p>
     *
     * @return the converted {@link JsonObject}.
     */
    private JsonObject convertFieldTypeToJson(final FieldType fieldType) {
        if (Objects.isNull(fieldType)) {
            return null;
        }
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.ID.getName(), fieldType.id());
        jsonObject.put(FieldKeys.NAME.getName(), fieldType.storeType().name());

        return jsonObject;
    }
}
