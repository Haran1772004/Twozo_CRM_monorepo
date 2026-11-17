package com.tlc.crm.webform.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Holds the {@link com.tlc.crm.webform.api.model.WebForm} related error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WebFormErrorCodes implements ErrorCodeProvider {

    WEB_FORM_CREATION_FAILED(0X21),
    WEB_FORM_UPDATE_ACTION_FAILED(0x22),
    WEB_FORM_NOT_FOUND(0x23),
    WEB_FORM_EMAIL_NOT_FOUND(0x24),
    TEMPLATE_NOT_FOUND(0x25),
    FORM_STYLE_NOT_FOUND(0x26),
    FIELD_STYLE_NOT_FOUND(0X27),
    SUBMIT_BUTTON_STYLE_NOT_FOUND(0X28),
    FORM_FIELD_NOT_FOUND(0x29),
    FORM_ELEMENT_NOT_FOUND(0x30),
    INVALID_FIELD_PROPERTY_TYPE(0x31),
    SUBMIT_ACTION_DETAIL_NOT_FOUND(0x32),
    FORM_REQUIRED_FIELD_DATA_IS_MISSING(0x33),
    INVALID_ALIGNMENT(0x34),
    INVALID_FONT(0x35),
    SORTING_NOT_ALLOWED(0x36),
    WEB_FORM_IS_INACTIVE(0x37),
    FIELD_IS_NOT_A_DROPDOWN(0x38),
    WEB_FORM_NAME_LENGTH_EXCEEDED(0x39),
    UNSUPPORTED_OPERATION(0x40),
    IMAGE_ATTACHMENT_NOT_FOUND(0x41),
    MULTIPLE_IMAGES_NOT_ALLOWED(0x42),
    INVALID_IMAGE_ELEMENT_LABEL(0x43),
    INVALID_IMAGE_ELEMENT_CONFIG(0x44),
    ;

    private final int code;

    WebFormErrorCodes(final int localCode) {
        this.code = WebFormErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class WebFormErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new WebFormErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}

