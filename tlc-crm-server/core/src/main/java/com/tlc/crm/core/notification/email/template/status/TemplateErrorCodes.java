package com.tlc.crm.core.notification.email.template.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 *     Represents error codes related to template of email
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum TemplateErrorCodes implements ErrorCodeProvider {

    TEMPLATE_NOT_FOUND(0x01),
    SOURCE_NOT_FOUND(0x02),
    TEMPLATE_FOLDER_NOT_MAPPED(0x03),
    TEMPLATE_FOLDER_LIMIT_EXCEEDED(0x04),
    TEMPLATE_ATTACHMENT_UPLOAD_ACTION_FAILED(0x05),
    ATTACHMENT_NOT_FOUND(0x06),
    INVALID_METRICS_TYPE(0x07),
    TEMPLATE_ID_NOT_FOUND(0x08),
    USER_RETRIEVAL_FAILED(0x09),
    TEMPLATE_DISPATCH_FAILED(0x10),
    FIELD_VALUE_RESOLVER_FAILED(0x11),
    ATTACHMENT_RETRIEVAL_FAILED(0x12),
    TEMPLATE_NAME_ALREADY_EXISTS(0x13)
    ;

    private final int code;

    TemplateErrorCodes(final int localCode) {
        this.code = TemplateErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class TemplateErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TemplateErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x17_0_0001;
        }
    }
}
