package com.tlc.crm.messenger.validator.group;

/**
 * Represents a validator group for validating messenger related entities
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface MessengerValidatorGroup {

    /**
     * Represents a validator group for assigning user to chat
     */
    interface ChatUserAssignment {
    }

    /**
     * Represents a validator group for unassigning user from chat
     */
    interface ChatUserUnAssignment {
    }
}