package com.tlc.crm.messenger.api.conversation.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.MessengerErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * Pertains to conversation activity's template contents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConversationActivityTemplate implements Serializable {

    private String chatOwnerTemplate;
    private String otherUserTemplate;
    private Type type;

    /**
     * <p>
     * Template related types.
     * </p>
     *
     * @author Ajay.
     *  @version 1.0.
     */
    @AllArgsConstructor
    @Getter
    public enum Type implements Serializable {
        USER_ASSIGNMENT((short) 1),
        USER_REASSIGNMENT((short) 2),
        USER_UNASSIGNMENT((short) 3),
        ;
        private final short id;

        /**
         * <p>
         * Gets the {@link Type} based on the provided id.
         * </p>
         *
         * @param id Represents the enum id.
         * @return the {@link Type}.
         */
        public static Type get(final short id) {
            return switch (id) {
                case 1 -> USER_ASSIGNMENT;
                case 2 -> USER_REASSIGNMENT;
                case 3 -> USER_UNASSIGNMENT;
                default ->
                        throw ErrorCode.get(MessengerErrorCodes.INVALID_CONVERSATION_TYPE, "i18n.messenger.chat.invalid.conversation.type");
            };
        }
    }
}