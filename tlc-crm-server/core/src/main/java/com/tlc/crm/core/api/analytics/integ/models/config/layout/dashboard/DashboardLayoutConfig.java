package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;

/**
 * Represents the layout configuration of a dashboard.
 *
 * @author saravanan
 * @version 1.0
 */
public class DashboardLayoutConfig {

    private final JsonObject layoutJson;
    private final String id;
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final int viewPosition;

    public DashboardLayoutConfig(final String layoutAsString) {
        this.layoutJson = Json.object(layoutAsString);

        this.viewPosition = layoutJson.getInt(CRMChildViewKeys.VIEW_POSITION);
        this.id = layoutJson.getString(CRMReportKeys.ID);
        this.x = layoutJson.optInt(CRMReportKeys.X_POSITION, 0);
        this.y = layoutJson.optInt(CRMReportKeys.Y_POSITION, 0);
        this.w = layoutJson.optInt(CRMReportKeys.WIDTH, 0);
        this.h = layoutJson.optInt(CRMReportKeys.HEIGHT, 0);
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int getViewPosition() {
        return viewPosition;
    }

    public JsonObject getJson() {
        return layoutJson;
    }
}
