package com.tlc.crm.core.features.action.dropdown.quotasandForecasting;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.DefaultForecastCategory;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Manages the forecast list related dropdown actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "forecastList")
public class QuotaForecastList implements DropdownAction {

    private static final String FORECAST = "Forecast";
    private static final String WON_DEALS_PLACEHOLDER_KEY = "i18n.quota.won.deals.placeholder";
    private static final String DEALS_PLACEHOLDER_KEY = "i18n.quota.deals.placeholder";

    private final ForecastCategoryManager forecastCategoryManager;

    public QuotaForecastList() {
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
        final ForecastType[] forecastTypes = ForecastType.values();
        final Collection<ForecastCategory> forecastCategories = forecastCategoryManager.getAll(orgId);
        final OptionContainer optionContainer = new OptionContainer();
        final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final String commitedName = DefaultForecastCategory.COMMITTED.getName();
        final Locale locale = dropdownRequest.locale();
        final String wonDealsKey = i18NResolutionAdapter.get(WON_DEALS_PLACEHOLDER_KEY).toLowerCase(locale);
        final String dealsKey = i18NResolutionAdapter.get(DEALS_PLACEHOLDER_KEY).toLowerCase(locale);

        for (final ForecastType forecastType : forecastTypes) {
            final JsonObject optionJson = Json.object();
            final boolean isWon = forecastType.isWon();
            final String defaultSuffix = isWon ? wonDealsKey : dealsKey;

            optionJson.put(QuotaFields.ID, forecastType.getId());
            optionJson.put(QuotaFields.NAME, i18NResolutionAdapter.get(forecastType.getDisplayName()));
            optionJson.put(QuotaFields.IS_DEFAULT, forecastType.isDefault());
            final JsonArray summaryArray = Json.array();

            for (final ForecastCategory forecastCategory : forecastCategories) {
                final JsonObject forecastJson = Json.object();
                final I18nKey displayName = forecastCategory.getDisplayName();
                final boolean isBestCase = forecastCategory.isBestCase();
                final String name = Objects.nonNull(displayName) && !isBestCase ? i18NResolutionAdapter.get(displayName) : forecastCategory.getName();
                final String value;

                if (isWon && isBestCase) {
                    value = StringUtil.join(StringUtil.Delimiter.PLUS_WITH_SPACE, commitedName, name, defaultSuffix);
                } else if (isWon) {
                    value = StringUtil.join(StringUtil.Delimiter.PLUS_WITH_SPACE, name, defaultSuffix);
                } else {
                    value = StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, name, defaultSuffix);
                }
                forecastJson.put(QuotaFields.DISPLAY_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE,
                        name, FORECAST));
                forecastJson.put(QuotaFields.VALUE, value);

                summaryArray.put(forecastJson);
            }
            optionJson.put(QuotaFields.SUMMARY, summaryArray);
            optionContainer.addOption(optionJson);
        }
        return optionContainer;
    }
}
