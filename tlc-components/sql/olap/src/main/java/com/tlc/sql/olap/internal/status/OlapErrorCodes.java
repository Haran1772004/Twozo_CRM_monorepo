package com.tlc.sql.olap.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

public enum OlapErrorCodes  implements ErrorCodeProvider {

    DB_OLAP_ORDER_BY_REQUIRED(0x113),
    DB_OLAP_ORDER_BY_LIMIT_EXCEED(0x114),
    DB_OLAP_PARTITION_BY_LIMIT_EXCEED(0x115),
    DB_OLAP_PARTITION_BY_REQUIRED(0x116),
    DB_OLAP_INVALID_SKIP_INDEXES_CONFIGURATION(0x117),
    DB_OLAP_SKIP_INDEX_LIMIT_EXCEED(0x118),
    DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED(0x119),
    DB_OLAP_SKIP_INDEX_INVALID_FALSE_POSITIVE_PROBABILITY(0x120),
    DB_OLAP_INVALID_SKIP_INDEX(0x121),
    DB_OLAP_AGGREGATE_FUNCTION_REQUIRED(0x122),
    DB_OLAP_PARAMETER_TYPES_REQUIRED(0x123),
    DB_OLAP_PK_LIMIT_EXCEED(0x124),
    DB_OLAP_INVALID_SKIP_INDEX_NAME_PATTERN(0x125),
    DB_OLAP_PARTITION_BY_ATTRIBUTE_REQUIRED(0x126),
    DB_OLAP_FUNCTION_NOT_FOUND(0x127),
    DB_OLAP_INVALID_PARTITION_BY_CONFIGURATION(0X128);

    private final int code;
    OlapErrorCodes(int localCode)
    {
        this.code = OlapErrorCodes.OlapErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class OlapErrorCodeGroup implements ErrorCodeGroup
    {
        private static final ErrorCodeGroup GROUP = new OlapErrorCodes.OlapErrorCodeGroup();
        @Override
        public int getPrefix()
        {
            return 0x11_0_0000;
        }
    }
}
