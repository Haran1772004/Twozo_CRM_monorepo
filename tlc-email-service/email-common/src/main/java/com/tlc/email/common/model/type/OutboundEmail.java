package com.tlc.email.common.model.type;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * A model that represents the outbound email.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class OutboundEmail extends Email {

    private boolean isBounced;
    private OutboundType outboundType;

    public boolean isBounced() {
        return isBounced;
    }

    public void setBounced(final boolean isBounced) {
        this.isBounced = isBounced;
    }

    public OutboundType getOutboundType() {
        return Objects.isNull(outboundType) ? OutboundType.SEND : outboundType;
    }

    public void setOutboundType(final OutboundType outboundType) {
        this.outboundType = outboundType;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return MessageCategory.OUTBOUND;
    }

    /**
     * Refers to the {@link OutboundType} of mail.
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum OutboundType {

        SEND((short) 1),
        REPLY((short) 2),
        REPLY_ALL((short) 3),
        FORWARD((short) 4);

        private final short id;

        OutboundType(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the id of {@link OutboundType}
         * </p>
         *
         * @return the outbound type id
         */
        public short getId() {
            return this.id;
        }

        /**
         * Gets the value of {@link OutboundType} for the given id
         *
         * @param id Represents the id of {@link OutboundType}
         * @return {@link OutboundType}
         */
        public static OutboundType get(final short id) {
            return switch (id) {
                case 1 -> SEND;
                case 2 -> REPLY;
                case 3 -> REPLY_ALL;
                case 4 -> FORWARD;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_OUTBOUND_TYPE);
            };
        }

        public boolean isSendType() {
            return SEND == this;
        }
    }
}