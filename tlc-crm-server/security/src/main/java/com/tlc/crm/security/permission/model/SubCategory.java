package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;

import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.i18n.I18nKey;

import java.io.Serializable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A model that represents the subCategory of the permission.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SubCategory implements Serializable {

    private Long id;
    private Category category;
    private final Type type;
    private Scope scope;
    private I18nKey displayName;
    private Requisites requisites;
    private static final Set<Type> NON_SOURCE_ANALYTICS_SUBCATEGORIES =  Set.of(Type.ACTIVITIES, Type.EMAILS,
            Type.PHONE_CALLS, Type.SALES_GOALS, Type.DEAL_LINE_ITEMS, Type.CALLS);

    public SubCategory(final String name) {
        this(Type.get(name));
    }

    public SubCategory(final Type type) {
        this.type = type;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return type.name;
    }

    public String getAliasName() {
        return type.aliasName;
    }

    public Type getType() {
        return type;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public Requisites getRequisites() {
        return requisites;
    }

    public void setRequisites(final String requisites) {
        this.requisites = new Requisites(Json.object(requisites));
    }

    public void setRequisites(final Requisites requisites) {
        this.requisites = requisites;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(final Scope scope) {
        if (Objects.isNull(scope)) {
            this.scope = null;
        } else {
            final Collection<Scope> subCategoryScopes = Scope.getSubCategoryScopes( type, category.getType() );

            if (Objects.nonNull(subCategoryScopes) && subCategoryScopes.contains(scope)) {
                this.scope = scope;
                return;
            }
            throw ErrorCode.get(PermissionErrorCodes.INVALID_SCOPE, String.format("%s is not a valid scope for the %s", scope.getName(), type.getName()));
        }
    }

    /**
     * <p>
     * Provides a flag that indicates whether the sub-category type has scope or not.
     * </p>
     *
     * @return A flag that indicates whether the sub-category type has scope or not.
     */
    public boolean hasScope() {
        return switch (type) {
            case CONTACTS, COMPANIES, DEALS, PRODUCTS, VIEW_QUOTAS, CREATE_QUOTAS, EDIT_QUOTAS, DELETE_QUOTAS, TEAM_VIEW,
                 HIERARCHY_VIEW, VIEW_GOALS, CREATE_GOALS, EDIT_GOALS, DELETE_GOALS, VIEW_EMAIL_CONVERSATION -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Provides a copy of the current {@link SubCategory}.
     * </p>
     *
     * @return Copied {@link SubCategory}.
     */
    public SubCategory copy() {
        final SubCategory subCategory = new SubCategory(type);

        subCategory.setId(id);
        subCategory.setDisplayName(displayName);
        subCategory.setRequisites(requisites);
        subCategory.setScope(scope);

        if (Objects.nonNull(category)) {
            subCategory.setCategory(category.copy());
        }
        return subCategory;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean equals(final Object secondaryPermission) {
        return this == secondaryPermission || (secondaryPermission instanceof SubCategory && hashCode() == secondaryPermission.hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, type, category);
    }

    /**
     * <p>
     * Provides the constant value of the {@link SubCategory}.
     * </p>
     */
    public enum Type implements Serializable {

        CONTACTS("contacts", "contact"),
        COMPANIES("companies", "company"),
        DEALS("deals", "deal"),
        PRODUCTS("products", "product"),
        ACTIVITIES("activities", "activity"),
        LIMIT_FOR_CREATING_RECORDS_THROUGH_IMPORT("limitForCreatingRecordsThroughImport"),
        LIMIT_FOR_UPDATING_RECORDS_THROUGH_IMPORT("limitForUpdatingRecordsThroughImport"),
        VIEW_QUOTAS("viewQuotas", "view"),
        CREATE_QUOTAS("createQuotas", "create"),
        EDIT_QUOTAS("editQuotas", "edit"),
        DELETE_QUOTAS("deleteQuotas", "delete"),
        RECALCULATE_QUOTAS("recalculateQuotas", "recalculate"),
        TEAM_VIEW("teamView"),
        HIERARCHY_VIEW("hierarchyView"),
        COMMIT_DEAL("commitDeal"),
        OVERRIDE("override"),
        VIEW_GOALS("viewGoals", "view"),
        CREATE_GOALS("createGoals", "create"),
        EDIT_GOALS("editGoals", "edit"),
        DELETE_GOALS("deleteGoals", "delete"),
        RECALCULATE_GOALS("recalculateGoals", "recalculate"),
        VIEW_EMAIL_CONVERSATION("viewEmailConversation"),
        SHARE_EMAIL_TEMPLATES_FOLDER("shareEmailTemplatesFolder"),
        BULK_EMAIL_LIMIT("bulkEmailLimit"),
        INDIVIDUAL_EMAIL_LIMIT("individualEmailLimit"),
        ACCESS_TO_PHONE("accessToPhone"),
        ALLOW_USERS_TO_MAKE_PHONE_CALLS("allowUsersToMakePhoneCalls"),
        CALL_LOGS("callLogs", "callLog"),
        WEB_FORM_TRACKING("webFormTracking"),
        BULK_SMS_LIMIT("bulkSmsLimit"),
        INDIVIDUAL_SMS_LIMIT("individualSmsLimit"),
        ACCESS_ADMIN_SETTINGS("accessAdminSettings", "adminSettings"),
        MANAGE_SALES_ACTIVITIES("manageSalesActivities", "activityType"),
        MANAGE_SALES_FORECAST_CATEGORIES("manageSalesForecastCategories", "salesForecastCategories"),
        MANAGE_WORKFLOWS("manageWorkFlows", "workFlow"),
        MANAGE_TERRITORIES("manageTerritories", "territory"),
        MANAGE_USERS("manageUsers", "user"),
        MANAGE_ALL_TAGS("manageAllTags", "tags"),
        EXPORT_ALL_DATA("exportAllData"),
        MIGRATE_FROM_A_CRM("migrateFromACrm"),
        MANAGE_ROLES("manageRoles", "role"),
        MANAGE_INTEGRATIONS_FOR_CHAT("manageIntegrationsForChat", "integrationsForChat"),
        MANAGE_CONVERSATIONAL_CHANNELS("manageConversationalChannels", "conversationalChannels"),
        MANAGE_PHONE_SETTINGS("managePhoneSettings", "phoneSettings"),
        MANAGE_SALES_TEAM("manageSalesTeam", "team"),
        COMMON("common"),
        CONTACT_FOLLOWERS_RECORDS("contactFollowersRecords"),
        COMPANY_FOLLOWERS_RECORDS("companyFollowersRecords"),
        DEAL_FOLLOWERS_RECORDS("dealFollowersRecords"),
        CONTACT_FOLLOWERS("contactFollowers"),
        COMPANY_FOLLOWERS("companyFollowers"),
        DEAL_FOLLOWERS("dealFollowers"),
        VIEW_ONLY("viewOnly"),
        VIEW_AND_EDIT("viewAndEdit"),
        VIEW_EDIT_AND_MANAGE("viewEditAndManage"),
        DEAL_LINE_ITEMS("dealLineItems", "dealLineItems"),
        EMAILS("emails"),
        PHONE_CALLS("phoneCalls"),
        SALES_GOALS("salesGoals"),
        CALLS("calls"),
        LISTS("lists"),
        MOVE_CONTACTS_TO_LIST("moveContactsToList"),
        COPY_CONTACTS_FROM_LIST("copyContactsFromList"),
        REMOVE_CONTACTS_FROM_LIST("removeContactsFromList"),
        MARKETING_SEGMENTS("marketingSegments"),
        USER_FOLLOWERS_RECORDS("userFollowersRecords"),
        USER_FOLLOWERS("userFollowers"),
        ;

        private final String name;
        private final String aliasName;

        Type(final String name) {
            this(name, name);
        }

        Type(final String name, final String aliasName) {
            this.name = name;
            this.aliasName = aliasName;
        }

        /**
         * <p>
         * Provides the name of the {@link SubCategory}.
         * </p>
         *
         * @return Name of the {@link SubCategory}.
         */
        public String getName() {
            return name;
        }

        public String getAliasName() {
            return aliasName;
        }

        /**
         * <p>
         * Provides a flag that indicates whether the subcategory has basic(CRUD) actions or not.
         * </p>
         *
         * @return Subcategory has basic(CRUD) actions.
         */
        public boolean hasBasicActions() {
            return switch (this) {
                case CONTACTS, COMPANIES, DEALS, PRODUCTS, ACTIVITIES, CONTACT_FOLLOWERS_RECORDS, COMPANY_FOLLOWERS_RECORDS,
                     DEAL_FOLLOWERS_RECORDS, USER_FOLLOWERS_RECORDS, CALL_LOGS, WEB_FORM_TRACKING, MANAGE_SALES_ACTIVITIES, MANAGE_SALES_TEAM,
                     MANAGE_USERS, MANAGE_TERRITORIES, MANAGE_ROLES, LISTS, MARKETING_SEGMENTS -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Provides a flag that indicates whether the subcategory is related to limit value or not.
         * </p>
         *
         * @return Subcategory has been related to limit.
         */
        public boolean isRelatedToLimit() {
            return switch (this) {
                case BULK_EMAIL_LIMIT, BULK_SMS_LIMIT, INDIVIDUAL_EMAIL_LIMIT, INDIVIDUAL_SMS_LIMIT,
                     LIMIT_FOR_CREATING_RECORDS_THROUGH_IMPORT, LIMIT_FOR_UPDATING_RECORDS_THROUGH_IMPORT -> true;
                default -> false;
            };
        }

        public boolean isScopeRequiredForCurrentUserPermission() {
            return switch (this) {
                case CREATE_GOALS, CREATE_QUOTAS -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Provides the collection of ignorable {@link Action.Type} of the {@link SubCategory.Type}.
         * </p>
         *
         * @return Collection of ignorable {@link Action.Type}.
         */
        public Collection<Action.Type> getIgnorableActions() {
            return switch (this) {
                case MANAGE_SALES_ACTIVITIES, MANAGE_ROLES -> Set.of(Action.Type.VIEW);
                default -> Collections.emptySet();
            };
        }

        /**
         * <p>
         * Provides the {@link Type} for the given name.
         * </p>
         *
         * @param name Represents the sub-category name.
         * @return {@link Type} holds the details of the sub-category.
         */
        public static Type get(final String name) {
            return switch (name.toLowerCase()) {
                case "contacts" -> CONTACTS;
                case "companies" -> COMPANIES;
                case "deals" -> DEALS;
                case "products" -> PRODUCTS;
                case "activities" -> ACTIVITIES;
                case "limitforcreatingrecordsthroughimport" -> LIMIT_FOR_CREATING_RECORDS_THROUGH_IMPORT;
                case "limitforupdatingrecordsthroughimport" -> LIMIT_FOR_UPDATING_RECORDS_THROUGH_IMPORT;
                case "viewquotas" -> VIEW_QUOTAS;
                case "createquotas" -> CREATE_QUOTAS;
                case "editquotas" -> EDIT_QUOTAS;
                case "deletequotas" -> DELETE_QUOTAS;
                case "recalculatequotas" -> RECALCULATE_QUOTAS;
                case "teamview" -> TEAM_VIEW;
                case "hierarchyview" -> HIERARCHY_VIEW;
                case "commitdeal" -> COMMIT_DEAL;
                case "override" -> OVERRIDE;
                case "viewgoals" -> VIEW_GOALS;
                case "creategoals" -> CREATE_GOALS;
                case "editgoals" -> EDIT_GOALS;
                case "deletegoals" -> DELETE_GOALS;
                case "recalculategoals" -> RECALCULATE_GOALS;
                case "viewemailconversation" -> VIEW_EMAIL_CONVERSATION;
                case "shareemailtemplatesfolder" -> SHARE_EMAIL_TEMPLATES_FOLDER;
                case "bulkemaillimit" -> BULK_EMAIL_LIMIT;
                case "individualemaillimit" -> INDIVIDUAL_EMAIL_LIMIT;
                case "accesstophone" -> ACCESS_TO_PHONE;
                case "allowuserstomakephonecalls" -> ALLOW_USERS_TO_MAKE_PHONE_CALLS;
                case "calllogs" -> CALL_LOGS;
                case "webformtracking" -> WEB_FORM_TRACKING;
                case "bulksmslimit" -> BULK_SMS_LIMIT;
                case "individualsmslimit" -> INDIVIDUAL_SMS_LIMIT;
                case "accessadminsettings" -> ACCESS_ADMIN_SETTINGS;
                case "managesalesactivities" -> MANAGE_SALES_ACTIVITIES;
                case "managesalesforecastcategories" -> MANAGE_SALES_FORECAST_CATEGORIES;
                case "manageworkflows" -> MANAGE_WORKFLOWS;
                case "manageterritories" -> MANAGE_TERRITORIES;
                case "manageusers" -> MANAGE_USERS;
                case "managealltags" -> MANAGE_ALL_TAGS;
                case "exportalldata" -> EXPORT_ALL_DATA;
                case "migratefromacrm" -> MIGRATE_FROM_A_CRM;
                case "manageroles" -> MANAGE_ROLES;
                case "manageintegrationsforchat" -> MANAGE_INTEGRATIONS_FOR_CHAT;
                case "manageconversationalchannels" -> MANAGE_CONVERSATIONAL_CHANNELS;
                case "managephonesettings" -> MANAGE_PHONE_SETTINGS;
                case "managesalesteam" -> MANAGE_SALES_TEAM;
                case "contactfollowersrecords" -> CONTACT_FOLLOWERS_RECORDS;
                case "companyfollowersrecords" -> COMPANY_FOLLOWERS_RECORDS;
                case "dealfollowersrecords" -> DEAL_FOLLOWERS_RECORDS;
                case "contactfollowers" -> CONTACT_FOLLOWERS;
                case "companyfollowers" -> COMPANY_FOLLOWERS;
                case "dealfollowers" -> DEAL_FOLLOWERS;
                case "deallineitems" -> DEAL_LINE_ITEMS;
                case "emails" -> EMAILS;
                case "phonecalls" -> PHONE_CALLS;
                case "salesgoals" -> SALES_GOALS;
                case "calls" -> CALLS;
                case "lists" -> LISTS;
                case "movecontactstolist" -> MOVE_CONTACTS_TO_LIST;
                case "copycontactsfromlist" -> COPY_CONTACTS_FROM_LIST;
                case "removecontactsfromlist" -> REMOVE_CONTACTS_FROM_LIST;
                case "marketingsegments" -> MARKETING_SEGMENTS;
                case "userfollowersrecords" -> USER_FOLLOWERS_RECORDS;
                case "userfollowers" -> USER_FOLLOWERS;
                default -> null;
            };
        }

        /**
         * <p>
         * Provides the {@link Type} for the given source name.
         * </p>
         *
         * @param source Represents the source name.
         * @return {@link Type} holds the details of the subcategory.
         */
        public static Type getFollowersTypeBySource(final String source) {
            return switch (source.toLowerCase()) {
                case "contact" -> CONTACT_FOLLOWERS;
                case "company" -> COMPANY_FOLLOWERS;
                case "deal" -> DEAL_FOLLOWERS;
                default -> throw ErrorCode.get(PermissionErrorCodes.ACTION_NOT_FOUND, String.format("No action found, Source: %s", source));
            };
        }

        /**
         * <p>
         * Provides the {@link Type} for the given dropdown name.
         * </p>
         *
         * @param dropdownName Represents the dropdown name.
         * @return {@link Type} holds the details of the sub-category.
         */
        public static Type getByDropdownName(final String dropdownName) {
            return switch (dropdownName.toLowerCase()) {
                case "contactlist", "contactrecipientslist" -> CONTACTS;
                case "companylist" -> COMPANIES;
                case "deallist" -> DEALS;
                case "productlist" -> PRODUCTS;
                default -> null;
            };
        }

        /**
         * <p>
         * Provides the {@link Action.Type} for the given exporter name.
         * </p>
         *
         * @param exporterName Represents the exporter name.
         * @return {@link Action.Type} holds the details of the action.
         */
        public static SubCategory.Type getByExporterName(final String exporterName) {
            return switch (exporterName.toLowerCase()) {
                case "contact" -> CONTACTS;
                case "company" -> COMPANIES;
                case "deal" -> DEALS;
                case "product" -> PRODUCTS;
                case "activity" -> ACTIVITIES;
                default -> null;
            };
        }

        /**
         * <p>
         * Checks whether the {@link SubCategory} has relative {@link Action} or not.
         * Ex: Subcategory - Contact, Actions - [View, Create, Edit, Delete], Relative Actions - [Assign Records, Bulk Update Records, Import Records, etc,...].
         * </p>
         *
         * @return A flag that indicates whether the {@link SubCategory} has relative {@link Action} or not.
         */
        public boolean hasRelativeActions() {
            return switch (this) {
                case CONTACTS, COMPANIES, DEALS, PRODUCTS, ACTIVITIES, CONTACT_FOLLOWERS, COMPANY_FOLLOWERS, DEAL_FOLLOWERS,
                     USER_FOLLOWERS -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks whether the {@link SubCategory} has primary actions or not.(Primary action = Access)
         * </p>
         *
         * @return A flag that indicates whether the {@link SubCategory} has primary actions or not.
         */
        public boolean hasPrimaryAction() {
            return switch (this) {
                case CONTACT_FOLLOWERS, COMPANY_FOLLOWERS, DEAL_FOLLOWERS, USER_FOLLOWERS -> false;
                default -> true;
            };
        }

        /**
         * <p>
         * Provides the relative {@link SubCategory.Type} of the current {@link SubCategory.Type}.
         * </p>
         *
         * @return Relative {@link SubCategory.Type}.
         */
        public Type getRelativeSubCategory() {
            return switch (this) {
                case CONTACT_FOLLOWERS -> CONTACT_FOLLOWERS_RECORDS;
                case COMPANY_FOLLOWERS -> COMPANY_FOLLOWERS_RECORDS;
                case DEAL_FOLLOWERS -> DEAL_FOLLOWERS_RECORDS;
                case USER_FOLLOWERS -> USER_FOLLOWERS_RECORDS;
                default -> this;
            };
        }

        /**
         * Provides the list of subcategory types in analytics which are not in the source.
         *
         * @return an unmodifiable collection of {@link Type} representing
         *         the analytics subcategories
         */
        public static Collection<Type> getNonSourceAnalyticsSubCategories() {
            return NON_SOURCE_ANALYTICS_SUBCATEGORIES;
        }
    }
}
