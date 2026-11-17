package com.tlc.crm.entity.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Objects;

/**
 * Manages {@link ForecastCategory} related dropdown actions.
 *
 * @author kishore J
 * @version 1.0
 */
@Dropdown(name = "forecastCategory", fieldAlias = "forecastCategory")
public class ForecastCategoryList implements DropdownAction {

    private final ForecastCategoryManager forecastCategoryManager;

    public ForecastCategoryList() {
        forecastCategoryManager = ForecastCategoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return The {@link OptionContainer}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = forecastCategoryManager.getTotalRecords(orgId);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<ForecastCategory> forecastCategories = forecastCategoryManager.get(orgId, new LimitConfig(dropdownLimit.start(), dropdownLimit.limit()));
            final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();

            for (final ForecastCategory forecastCategory : forecastCategories) {
                final I18nKey displayName = forecastCategory.getDisplayName();
                final String name = Objects.nonNull(displayName) && !forecastCategory.isBestCase() ? i18NResolutionAdapter.get(displayName) : forecastCategory.getName();

                if (forecastCategory.isDefault()) {
                    final JsonObject forecastCategoryJson = Json.object();

                    forecastCategoryJson.put(OptionKeys.IS_DEFAULT, true);
                    forecastCategoryJson.put(OptionKeys.VALUE, forecastCategory.id());
                    forecastCategoryJson.put(OptionKeys.NAME, name);
                    optionContainer.addOption(forecastCategoryJson);
                } else {
                    optionContainer.addOption(forecastCategory.id(), name);
                }
            }
        }
        return optionContainer;
    }
}
