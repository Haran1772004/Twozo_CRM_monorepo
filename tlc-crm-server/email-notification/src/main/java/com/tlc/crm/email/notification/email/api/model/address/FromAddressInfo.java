package com.tlc.crm.email.notification.email.api.model.address;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.i18n.I18nKey;

import java.util.Objects;

/**
 * <p>
 * Provides methods to interact with and manipulate the {@link FromAddressInfo}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class FromAddressInfo extends UserSyncAddressInfo {

    private Long userId;
    private Type type;
    private boolean isActive;
    private boolean isVerified;

    public FromAddressInfo(final String emailId) {
        super(emailId);
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public Long getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(final boolean verified) {
        isVerified = verified;
    }

    /**
     * <p>
     * Represents the from address {@link Type}.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum Type {
        ORG_EMAIL(new I18nKey("i18n.notifications.from.address.org.email"), (short) 1),
        TEAM_INBOX(new I18nKey("i18n.notifications.from.address.team.inbox"), (short) 2),
        SYNC_EMAIL(new I18nKey("i18n.notifications.from.address.sync.email"), (short) 3),
        CAMPAIGN(new I18nKey("i18n.notifications.from.address.campaign.email"), (short) 4),
        ;

        private final I18nKey i18nKey;
        private final short id;

        public I18nKey getI18nKey() {
            return i18nKey;
        }

        public short getId() {
            return id;
        }

        Type(final I18nKey i18nKey, final short id) {
            this.i18nKey = i18nKey;
            this.id = id;
        }

        /**
         * <p>
         * Gets the type of {@link FromAddressInfo}.
         * </p>
         *
         * @param id Represents the from address Type id
         * @return {@link Type}
         */
        public static Type getType(final short id) {
            return switch (id) {
                case 1 -> ORG_EMAIL;
                case 2 -> TEAM_INBOX;
                case 3 -> SYNC_EMAIL;
                case 4 -> CAMPAIGN;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_FROM_TYPE,
                        "i18n.notifications.from.address.type.invalid");
            };
        }

        public boolean isSyncType() {
            return Objects.equals(SYNC_EMAIL, this);
        }

        public boolean isTeamInboxType() {
            return Objects.equals(TEAM_INBOX, this);
        }
    }
}
