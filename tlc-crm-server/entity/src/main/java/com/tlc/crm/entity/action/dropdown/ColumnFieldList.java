package com.tlc.crm.entity.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.FieldConfig;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityStructureHandler;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Manages column field-related dropdown actions for a given source.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "columnFieldList")
public class ColumnFieldList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested field values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final Source source = Source.get(dropdownRequest.data().getString(FieldKeys.SOURCE.getName()));
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final I18nResolutionAdapter i18nResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final List<FieldConfig> fieldConfigs = EntityStructureHandler.get().fetchConfiguredFields(dropdownRequest.getUserId(), orgId, Provider.getBySource(source).name())
                .stream().map(ConfigHolder::fieldConfig).toList();
        final Collection<FieldConfig> fields = applyCriteria(fieldConfigs, dropdownRequest.getCriteria(),
                List.of(fieldConfig -> i18nResolutionAdapter.get(fieldConfig.getDisplayName())), Optional.empty())
                .stream().sorted(Comparator.comparing(FieldConfig::getOrder)).toList();
        final int start = dropdownLimit.start();
        final long totalRecords = fields.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);
        final String idKey = FieldKeys.ID.getName();
        final String valueKey = FieldKeys.VALUE.getName();
        final String nameKey = FieldKeys.NAME.getName();

        if (totalRecords > 0 && start <= totalRecords) {
            applyLimit(fields, start, dropdownLimit.limit()).forEach(fieldConfig -> {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(idKey, fieldConfig.getFieldId());
                jsonObject.put(nameKey, i18nResolutionAdapter.get(fieldConfig.getDisplayName()));
                jsonObject.put(valueKey, fieldConfig.getName());

                optionContainer.addOption(jsonObject);
            });
        }

        return optionContainer;
    }
}