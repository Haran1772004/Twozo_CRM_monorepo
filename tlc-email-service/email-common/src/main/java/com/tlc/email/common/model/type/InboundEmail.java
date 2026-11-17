package com.tlc.email.common.model.type;

/**
 * <p>
 * A model that represents the inbound email.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class InboundEmail extends Email {

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return MessageCategory.INBOUND;
    }
}