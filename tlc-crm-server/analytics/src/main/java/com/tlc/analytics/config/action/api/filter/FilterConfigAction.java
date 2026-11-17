package com.tlc.analytics.config.action.api.filter;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.action.validator.AnalyticsValidator;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.filter.FilterField;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Manages the filter configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/filter")
public class FilterConfigAction extends AnalyticsConfigAction<FilterField>
{
    @Override
    protected @NonNull Class<FilterField> getModelClass()
    {
        return FilterField.class;
    }

    @Override
    public ConfigManager<FilterField> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getFilterFieldManager();
    }

    @Override
    protected boolean isValidDetails( final @NonNull Long orgId,
                                      final @NonNull Long userId,
                                      final @NonNull Action action,
                                      final @NonNull Collection<FilterField> models )
    {
        return AnalyticsValidator.INSTANCE()
                                 .getFilterValidator()
                                 .isValid( orgId, models );
    }
}
