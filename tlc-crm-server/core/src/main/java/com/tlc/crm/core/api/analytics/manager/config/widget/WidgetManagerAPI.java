package com.tlc.crm.core.api.analytics.manager.config.widget;

import com.tlc.crm.core.api.analytics.entity.config.widget.WidgetCompareUnit;
import com.tlc.crm.core.api.analytics.entity.config.widget.WidgetPeriod;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 * Provides an abstract way to manages the widget configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface WidgetManagerAPI
{
    /**
     * <p>
     * Returns an instance of the widget period related field configuration
     * </p>
     *
     * @return the {@link WidgetCompareUnit} instance for widget period related field configuration
     */
    default CRMViewConfigManager<WidgetPeriod> getWidgetPeriodManager()
    {
        return WidgetPeriodManager.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the widget compare related field configuration
     * </p>
     *
     * @return the {@link WidgetCompareUnit} instance for widget compare related field configuration
     */
    default CRMViewConfigManager<WidgetCompareUnit> getWidgetCompareManager()
    {
        return WidgetCompareManager.INSTANCE();
    }
}
