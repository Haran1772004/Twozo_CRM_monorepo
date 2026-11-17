package com.tlc.crm.entity.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;

/**
 * Manages the dropdown type fields options
 *
 * @author sundar
 * @version 1.0
 */
@StaticDropdown(name = "entityOptions")
public class EntityOptionList extends AbstractDropdownAction {

    private final OptionService optionService;

    public EntityOptionList() {
        optionService = OptionService.getInstance();
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
        final Collection<Option> options = optionService.getOptions(dropdownRequest.getOrgId(), dropdownRequest.getField(), dropdownRequest.getCriteria(),
                dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
        final long totalRecords = options.size();
        final int start = dropdownLimit.start();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<Option> optionList = applyLimit(options, start, dropdownLimit.limit());

            if (CrmRequest.ClientType.REST_API == dropdownRequest.getClientType()) {
                for (final Option option : optionList) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(OptionKeys.ID, option.id());
                    jsonObject.put(OptionKeys.NAME, option.getOption());
                    optionContainer.addOption(jsonObject);
                }
            } else {
                for (final Option option : optionList) {
                    optionContainer.addOption(option.id(), option.getOption(), option.getOrder(), option.isEditable(), option.isDefault());
                }
            }
        }
        return optionContainer;
    }
}
