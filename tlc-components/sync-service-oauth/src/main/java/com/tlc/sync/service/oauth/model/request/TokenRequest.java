package com.tlc.sync.service.oauth.model.request;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Represents a top level token request
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TokenRequest {

    private final SyncServiceCategory syncServiceCategory;
    private final TokenType tokenType;

    @AllArgsConstructor
    @Getter
    public enum TokenType {

        ACCESS((short) 1),
        REFRESH((short) 2);

        private final short id;

        public static TokenType get(final short id) {
            return switch (id) {
                case 1 -> ACCESS;
                case 2 -> REFRESH;
                default -> throw ErrorCode.get(SyncErrorCodes.INVALID_TOKEN_TYPE, "i18n.sync.service.invalid.token.type");
            };
        }
    }
}