package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;

import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages {@link CriteriaField} related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "filterFieldList")
public class CriteriaFieldList extends AbstractDropdownAction {

    private final FilterFieldService filterFieldService;

    public CriteriaFieldList() {
        filterFieldService = FilterFieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested criteria field values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final long sourceId = dropdownRequest.data().getLong(FilterFieldAttributes.SOURCE_ID.getName());
        final Comparator<CriteriaField> comparator = Comparator.comparing(field -> i18NResolutionAdapter.get(field.getName()).toLowerCase());
        final Collection<CriteriaField> criteriaFields = applyCriteria(filterFieldService.getFieldsBySourceId(dropdownRequest.getOrgId(), sourceId, comparator, true)
                        .stream().filter(criteriaField -> !criteriaField.isDefault()).toList(), dropdownRequest.getCriteria(),
                List.of(criteriaField -> i18NResolutionAdapter.get(criteriaField.getName())), Optional.empty());
        final OptionContainer optionContainer = new OptionContainer();

        for (final CriteriaField criteriaField : criteriaFields) {
            fillOptionContainer(optionContainer, criteriaField, i18NResolutionAdapter);
        }

        return optionContainer;
    }

    /**
     * <p>
     * Loads the {@link CriteriaField} values in {@link OptionContainer}.
     * </p>
     *
     * @param optionContainer which holds the option values.
     * @param criteriaField   which holds the values of criteria field.
     */
    private void fillOptionContainer(final OptionContainer optionContainer, final CriteriaField criteriaField,
                                     final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject jsonObject = Json.object();
        final Collection<DropdownDetails> dropdownDetailsList = criteriaField.getDropdownDetails();

        jsonObject.put(FilterFieldAttributes.NAME.getName(), i18NResolutionAdapter.get(criteriaField.getName()));
        jsonObject.put(FilterFieldAttributes.FIELD_TYPE.getName(), convertFieldTypeToJson(criteriaField.getFieldType()));
        jsonObject.put(criteriaField instanceof CriteriaOption ? FilterFieldAttributes.OPTION_ID.getName()
                : FilterFieldAttributes.FIELD_ID.getName(), criteriaField.getId());

        if (Objects.nonNull(dropdownDetailsList)) {
            final JsonArray additionalOptionDropdowns = Json.array();
            final String requiredFieldsKey = FilterFieldAttributes.REQUIRED_FIELDS.getName();

            for (final DropdownDetails dropdownDetails : dropdownDetailsList) {
                final String dropdownName = dropdownDetails.getDropdownName();
                final String[] requiredFields = dropdownDetails.getRequiredFields();

                if (dropdownDetails.isAdditionalOption()) {
                    final JsonObject additionalOptionJson = Json.object();

                    additionalOptionJson.put(FilterFieldAttributes.ADDITIONAL_OPTIONS_DROPDOWN_NAME.getName(), dropdownName);
                    additionalOptionJson.put(FilterFieldAttributes.ORDER.getName(), dropdownDetails.getOrder());
                    additionalOptionJson.put(requiredFieldsKey, requiredFields);
                    additionalOptionDropdowns.put(additionalOptionJson);
                } else {
                    final JsonObject dropdownJson = Json.object();
                    final DropdownType dropdownType = dropdownDetails.getDropdownType();

                    if (Objects.nonNull(requiredFields)) {
                        dropdownJson.put(requiredFieldsKey, requiredFields);
                    }

                    if (Objects.nonNull(dropdownType)) {
                        jsonObject.put(FilterFieldAttributes.DROPDOWN_TYPE.getName(), Objects.equals(DropdownType.GROUPED,
                                dropdownType) ? DropdownType.GROUPED.name().toLowerCase() : FilterFieldAttributes.BASIC.getName());
                    }
                    dropdownJson.put(FilterFieldAttributes.VALUE_DROPDOWN_NAME.getName(), dropdownName);
                    jsonObject.put(FilterFieldAttributes.VALUE_DROPDOWN.getName(), dropdownJson);
                }
            }

            if (additionalOptionDropdowns.size() > 0) {
                jsonObject.put(FilterFieldAttributes.ADDITIONAL_OPTIONS_DROPDOWN.getName(), additionalOptionDropdowns);
            }
        } else if (criteriaField.hasValueDropdown()) {
            final JsonObject dropdownJson = Json.object();

            dropdownJson.put(FilterFieldAttributes.REQUIRED_FIELDS.getName(), Json.array(List.of(FieldKeys.ID.getName())));
            jsonObject.put(FilterFieldAttributes.VALUE_DROPDOWN.getName(), dropdownJson);
            jsonObject.put(FilterFieldAttributes.DROPDOWN_TYPE.getName(), FilterFieldAttributes.BASIC.getName());
        }
        optionContainer.addOption(jsonObject);
    }

    /**
     * <p>
     * Converts the {@link FieldType} into {@link JsonObject}.
     * </p>
     *
     * @param fieldType Holds the values of field type.
     * @return Converted {@link JsonObject}.
     */
    private JsonObject convertFieldTypeToJson(final FieldType fieldType) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.ID.getName(), fieldType.id());
        jsonObject.put(FieldKeys.NAME.getName(), fieldType.name());

        return jsonObject;
    }
}
