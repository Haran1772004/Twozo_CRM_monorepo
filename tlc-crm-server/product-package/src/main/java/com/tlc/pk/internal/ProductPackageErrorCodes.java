package com.tlc.pk.internal;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to product package
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public enum ProductPackageErrorCodes implements ErrorCodeProvider
{
    EXPORT_ACTION_FAILED( 0x01),
    FILE_WRITE_ACTION_FAILED( 0x02 );

    private final int code;

    ProductPackageErrorCodes(final int localCode )
    {
        this.code = CommonErrorCodesGroup.GROUP.getConvertedCode( localCode );
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class CommonErrorCodesGroup implements ErrorCodeGroup
    {
        private static final ErrorCodeGroup GROUP = new CommonErrorCodesGroup();

        @Override
        public int getPrefix()
        {
            return 0x45_0_0001;
        }
    }
}
