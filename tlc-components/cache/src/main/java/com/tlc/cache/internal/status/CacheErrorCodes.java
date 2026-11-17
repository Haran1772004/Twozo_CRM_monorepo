package com.tlc.cache.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Manages the error codes associated with the cache module
 *
 * @author sundar
 * @version 1.0
 */
public enum CacheErrorCodes implements ErrorCodeProvider {

    ASYNC_EXECUTOR_REQUIRED(0x0001),
    ;

    private final int code;

    CacheErrorCodes(int localCode) {
        this.code = CacheErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class CacheErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new CacheErrorCodesGroup();

        /**
         * {@inheritDoc}
         *
         * @return
         */
        @Override
        public int getPrefix() {
            return 0x10_0_0006;
        }
    }
}

