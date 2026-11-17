package com.tlc.analytics.config.internal;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Error codes related to config error codes.
 * </p>
 *
 * @author Ajay
 * @version 1.0.
 */
public enum ConfigErrorCodes implements ErrorCodeProvider {

    INVALID_GRAPH(0x01),
    ONLY_MULTIPLE_AGGREGATES_ARE_ALLOWED(0x02),
    COLOUR_AXIS_NOT_ALLOWED(0x03),
    X_AXIS_CANNOT_BE_AGGREGATE(0x04),
    INVALID_OPERATOR_TYPE(0X05),
    INVALID_COLUMN_CONFIGURATION(0X06),
    INVALID_URL_CONFIGURATION(0X07),
    INVALID_MODEL_IDS(0X08),
    INVALID_COLOR_PALETTE_TYPE(0X09),
    INVALID_DATA_TYPE(0X10),
    INVALID_JOIN_TYPE(0X11),
    INVALID_OPERATION_TYPE(0X12),
    INVALID_POSITION_TYPE(0X13),
    INVALID_VIEW_TYPE(0X14),
    INVALID_FILTER_TYPE(0X15),
    INVALID_FIELD_TYPE(0X16),
    INVALID_REPORT_TYPE(0X17),
    INVALID_AXIS_TYPE(0X18),
    INVALID_GRAPH_TYPE(0X19),
    INVALID_GRAPH_SUB_TYPE(0X20),
    INVALID_WIDGET_COMPARE_TYPE(0X21),
    INVALID_WIDGET_COMPARE_SIDE(0X22),
    INVALID_PERMISSION_TYPE(0X23),
    INVALID_CRITERIA_OPERATOR_TYPE(0X24),
    INVALID_ENUM_NAME( 0x25 ),
    INVALID_TIME_PERIOD( 0x26 ),
    INVALID_FUNCTION( 0x27 ),
    INVALID_COMPARE_UNIT( 0x28 ),
    TABLE_DETAILS_NOT_FOUND( 0x29 ),
    INVALID_JOIN_CONDITION( 0x30 ),
    GRAPH_CONTAIN_NEGATIVE_WEIGHT_CYCLE( 0x31 ),
    INVALID_VIEW_ID(0x32),
    INVALID_RESPONSE( 0x33),
    INVALID_CRITERIA_FILTER_TYPE( 0x34 ),
    UNDERLYING_DATA_RESPONSE_FAILED( 0x35 ),
    MODEL_NOT_FOUND( 0x36 );

    private final int code;

    ConfigErrorCodes( final int localCode) {
        this.code = ConfigErrorCodes.CommonErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class CommonErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new ConfigErrorCodes.CommonErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x30_0_0007;
        }
    }
}
