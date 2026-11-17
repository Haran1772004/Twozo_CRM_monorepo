package com.tlc.crm.messenger.api.conversation.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.MessengerErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.social.common.model.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Pertains crm related fields to {@link Message}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrmMessage extends Message {

    private Long orgId;
    private Long crmId;
    private Long userId;
    private DateTime timeStamp;
    private OutboundMessageSource outboundMessageSource;

    /**
     * <p>
     * Represents the source of a outbound message
     * </p>
     */
    public enum OutboundMessageSource {

        MANUAL((short) 1),
        WORKFLOW((short) 2);

        private final short id;

        OutboundMessageSource(final short id) {
            this.id = id;
        }

        /**
         * Gets the id of the message source
         *
         * @return The id
         */
        public short getId() {
            return id;
        }

        /**
         * Gets the {@link OutboundMessageSource} by id
         *
         * @param id The id of the message source
         * @return The {@link OutboundMessageSource}
         */
        public static OutboundMessageSource getById(final short id) {
            return switch (id) {
                case 1 -> MANUAL;
                case 2 -> WORKFLOW;
                default -> throw ErrorCode.get(MessengerErrorCodes.INVALID_OUTBOUND_MESSAGE_SOURCE, "i18n.messenger.invalid.message.source");
            };
        }
    }
}