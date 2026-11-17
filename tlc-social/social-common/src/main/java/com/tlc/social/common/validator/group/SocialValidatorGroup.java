package com.tlc.social.common.validator.group;

/**
 * Represents a validator group for validating social related entities
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface SocialValidatorGroup {

    /**
     * Represents a validator group for outbound message creation
     */
    interface OutboundMessageCreate {
    }

    /**
     * Represents a validator group for inbound message creation
     */
    interface InboundMessageCreate {
    }

    /**
     * Represents a validator group for FB outbound message creation
     */
    interface FBOutboundMessageCreate {
    }

    /**
     * Represents a validator group for Whatsapp outbound message creation
     */
    interface WAOutboundMessageCreate {
    }

    /**
     * Represents a validator group for Instagram outbound message creation
     */
    interface InstagramOutboundMessageCreate {
    }

    /**
     * Represents a validator group for new chat creation
     */
    interface NewChatCreate {
    }
}
