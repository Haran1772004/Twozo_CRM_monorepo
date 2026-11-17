package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * A model that represents the category of the permission.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Category implements Serializable {

    private Long id;
    private final Type type;

    public Category(final String name) {
        this(Type.get(name));
    }

    public Category(final Type type) {
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

    /**
     * <p>
     * Provides a copy of the current {@link Category}.
     * </p>
     *
     * @return Copied {@link Category}.
     */
    public Category copy() {
        final Category category = new Category(type);

        category.setId(id);
        return category;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean equals(final Object category) {
        return this == category || (category instanceof Type && hashCode() == category.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    /**
     * <p>
     * Provides the constant value of the category.
     * </p>
     */
    public enum Type implements Serializable {

        SOURCES("sources"),
        ACTIONS("actions"),
        QUOTAS_AND_FORECASTING("quotasAndForecasting"),
        ACTIVITY_GOALS("activityGoals", "goal"),
        EMAILS("emails", "email"),
        PHONE("phone"),
        LEAD_GENERATION("leadGeneration"),
        SMS("sms"),
        USER_SETTINGS("userSettings"),
        ADMIN_ACCESS("adminAccess", "settings"),
        FOLLOWERS("followers"),
        ANALYTICS("analytics", "analytics"),
        MARKETING_LISTS("marketingLists"),
        MARKETING_CAMPAIGNS("marketingCampaigns"),
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
         * Provides the name of the {@link Category}.
         * </p>
         *
         * @return Name of the {@link Category}.
         */
        public String getName() {
            return name;
        }

        public String getAliasName() {
            return aliasName;
        }

        /**
         * <p>
         * Provides the {@link Type} for the given name.
         * </p>
         *
         * @param name Represents the category name.
         * @return {@link Type} holds the details of the category.
         */
        public static Type get(final String name) {
            return switch (name) {
                case "sources" -> SOURCES;
                case "actions" -> ACTIONS;
                case "quotasAndForecasting" -> QUOTAS_AND_FORECASTING;
                case "activityGoals" -> ACTIVITY_GOALS;
                case "emails" -> EMAILS;
                case "phone" -> PHONE;
                case "leadGeneration" -> LEAD_GENERATION;
                case "sms" -> SMS;
                case "userSettings" -> USER_SETTINGS;
                case "adminAccess" -> ADMIN_ACCESS;
                case "followers" -> FOLLOWERS;
                case "analytics" -> ANALYTICS;
                case "marketingLists" -> MARKETING_LISTS;
                case "marketingCampaigns" -> MARKETING_CAMPAIGNS;
                default -> throw ErrorCode.get(PermissionErrorCodes.CATEGORY_NOT_FOUND, name);
            };
        }

        /**
         * <p>
         * Provides the relative {@link Category} of the {@link Category}.
         * </p>
         *
         * @return Related {@link Category}
         */
        public Category.Type getRelativeCategory() {
            return switch (this) {
                case SOURCES -> ACTIONS;
                case ACTIONS -> SOURCES;
                default -> this;
            };
        }

        /**
         * <p>
         * Checks if the current category has an interlink association
         * </p>
         *
         * @return true if the category supports interlinking, otherwise false
         */
        public boolean hasInterLinkCategory() {
            return switch (this) {
                case ANALYTICS -> true;
                default -> false;
            };
        }
    }
}
