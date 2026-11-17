package com.tlc.social.common.event;

/**
 * Represents the common social events which is used in event callback
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CommonSocialEvents {

    public static final String RETRIEVE_MESSAGES = "social.chat.messages.retrieval.event";
    public static final String RETRIEVE_MESSAGE_TIMESTAMPS = "social.chat.message.timestamps.retrieval.event";
    public static final String RETRIEVE_CHATS = "social.chats.retrieval.event";
    public static final String SEND_OUTBOUND_MESSAGE = "social.outbound.message.event";
    public static final String SEND_READ_RECEIPT = "social.read.receipt.send.event";
    public static final String RETRIEVE_PLATFORM_AND_RECIPIENT_PROFILE = "social.recipient.profile.and.platform.retrieval.event";
    public static final String RETRIEVE_CHANNEL = "social.channel.retrieval.event";
}
