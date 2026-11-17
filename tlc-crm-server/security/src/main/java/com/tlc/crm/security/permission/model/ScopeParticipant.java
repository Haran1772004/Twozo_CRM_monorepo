package com.tlc.crm.security.permission.model;

/**
 * Defines the participants relevant to the scopes of the permissions
 *
 * @author Sundar
 * @version 1.0
 */
public enum ScopeParticipant {

    USER,
    TERRITORY,
    TEAM,
    TEAM_USER,
    FOLLOWER,
    ;

    /**
     * Gets the {@link ScopeParticipant} corresponding to the scope
     *
     * @param scope Represents the scope
     * @return the {@link ScopeParticipant}
     */
    public static ScopeParticipant get(final Scope scope) {
        return switch (scope) {
            case RESTRICTED_ACCESS, USER_LEVEL_ACCESS, USERS_GOALS, USERS_QUOTAS, GOALS_CREATED_BY_USER,
                 QUOTAS_CREATED_BY_USER, VIEW_ONLY_PRIVATE_EMAILS -> USER;
            case TEAM_LEVEL_ACCESS, TEAM_QUOTAS, TEAMS_GOALS -> TEAM;
            case TERRITORY_ACCESS -> TERRITORY;
            case REPORT_LEVEL_ACCESS -> TEAM_USER;
            default -> null;
        };
    }
}