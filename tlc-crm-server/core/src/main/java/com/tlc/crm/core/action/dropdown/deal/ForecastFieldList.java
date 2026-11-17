package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the forecast field list dropdown actions
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "forecastFieldList")
public class ForecastFieldList extends AbstractDropdownAction {

    private final FieldService fieldService;

    public ForecastFieldList() {
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final I18nResolutionAdapter i18nResolver = dropdownRequest.getI18NResolutionAdapter();
        final int start = dropdownLimit.start();
        final String idKey = DealFields.ID;
        final String nameKey = DealFields.NAME;
        final String expectedCloseDateKey = DealFields.EXPECTED_CLOSE_DATE;
        final Map<String, FieldInfo> dealFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE,
                List.of(DealFields.DEAL_CREATED, expectedCloseDateKey));
        final Collection<FieldInfo> persistentFields = fieldService.getPersistentFieldsBySource(dropdownRequest.getOrgId(), Deal.ENTITY_TYPE);
        final Collection<FieldInfo> fields = new ArrayList<>(dealFields.values());

        if (Objects.nonNull(persistentFields) && !persistentFields.isEmpty()) {
            fields.addAll(persistentFields.stream().filter(fieldInfo -> {
                final StoreType storeType = fieldInfo.storeType();

                return StoreType.DATE == storeType || StoreType.DATE_RANGE == storeType;
            }).toList());
        }
        final Collection<FieldInfo> forecastFields = applyCriteria(fields.stream().sorted(Comparator.comparing(fieldInfo ->
                fieldInfo.presetConfig().order())).toList(), dropdownRequest.getCriteria(), List.of(fieldInfo ->
                i18nResolver.get(fieldInfo.i18nName())), Optional.empty());
        final long totalRecords = forecastFields.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<FieldInfo> forecastFieldList = applyLimit(forecastFields, start, dropdownLimit.limit());

            for (final FieldInfo fieldInfo : forecastFieldList) {
                final JsonObject jsonObject = Json.object();

                if (Objects.equals(expectedCloseDateKey, fieldInfo.aliasName())) {
                    jsonObject.put(DealFields.IS_DEFAULT, true);
                }
                jsonObject.put(idKey, fieldInfo.id());
                jsonObject.put(nameKey, i18nResolver.get(fieldInfo.i18nName()));
                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
