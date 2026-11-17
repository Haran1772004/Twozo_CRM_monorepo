package com.tlc.sql.common.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

@Getter
public enum DatabaseType {

    OLTP(1),
    OLAP(2);

    private final int id;

    DatabaseType(final int id) {
        this.id = id;
    }

    public static DatabaseType get(final int id) {
        return switch (id) {
            case 1 -> OLTP;
            case 2 -> OLAP;
            default -> throw ErrorCode.get(SQLErrorCodes.INVALID_DATABASE_TYPE);
        };
    }
}