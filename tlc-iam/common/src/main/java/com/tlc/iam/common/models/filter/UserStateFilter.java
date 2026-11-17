package com.tlc.iam.common.models.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;

/**
 * <p>
 * Represents the state filter for {@link User}
 * </p>
 *
 * @author pandiarajkuamrG
 * @version 1.0
 */
public enum UserStateFilter {

    ALL_USERS((short) 1, "All Users"),
    ACTIVE((short) 2, "Active"),
    DEACTIVATED((short) 3, "Deactivated"),
    INVITED((short) 4, "Invited");

    private final short id;
    private final String name;

    UserStateFilter(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public short id() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static UserStateFilter get(final short id) {
        return switch (id) {
            case 1 -> ALL_USERS;
            case 2 -> ACTIVE;
            case 3 -> DEACTIVATED;
            case 4 -> INVITED;
            default -> throw ErrorCode.get(IamErrorCodes.INVALID_USER_STATE_FILTER);
        };
    }

    public static UserStateFilter get(final String name) {
        return switch (name) {
            case "ALL_USERS", "allUsers" -> ALL_USERS;
            case "ACTIVE", "active" -> ACTIVE;
            case "DEACTIVATED", "deactivated" -> DEACTIVATED;
            case "INVITED", "invited" -> INVITED;
            default -> throw ErrorCode.get(IamErrorCodes.INVALID_USER_STATE_FILTER);
        };
    }
}
