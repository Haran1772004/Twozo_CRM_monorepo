package com.tlc.crm.core.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the error codes of core module
 *
 * @author Sundar
 * @version 1.0
 */
public enum CoreErrorCodes implements ErrorCodeProvider {

    INVALID_CRM_FIELD_STORE_TYPE(0x30),
    INVALID_CRM_FIELD_STORE_SUBTYPE(0x59),
    UPDATE_ACTION_FAILED(0x60),
    ROLE_NOT_FOUND(0x61),
    TEAM_NOT_FOUND(0x62),
    CREATE_ACTION_FAILED(0x63),
    USER_NOT_FOUND(0x64),
    INVALID_STAGE_CATEGORY(0x65),
    INVALID_REPORTING_MANAGER(0x66),
    FAILED_TO_REGISTER_ACTIONS(0x67),
    USER_RETRIEVAL_FAILED(0x68),
    DESERIALIZATION_FAILED(0x69),
    SERIALIZATION_FAILED(0x70),
    INVALID_SOURCE(0x71),
    MODEL_VALUE_ACCESS_FAILED(0x72),
    PASSWORD_RESET_ACTION_FAILED(0x73),
    USER_INVITATION_LINK_EXPIRED(0x74),
    INVALID_USER_INVITATION(0X75),
    USER_INVITATION_NOT_FOUND(0x76),
    USER_ALREADY_ACTIVATED(0x77),
    INVALID_EMAIL(0x78),
    INVALID_TYPE(0x79),
    SYNC_ACTION_FAILED(0x80),
    INVALID_SOURCE_TYPE_ID(0x81),
    FAILED_TO_LOAD_CONFIG(0x82),
    INVALID_SYNC_MODE(0x83),
    PHONE_ALREADY_EXISTS(0x84),
    EMAIL_ALREADY_EXISTS(0x85),
    UNSUPPORTED_OPERATION(0x86),
    INVALID_FILTER_FIELD(0x87),
    CALL_LOG_NOT_FOUND(0x88),
    UNABLE_TO_PARSE_DATE(0x89),
    ENTITY_NOT_FOUND(0x90),
    DEALS_RETRIEVAL_ACTION_FAILED(0x91),
    DUPLICATE_QUOTA(0x92),
    QUOTA_CREATE_ACTION_FAILED(0x93),
    INVALID_QUOTA_TIME_PERIOD(0x94),
    INVALID_QUOTA_TIME_RANGE(0x95),
    INVALID_QUOTA_METRIC(0x96),
    INVALID_QUOTA_TYPE(0x97),
    FAILED_TO_RETRIEVE_DATA(0x98),
    FORECAST_WON_TYPE_NOT_FOUND(0x99),
    QUOTA_RECALCULATE_RESTRICTED(0X100),
    QUOTA_FIELD_NOT_FOUND(0x101),
    INVALID_IDENTIFIER_TYPE(0x102),
    INSIGHTS_DATA_LOAD_ACTION_FAILED(0x103),
    INVALID_INSIGHTS_GROUP(0x104),
    ;

    private final int code;

    CoreErrorCodes(int localCode) {
        this.code = CoreErrorCodes.CommonErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class CommonErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new CoreErrorCodes.CommonErrorCodesGroup();
        @Override
        public int getPrefix()
        {
            return 0x30_0_0005;
        }
    }
}
