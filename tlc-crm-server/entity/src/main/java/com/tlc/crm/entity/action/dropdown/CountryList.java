package com.tlc.crm.entity.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.l10n.model.country.Country;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Provides functionality related to managing {@link Country} related dropdown actions.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see DropdownAction
 */
@StaticDropdown(name = "countryList")
public class CountryList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return the {@link OptionContainer}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Collection<Country> countries = applyCriteria(Arrays.asList(Country.values()), dropdownRequest.getCriteria(),
                List.of(Country::getName), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = countries.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);
        final String idKey = FieldKeys.ID.getName();
        final String nameKey = FieldKeys.NAME.getName();

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Country country : applyLimit(countries, start, dropdownLimit.limit())) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(idKey, country.getId());
                jsonObject.put(nameKey, country.getName());

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
