package com.tlc.crm.core.conversation.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.conversation.status.EntityConversationErrorCodes;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Model which represents the {@link EntityConversation}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public abstract class EntityConversation<T extends ConversableModel> implements TlcModel {

    private Long id;
    private Long createdAt;
    private Type type;
    private Long userId;
    private Long orgId;
    private CrmUser crmUser;

    /**
     * <p>
     * Represents the {@link Type} of {@link EntityConversation}
     * </p>
     *
     * @author Kavinila E
     */
    public enum Type {

        CALL(1),
        EMAIL(2),
        MANUALLY_LOGGED_CALL(3),
        FACEBOOK(4),
        INSTAGRAM(5),
        WHATSAPP(6),
        ;

        private final int id;

        public int getId() {
            return id;
        }

        Type(final int id) {
            this.id = id;
        }

        /**
         * Gets the {@link Type} by its ID
         *
         * @param id Represents the {@link Type} ID
         * @return the {@link Type} associated with the ID
         */
        public static Type getById(final int id) {
            return switch (id) {
                case 1 -> CALL;
                case 2 -> EMAIL;
                case 3 -> MANUALLY_LOGGED_CALL;
                case 4 -> FACEBOOK;
                case 5 -> INSTAGRAM;
                case 6 -> WHATSAPP;
                default -> throw ErrorCode.get(EntityConversationErrorCodes.INVALID_CONVERSATION_TYPE);
            };
        }

        /**
         * Gets the {@link Type} by its type
         *
         * @param type Represents the {@link Type} name
         * @return The {@link Type}
         */
        public static Type get(final String type) {
            if (Objects.nonNull(type)) {
                return switch (type.toLowerCase()) {
                    case "call" -> CALL;
                    case "email" -> EMAIL;
                    case "manuallyloggedcall" -> MANUALLY_LOGGED_CALL;
                    case "facebook" -> FACEBOOK;
                    case "instagram" -> INSTAGRAM;
                    case "whatsapp" -> WHATSAPP;
                    default -> throw ErrorCode.get(EntityConversationErrorCodes.INVALID_CONVERSATION_TYPE);
                };
            }
            throw ErrorCode.get(EntityConversationErrorCodes.INVALID_CONVERSATION_TYPE);
        }

        /**
         * <p>
         * Gets the collection of call types
         * </p>
         *
         * @return the collection of call types
         */
        public static Collection<Type> getCallTypes() {
            return List.of(CALL, MANUALLY_LOGGED_CALL);
        }

        /**
         * <p>
         * Checks whether the current value represents a call-related type.
         * </p>
         *
         * @return true if the enum value is call-related; false otherwise
         */
        public boolean isCallType() {
            return switch (this) {
                case CALL, MANUALLY_LOGGED_CALL -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Gets the collection of social types
         * </p>
         *
         * @return the collection of social types
         */
        public static Collection<Type> getSocialTypes() {
            return List.of(FACEBOOK, WHATSAPP, INSTAGRAM);
        }
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Type getType() {
        return type;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public abstract T getConversableModel();

    /**
     * <p>
     * Checks whether the conversation type is social or not
     * </p>
     *
     * @return true if it is social type, false otherwise
     */
    public boolean isSocialType() {
        return switch (type) {
            case FACEBOOK, WHATSAPP, INSTAGRAM -> true;
            default -> false;
        };
    }
}