package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonArray;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import java.util.Collection;

/**
 * <p>
 * Loads activity alerts.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractActivityAlertLoader extends AbstractAppNotificationAlertLoader<ActivityHolder> {

    /**
     * <p>
     * Load alerts.
     * </p>
     *
     * @param orgId      An organization id.
     * @param modelsData Converted model data.
     */
    protected abstract void loadAlerts(final long orgId, final JsonArray modelsData);

    /**
     * <p>
     * Converts models
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param activityHolders Represents the collection {@link ActivityHolder}.
     */
    @Override
    public void load(final long orgId, final Collection<ActivityHolder> activityHolders) {
        loadAlerts(orgId, appNotificationConverter.convertActivities(activityHolders));
    }
}
