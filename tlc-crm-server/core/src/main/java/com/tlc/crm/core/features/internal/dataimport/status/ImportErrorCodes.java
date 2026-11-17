package com.tlc.crm.core.features.internal.dataimport.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the error codes for data import feature
 *
 * @author Sundar
 * @version 1.0
 */
public enum ImportErrorCodes implements ErrorCodeProvider {

    ENTITY_NOT_FOUND(0x30),
    INVALID_IMPORT_DETAIL_STATUS(0X31),
    FILE_UPLOAD_FAILED(0X32),
    INVALID_DATE_OR_TIME_TYPE(0X33),
    IMPORT_ALREADY_FINISHED(0X34),
    IMPORT_NOT_FINISHED(0X35),
    IMPORT_ALREADY_REVERTED(0X36),
    READ_FAILED(0X37),
    INVALID_RESPONSE_TYPE(0x38),
    INVALID_REVERT_STATUS(0x39),
    REVERT_ACTION_FAILED(0x40),
    IMPORT_STOPPED(0x41),
    USER_RETRIEVAL_FAILED(0x42),
    NO_MAPPING_EXIST_BETWEEN_HEADERS_AND_FIELDS(0x43),
    SKIP_FILE_NOT_EXIST(0x44),
    FIELD_MAPPING_FAILED(0x45),
    SOURCE_DISABLED(0x46),
    INVALID_CONTACT_FIELD_TYPE_VALUE(0x47),
    INVALID_COMPANY_FIELD_TYPE_VALUE(0x48),
    INVALID_COUNTRY(0x49),
    INVALID_SOURCE(0x50),
    REVERT_CONTAINER_BUILD_FAILED(0x51),
    ;

    private final int code;

    ImportErrorCodes(int errorCode) {
        this.code = ImportErrorCodeGroup.GROUP.getConvertedCode(errorCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class ImportErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ImportErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0X31_0_0001;
        }
    }
}
