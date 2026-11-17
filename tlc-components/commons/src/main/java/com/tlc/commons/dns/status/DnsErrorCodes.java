package com.tlc.commons.dns.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Defines error codes specific to DNS operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum DnsErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_RECORD_TYPE(0x01),
    UNSUPPORTED_DOMAIN_STATUS(0x02),
    ;

    private final int code;

    /**
     * <p>
     * Constructs a DNS error code with the specified local code.
     * </p>
     *
     * @param localCode the local error code
     */
    DnsErrorCodes(final int localCode) {
        this.code = DnsErrorCodes.DnsErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * <p>
     * Returns the error code.
     * </p>
     *
     * @return the error code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Provides the error code group for DNS operations.
     * </p>
     */
    private static class DnsErrorCodeGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new DnsErrorCodes.DnsErrorCodeGroup();

        /**
         * <p>
         * Returns the prefix for the DNS error code group.
         * </p>
         *
         * @return the error code prefix
         */
        @Override
        public int getPrefix() {
            return 0x65_0_0001;
        }
    }
}