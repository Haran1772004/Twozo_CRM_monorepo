package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents the variant type of {@link EntityEmail},
 * encapsulating the variant enum and its associated metadata.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailVariant {

    private Long id;
    private final Type type;

    public EmailVariant(final long id, final Type type) {
        this.id = id;
        this.type = type;
    }

    public EmailVariant(final Type type) {
        this.type = type;
    }

    public static EmailVariant of(final Type type) {
        return new EmailVariant(type.getValue(), type);
    }

    public static EmailVariant fromValue(final short value) {
        return of(Type.fromValue(value));
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public boolean isSmartBcc() {
        return type.isSmartBcc();
    }

    public boolean isTeamInbox() {
        return Objects.equals(type, Type.TEAM_INBOX);
    }

    public boolean isSyncEmail() {
        return type == Type.SYNC_EMAIL;
    }

    public boolean isOrgMail() {
        return Objects.equals(type, Type.ORG_MAIL);
    }

    public short getValue() {
        return type.getValue();
    }

    public static Type get(final String name) {
        for (final Type type : Type.values()) {

            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_VARIANT_TYPE);
    }

    public static EmailVariant fromName(final String name) {
        return new EmailVariant(get(name));
    }

    @Override
    public boolean equals(final Object variant) {
        if (this == variant) {
            return true;
        }

        if (!(variant instanceof EmailVariant other)) {
            return false;
        }

        return id == other.id && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    /**
     * <p>
     * Enumerates the supported email variant types.
     * </p>
     */
    public enum Type {

        SYNC_EMAIL((short) 1),
        SMART_BCC((short) 2),
        TEAM_INBOX((short) 3),
        ORG_MAIL((short) 4);

        private final short value;

        Type(final short value) {
            this.value = value;
        }

        public short getValue() {
            return value;
        }

        public static Type fromValue(final short value) {
            for (final Type type : Type.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_EMAIL_VARIANT);
        }

        public boolean isSmartBcc() {
            return this == SMART_BCC;
        }

        public boolean isSyncType() {
            return Objects.equals(this, SYNC_EMAIL);
        }
    }
}
