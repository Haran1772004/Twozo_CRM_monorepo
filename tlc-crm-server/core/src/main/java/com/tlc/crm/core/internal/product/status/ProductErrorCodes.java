package com.tlc.crm.core.internal.product.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 *
 */
public enum ProductErrorCodes implements ErrorCodeProvider {

    PRODUCT_NOT_FOUND(0x20),
    PRICE_TAG_NOT_FOUND(0x21),
    CURRENCY_NOT_FOUND(0x22),
    CATEGORY_NOT_FOUND(0x23),
    ENTITY_NOT_FOUND(0x24),
    CATEGORY_ALREADY_EXISTS(0X25),
    VARIATION_ALREADY_EXISTS(0x26),
    VARIATION_NOT_FOUND(0x27),
    INVALID_DATA(0x28),
    PRODUCT_VARIATION_IS_DISABLED(0x39),
    PRODUCT_IS_DISABLED(0x30),
    PRICE_TAG_DELETION_RESTRICTED(0x29),
    INVALID_PRODUCT_TYPE(0x30),
    INVALID_STATUS(0x31),
    PRICE_TAG_VARIATION_MISMATCH(0x32),
    UNSUPPORTED_CURRENCY(0x33),
    ;

    private final int code;

    ProductErrorCodes(int localCode) {
        this.code = ProductErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class ProductErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ProductErrorCodeGroup();
        @Override
        public int getPrefix()
        {
            return 0x20_0_0001;
        }
    }

}
