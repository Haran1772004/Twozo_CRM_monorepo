package com.tlc.crm.marketplace.app.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.marketplace.app.internal.status.CrmMarketplaceErrorCodes;

/**
 * Refers the enum of {@code ApplicationScope}.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum ApplicationScope {

    ORG_BASED((short) 1),
    USER_BASED((short) 2);

    final short id;

    ApplicationScope(final short id) {
        this.id = id;
    }

    public static ApplicationScope get(final short id) {
        return switch (id) {
            case 1 -> ORG_BASED;
            case 2 -> USER_BASED;
            default -> throw ErrorCode.get(CrmMarketplaceErrorCodes.INVALID_APPLICATIONS_SCOPE, "i18n.marketplace.application.scope.invalid");
        };
    }

    public short getId() {
        return id;
    }
}