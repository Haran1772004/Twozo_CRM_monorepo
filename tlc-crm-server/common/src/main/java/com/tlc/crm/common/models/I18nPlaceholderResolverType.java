package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

/**
 * <p>
 * An enum contains the type of placeholder value resolvers
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum I18nPlaceholderResolverType {

    SOURCE;

    public static I18nPlaceholderResolverType get(final String prefix) {
        return switch (prefix) {
            case "crm.source." -> SOURCE;
            default -> throw ErrorCode.get(CommonErrorCodes.RESOLVER_TYPE_NOT_FOUND);
        };
    }
}