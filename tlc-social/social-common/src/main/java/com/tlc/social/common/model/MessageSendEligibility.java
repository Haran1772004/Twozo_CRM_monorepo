package com.tlc.social.common.model;

/**
 * An enum which represents the eligibility of the outbound messaging. It defines whether an outbound message can be sent or not
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum MessageSendEligibility {

    /**
     * Outbound message can be sent
     */
    ELIGIBLE,

    /**
     * Outbound message cannot be sent. Page disabled explicitly
     */
    PAGE_DISABLED,

    /**
     * Outbound message cannot be sent. Page deleted explicitly
     */
    PAGE_DELETED,

    /**
     * Outbound message cannot be sent. Response not yet received
     */
    WAITING_FOR_RECEIVER_RESPONSE,

    /**
     * Outbound message cannot be sent. 24-hour message window has been passed
     */
    MESSAGE_WINDOW_EXPIRED
}