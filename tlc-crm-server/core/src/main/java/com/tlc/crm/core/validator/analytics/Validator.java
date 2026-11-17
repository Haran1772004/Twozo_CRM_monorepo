package com.tlc.crm.core.validator.analytics;

import com.tlc.crm.core.validator.analytics.fields.FieldRequestValidator;
import com.tlc.crm.core.validator.analytics.layout.dashboard.DashboardRequestValidator;
import com.tlc.crm.core.validator.analytics.layout.share.DashboardShareRequestValidator;
import com.tlc.crm.core.validator.analytics.layout.template.TemplateRequestValidator;
import com.tlc.crm.core.validator.analytics.layout.view.ViewRequestValidator;
import com.tlc.crm.core.validator.analytics.layout.view.axis.AxisRequestValidator;

import com.tlc.crm.core.validator.analytics.layout.view.fav.FavouriteRequestValidator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Provides the functionality to validate the dashboard, view, axis, fields and filter configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class Validator implements DashboardRequestValidator,
                                  ViewRequestValidator,
                                  FieldRequestValidator,
                                  AxisRequestValidator,
                                  FavouriteRequestValidator,
                                  DashboardShareRequestValidator,
                                  TemplateRequestValidator
{
    private static class Instance
    {
        private static final Validator INSTANCE = new Validator();
    }

    public static Validator INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
