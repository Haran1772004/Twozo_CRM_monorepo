package com.tlc.crm.core.api.analytics.general.keys.layout;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to {@link DashboardLayout} model
 * </p>
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMDashboardKeys
{
    public static final String DASHBOARD_NAME = "dashboardName";
    public static final String DESCRIPTION    = "description";
    public static final String CHILD_VIEW     = "childViewInfo";
    public static final String VISIBILITY_ID = "visibilityId";
    public static final String ACCESS = "access";
    public static final String USERS = "users";
    public static final String TEAMS = "teams";
    public static final String TERRITORIES = "territories";
    public static final String EVERYONE = "everyone";
    public static final String IS_VISIBLE = "isVisible";
    public static final String SOURCE_DASHBOARD_ID = "sourceDashboardId";
    public static final String TARGET_DASHBOARD_ID = "targetDashboardId";
    public static final String DASHBOARD_ID = "dashboardId";
    public static final String IS_FAVOURITE = "isFavourite";
    public static final String IS_CURRENT_USER = "isCurrentUser";
    public static final String CURRENT_USER_CONFIG = "currentUserConfig";
    public static final String USER_PERMISSION = "userPermission";
    public static final String CAN_CLONE = "canClone";
    public static final String CAN_MOVE_TO_TRASH = "canMoveToTrash";
    public static final String CAN_RESTORE = "canRestore";
    public static final String CAN_DELETE = "canDelete";
    public static final String LAYOUT = "layout";
    public static final String CAN_EXPORT = "canExport";
}
