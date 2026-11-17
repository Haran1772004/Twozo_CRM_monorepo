package com.tlc.analytics.config.action.api.view;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.action.validator.AnalyticsValidator;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.sql.resource.ANAPERMANENTLINK;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.commons.functions.TriFunction;
import com.tlc.crm.common.models.Action;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Manages the report fields configuration related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction( path="/analytics/config/reports_fields" )
public class ReportFieldsAction extends AnalyticsConfigAction<ReportField>
{
    @Override
    protected @NonNull Class<ReportField> getModelClass()
    {
        return ReportField.class;
    }

    @Override
    protected @NonNull TriFunction<String, String, Object, Object> jsonValueTransformer()
    {
        return ( tableName, columName, value ) ->
        {
            if( ANAPERMANENTLINK.TABLE.equals( tableName ) )
            {
                if( ANAPERMANENTLINK.PASSWORD.equals( columName ) )
                {
                    return null;
                }
            }
            return value;
        };
    }

    @Override
    public ConfigManager<ReportField> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE().getReportFieldManager();
    }

    @Override
    protected boolean isValidDetails( final @NonNull Long orgId,
                                      final @NonNull Long userId,
                                      final @NonNull Action action,
                                      final @NonNull Collection<ReportField> models )
    {
        return AnalyticsValidator.INSTANCE()
                                 .getReportsFieldValidator()
                                 .isValid( orgId, models );
    }

}
