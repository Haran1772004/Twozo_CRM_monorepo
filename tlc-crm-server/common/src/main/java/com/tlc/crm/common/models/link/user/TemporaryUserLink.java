package com.tlc.crm.common.models.link.user;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.status.TemporaryUserLinkErrorCode;

import com.tlc.web.model.link.TemporaryLink;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents a {@link TemporaryUserLink} to manage temporary links tied to users.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TemporaryLink
 */
public class TemporaryUserLink extends TemporaryLink implements TlcModel, EmailNotifiableModel {

    private long orgId;
    private long id;
    private Category category;

    @EmailAlertField(
            fieldType = EmailAlertFieldType.TLC_MODEL
    )
    private CrmUser user;

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public CrmUser getUser() {
        return user;
    }

    public void setUser(final CrmUser user) {
        this.user = user;
    }

    /**
     * <p>
     * Gets the {@link TemporaryUserLink}.
     * </p>
     *
     * @param <T>
     * @return {@link TemporaryUserLink}
     */
    public <T> T get() {
        return (T) this;
    }

    /**
     * {@inheritDoc}
     *
     * @return id
     */
    @Override
    public Long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return orgId
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return identity
     */
    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Represents different {@link Category} of user-related actions that might be tracked.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum Category {

        /**
         * Represents the invitation category, typically used when a user is invited to
         * join the application.
         */
        INVITE((short) 1),

        /**
         * Represents the forgot password category, which is triggered when a user requests
         * to reset their password.
         */
        FORGOT_PASSWORD((short) 2),
        ;

        private final short id;

        Category(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Category} by given id.
         * </p>
         *
         * @param id Represents the category id
         * @return {@link Category}
         */
        public static Category get(final short id) {
            return switch (id) {
                case 1 -> INVITE;
                case 2 -> FORGOT_PASSWORD;
                default -> throw ErrorCode.get(TemporaryUserLinkErrorCode.INVALID_CATEGORY,
                        "i18n.user.expiry.link.category.invalid");
            };
        }
    }
}
