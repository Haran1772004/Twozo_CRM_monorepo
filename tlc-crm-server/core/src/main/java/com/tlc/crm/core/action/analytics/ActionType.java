package com.tlc.crm.core.action.analytics;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;
import lombok.NonNull;

/**
 * <p>
 * Manages Action Type related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see ReportsAction
 */
@Getter
public enum ActionType {

    CREATE("create"),
    UPDATE("update"),
    GET("get"),
    DELETE("delete"),
    APPLY("apply"),
    VIEW("view"),
    LIST("list"),
    VIEW_DATA("viewData"),
    CLONE("clone"),
    MOVE("move"),
    FAV("fav"),
    TRASH("trash"),
    RESTORE("restore"),
    INIT_EXPORT("initExport"),
    LOAD("load");

    private final String actionType;

    ActionType(String actionType) {
        this.actionType = actionType;
    }

    /**
     * Resolves {@link ActionType} from the given action key.
     *
     * @param type Represents the action key.
     * @return The resolved {@link ActionType}.
     */
    @NonNull
    public static ActionType getActionType(final @NonNull String type) {
        return switch (type.toUpperCase()) {
            case "CREATE" -> CREATE;
            case "UPDATE" -> UPDATE;
            case "GET" -> GET;
            case "DELETE" -> DELETE;
            case "APPLY" -> APPLY;
            case "VIEW" -> VIEW;
            case "LIST" -> LIST;
            case "VIEW_DATA" -> VIEW_DATA;
            case "CLONE" -> CLONE;
            case "MOVE" -> MOVE;
            case "FAV" -> FAV;
            case "TRASH" -> TRASH;
            case "RESTORE" -> RESTORE;
            case "INITEXPORT" -> INIT_EXPORT;
            case "LOAD" -> LOAD;
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_ACTION_TYPE, "i18n.analytics.invalid.action.type");
        };
    }
}
