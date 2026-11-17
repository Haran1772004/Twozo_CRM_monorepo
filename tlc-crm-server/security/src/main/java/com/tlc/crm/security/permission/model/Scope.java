package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;

import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides the constant value of the scopes.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Scope implements Serializable {

    GLOBAL_ACCESS(1, "globalAccess"),
    TERRITORY_ACCESS(2, "territoryAccess"),
    RESTRICTED_ACCESS(3, "restrictedAccess"),
    ALL_QUOTAS(4, "allQuotas"),
    TEAM_QUOTAS(5, "teamQuotas"),
    USERS_QUOTAS(6, "usersQuotas"),
    QUOTAS_CREATED_BY_USER(7, "quotasCreatedByUser"),
    COMPANY_LEVEL_ACCESS(8, "companyLevelAccess"),
    TEAM_LEVEL_ACCESS(9, "teamLevelAccess"),
    USER_LEVEL_ACCESS(10, "userLevelAccess"),
    REPORT_LEVEL_ACCESS(11, "reportLevelAccess"),
    ALL_GOALS(12, "allGoals"),
    TEAMS_GOALS(13, "teamsGoals"),
    USERS_GOALS(14, "usersGoals"),
    GOALS_CREATED_BY_USER(15, "goalsCreatedByUser"),
    VIEW_ALL_EMAILS(16, "viewAllEmails"),
    VIEW_ONLY_PRIVATE_EMAILS(17, "viewOnlyPrivateEmails"),
    VIEW_ONLY(18, "viewOnly"),
    VIEW_AND_EDIT(19, "viewAndEdit"),
    VIEW_EDIT_AND_MANAGE(20, "viewEditAndManage"),
    ;

    private static final Map<Category.Type, Map<SubCategory.Type, Collection<Scope>>> SUB_CATEGORY_SCOPES_BY_CATEGORY;

    private final int type;
    private final String name;

    static {
        final Set<Scope> entityScopes = Set.of(GLOBAL_ACCESS, TERRITORY_ACCESS, RESTRICTED_ACCESS);
        final Set<Scope> quotasScopes = Set.of(ALL_QUOTAS, QUOTAS_CREATED_BY_USER);
        final Set<Scope> goalsScopes = Set.of(ALL_GOALS, GOALS_CREATED_BY_USER);
        final Set<Scope> analyticsScopes = Set.of(VIEW_ONLY, VIEW_AND_EDIT, VIEW_EDIT_AND_MANAGE);
        SUB_CATEGORY_SCOPES_BY_CATEGORY = new HashMap<>();

        SUB_CATEGORY_SCOPES_BY_CATEGORY.put( Category.Type.SOURCES,
                                             Map.of( SubCategory.Type.CONTACTS, entityScopes,
                                                     SubCategory.Type.COMPANIES, entityScopes,
                                                     SubCategory.Type.DEALS, entityScopes,
                                                     SubCategory.Type.PRODUCTS, Set.of( GLOBAL_ACCESS, RESTRICTED_ACCESS ) ) );
        SUB_CATEGORY_SCOPES_BY_CATEGORY.put( Category.Type.QUOTAS_AND_FORECASTING,
                                             Map.of( SubCategory.Type.VIEW_QUOTAS, Set.of(ALL_QUOTAS, TEAM_QUOTAS, USERS_QUOTAS),
                                                     SubCategory.Type.CREATE_QUOTAS, Set.of(ALL_QUOTAS, USERS_QUOTAS),
                                                     SubCategory.Type.EDIT_QUOTAS, quotasScopes,
                                                     SubCategory.Type.DELETE_QUOTAS, quotasScopes,
                                                     SubCategory.Type.TEAM_VIEW, Set.of(COMPANY_LEVEL_ACCESS, TEAM_LEVEL_ACCESS, USER_LEVEL_ACCESS),
                                                     SubCategory.Type.HIERARCHY_VIEW, Set.of(COMPANY_LEVEL_ACCESS, REPORT_LEVEL_ACCESS) ) );
        SUB_CATEGORY_SCOPES_BY_CATEGORY.put( Category.Type.ACTIVITY_GOALS,
                                             Map.of( SubCategory.Type.VIEW_GOALS, Set.of(ALL_GOALS, TEAMS_GOALS, USERS_GOALS),
                                                     SubCategory.Type.CREATE_GOALS, Set.of(ALL_GOALS, USERS_GOALS),
                                                     SubCategory.Type.EDIT_GOALS, goalsScopes,
                                                     SubCategory.Type.DELETE_GOALS, goalsScopes));
        SUB_CATEGORY_SCOPES_BY_CATEGORY.put( Category.Type.EMAILS,
                                             Map.of( SubCategory.Type.VIEW_EMAIL_CONVERSATION, Set.of( VIEW_ALL_EMAILS, VIEW_ONLY_PRIVATE_EMAILS) ) );
        SUB_CATEGORY_SCOPES_BY_CATEGORY.put( Category.Type.ANALYTICS,
                                             Map.of( SubCategory.Type.CONTACTS, analyticsScopes,
                                                     SubCategory.Type.COMPANIES, analyticsScopes,
                                                     SubCategory.Type.DEALS, analyticsScopes,
                                                     SubCategory.Type.DEAL_LINE_ITEMS, analyticsScopes,
                                                     SubCategory.Type.EMAILS, analyticsScopes,
                                                     SubCategory.Type.PHONE_CALLS, analyticsScopes,
                                                     SubCategory.Type.SALES_GOALS, analyticsScopes,
                                                     SubCategory.Type.ACTIVITIES, analyticsScopes,
                                                     SubCategory.Type.CALLS, analyticsScopes ) );
    }

    Scope(final int type, final String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * <p>
     * Provides the collection of {@link Scope} related to the given {@link SubCategory}.
     * </p>
     *
     * @param subCategoryType Represents the type of the sub-category
     * @param categoryType Represents the type of the categoryType
     * @return Collection of sub-category scopes.
     */
    public static Collection<Scope> getSubCategoryScopes( final SubCategory.Type subCategoryType, final Category.Type categoryType ) {
        return SUB_CATEGORY_SCOPES_BY_CATEGORY.get(categoryType).get(subCategoryType);
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Scope getDefaultScope() {
        return Scope.GLOBAL_ACCESS;
    }

    /**
     * <p>
     * Fetches the {@link Scope} for the given scope type.
     * </p>
     *
     * @param type Represents the type of the scope.
     * @return {@link Scope} holds the details of the scope.
     */
    public static Scope get(final int type) {
        return switch (type) {
            case 1 -> GLOBAL_ACCESS;
            case 2 -> TERRITORY_ACCESS;
            case 3 -> RESTRICTED_ACCESS;
            case 4 -> ALL_QUOTAS;
            case 5 -> TEAM_QUOTAS;
            case 6 -> USERS_QUOTAS;
            case 7 -> QUOTAS_CREATED_BY_USER;
            case 8 -> COMPANY_LEVEL_ACCESS;
            case 9 -> TEAM_LEVEL_ACCESS;
            case 10 -> USER_LEVEL_ACCESS;
            case 11 -> REPORT_LEVEL_ACCESS;
            case 12 -> ALL_GOALS;
            case 13 -> TEAMS_GOALS;
            case 14 -> USERS_GOALS;
            case 15 -> GOALS_CREATED_BY_USER;
            case 16 -> VIEW_ALL_EMAILS;
            case 17 -> VIEW_ONLY_PRIVATE_EMAILS;
            case 18 -> VIEW_ONLY;
            case 19 -> VIEW_AND_EDIT;
            case 20 -> VIEW_EDIT_AND_MANAGE;
            default -> throw ErrorCode.get(PermissionErrorCodes.SCOPE_NOT_FOUND, String.valueOf(type));
        };
    }

    /**
     * <p>
     * Fetches the {@link Scope} for the given scope name.
     * </p>
     *
     * @param name Represents the name of the scope.
     * @return {@link Scope} holds the details of the scope.
     */
    public static Scope get(final String name) {
        return switch (name.toLowerCase()) {
            case "globalaccess" -> GLOBAL_ACCESS;
            case "territoryaccess" -> TERRITORY_ACCESS;
            case "restrictedaccess" -> RESTRICTED_ACCESS;
            case "allquotas" -> ALL_QUOTAS;
            case "teamquotas" -> TEAM_QUOTAS;
            case "usersquotas" -> USERS_QUOTAS;
            case "quotascreatedbyuser" -> QUOTAS_CREATED_BY_USER;
            case "companylevelaccess" -> COMPANY_LEVEL_ACCESS;
            case "teamlevelaccess" -> TEAM_LEVEL_ACCESS;
            case "userlevelaccess" -> USER_LEVEL_ACCESS;
            case "reportlevelaccess" -> REPORT_LEVEL_ACCESS;
            case "allgoals" -> ALL_GOALS;
            case "teamsgoals" -> TEAMS_GOALS;
            case "usersgoals" -> USERS_GOALS;
            case "goalscreatedbyuser" -> GOALS_CREATED_BY_USER;
            case "viewallemails" -> VIEW_ALL_EMAILS;
            case "viewonlyprivateemails" -> VIEW_ONLY_PRIVATE_EMAILS;
            case "viewonly" -> VIEW_ONLY;
            case "viewandedit" -> VIEW_AND_EDIT;
            case "vieweditandmanage" -> VIEW_EDIT_AND_MANAGE;
            default -> throw ErrorCode.get(PermissionErrorCodes.SCOPE_NOT_FOUND, name);
        };
    }
}