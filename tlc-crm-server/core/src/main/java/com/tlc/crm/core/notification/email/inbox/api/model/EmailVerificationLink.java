package com.tlc.crm.core.notification.email.inbox.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.web.model.link.TemporaryLink;

/**
 * <p>
 * Represents a verification link used in email-related notifications, providing additional
 * metadata such as organization ID, user ID, tracking ID, category, and email address.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailVerificationLink extends TemporaryLink implements EmailNotifiableModel {

    private long orgId;
    private long id;
    private long userId;
    private String trackingId;

    private Category category;
    private UserSyncAddressInfo emailAddress;

    /**
     * <p>
     * Retrieves the organization ID.
     * </p>
     *
     * @return the organization ID
     */
    public long getOrgId() {
        return orgId;
    }

    /**
     * <p>
     * Sets the organization ID.
     * </p>
     *
     * @param orgId the organization ID to set
     */
    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    /**
     * <p>
     * Retrieves the unique identifier.
     * </p>
     *
     * @return the link ID
     */
    public long getId() {
        return id;
    }

    /**
     * <p>
     * Sets the unique identifier.
     * </p>
     *
     * @param id the link ID to set
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * <p>
     * Retrieves the user ID.
     * </p>
     *
     * @return the user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * <p>
     * Sets the user ID.
     * </p>
     *
     * @param userId the user ID to set
     */
    public void setUserId(final long userId) {
        this.userId = userId;
    }

    /**
     * <p>
     * Retrieves the category.
     * </p>
     *
     * @return the category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * <p>
     * Sets the category.
     * </p>
     *
     * @param category the category to set
     */
    public void setCategory(final Category category) {
        this.category = category;
    }

    /**
     * <p>
     * Retrieves the email address.
     * </p>
     *
     * @return the email address information
     */
    public UserSyncAddressInfo getEmailAddress() {
        return emailAddress;
    }

    /**
     * <p>
     * Sets the email address information.
     * </p>
     *
     * @param emailAddress the email address information to set
     */
    public void setEmailAddress(final UserSyncAddressInfo emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * <p>
     * Retrieves the tracking identifier.
     * This can be used for monitoring or auditing link usage.
     * </p>
     *
     * @return the tracking ID
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * <p>
     * Sets the tracking identifier.
     * </p>
     *
     * @param trackingId the tracking ID to set
     */
    public void setTrackingId(final String trackingId) {
        this.trackingId = trackingId;
    }

    /**
     * <p>
     * Defines the possible categories that a verification link can belong to.
     * Each category is associated with a unique identifier.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Category {

        /**
         * Represents a link associated with a team inbox.
         */
        TEAM_INBOX((short) 1);

        private final short id;

        Category(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Retrieves the numeric identifier for this category.
         * </p>
         *
         * @return the category ID
         */
        public short getId() {
            return id;
        }

        /**
         * <p>
         * Resolves a category by its identifier.
         * </p>
         *
         * @param id the category ID
         * @return the corresponding {@link Category}
         * @throws ErrorCode if the provided ID does not match any category
         */
        public static Category get(final short id) {
            return switch (id) {
                case 1 -> Category.TEAM_INBOX;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_CATEGORY);
            };
        }
    }
}