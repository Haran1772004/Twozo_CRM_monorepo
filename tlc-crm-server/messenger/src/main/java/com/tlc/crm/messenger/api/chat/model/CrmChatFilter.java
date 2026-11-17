package com.tlc.crm.messenger.api.chat.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.MessengerErrorCodes;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An enum which contains chat filters related to the crm
 *
 * @author Maharaja S
 * @version 1.0
 */
@AllArgsConstructor
@Getter
public enum CrmChatFilter {

    ALL((short) 1, "all"),
    ASSIGNED_TO_ME((short) 2, "assignedToMe"),
    UNASSIGNED((short) 3, "unassigned"),
    UNREAD((short) 4, "unread"),
    CONTACTS((short) 5, "contacts"),
    NON_CONTACTS((short) 6, "nonContacts");

    private final short id;
    private final String name;

    /**
     * <p>
     * Gets the {@link CrmChatFilter} by the provided id
     * </p>
     *
     * @param id Represents the id of the enum
     * @return the {@link CrmChatFilter}
     */
    public static CrmChatFilter getFilterById(final Short id) {
        return switch (id) {
            case 1 -> ALL;
            case 2 -> ASSIGNED_TO_ME;
            case 3 -> UNASSIGNED;
            case 4 -> UNREAD;
            case 5 -> CONTACTS;
            case 6 -> NON_CONTACTS;
            default -> throw ErrorCode.get(MessengerErrorCodes.INVALID_CHAT_FILTER, "i18n.messenger.chat.invalid.filter");
        };
    }
}
