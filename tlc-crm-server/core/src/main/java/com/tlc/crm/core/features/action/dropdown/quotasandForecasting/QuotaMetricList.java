package com.tlc.crm.core.features.action.dropdown.quotasandForecasting;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;

import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Manages the quota metric related dropdown actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "quotaMetricList")
public class QuotaMetricList implements DropdownAction {

    public QuotaMetricList() {
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @return The {@link OptionContainer} containing all quota metric options
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final OptionContainer optionContainer = new OptionContainer();
        final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();

        for (final QuotaMetric quotaMetric : QuotaMetric.values()) {
            final JsonObject quotaJson = Json.object()
                    .put(QuotaFields.ID, quotaMetric.getId())
                    .put(QuotaFields.NAME, i18NResolutionAdapter.get(quotaMetric.getDisplayName()));

            if (quotaMetric == QuotaMetric.DEAL_VALUE) {
                quotaJson.put(QuotaFields.IS_DEFAULT, true);
            }
            optionContainer.addOption(quotaJson);
        }
        return optionContainer;
    }
}
