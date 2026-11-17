package com.tlc.crm.messenger.api.channel.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Channel page states.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AllArgsConstructor
@Getter
public enum ChannelState {
    ENABLED((short) 1, "enabled"),
    DISABLED((short) 2, "disabled"),
    DELETED((short) 3, "deleted"),
    ;

    private final short id;
    private final String state;

    public static ChannelState getStateById(final short id) {
        return switch (id) {
            case 1 -> ENABLED;
            case 2 -> DISABLED;
            case 3 -> DELETED;
            default -> throw ErrorCode.get(CrmSocialErrorCodes.INVALID_PAGE_STATE, "i18n.facebook.state.not.found");
        };
    }
}