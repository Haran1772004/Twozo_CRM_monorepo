package com.tlc.crm.entity.features.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Contains the tag related error codes
 *
 * @author EswariNivethaVU
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum TagErrorCodes implements ErrorCodeProvider {

    TAG_NOT_FOUND(0x21),
    TAG_NAME_ALREADY_EXISTS(0x24),
    DELETE_ACTION_FAILED(0x25),
    TAG_MAPPING_FAILED(0x26),
    ENTITY_IDS_REQUIRED(0x28),
    SOURCE_NOT_FOUND(0X29),
    ENTITY_IDS_NOT_FOUND(0X30),
    USER_RETRIEVAL_FAILED(0x31),
    INVALID_SOURCE(0x32),
    INVALID_TAG_COLOR(0x33);
    ;

    private final int code;

    TagErrorCodes(int localCode) {
        this.code = TagErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class TagErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TagErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x30_0_0001;
        }
    }
}