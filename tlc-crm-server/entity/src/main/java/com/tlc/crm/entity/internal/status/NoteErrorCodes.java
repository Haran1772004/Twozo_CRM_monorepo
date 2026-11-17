package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of crm note module
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum NoteErrorCodes implements ErrorCodeProvider {

    ENTITY_NOT_FOUND(0x20),
    NOTE_NOT_FOUND(0x21),
    SOURCE_NOT_FOUND(0x22),
    USER_RETRIEVAL_FAILED(0x23),
    COMMENT_NOT_FOUND(0x24),
    COMMENT_VALIDATION_FAILED(0x25),
    ;

    private final int code;

    NoteErrorCodes(final int localCode) {
        this.code = NoteErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class NoteErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new NoteErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x30_0_0001;
        }
    }
}
