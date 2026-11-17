package com.tlc.crm.core.notification.email.template.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;

/**
 * <p>
 * Provides the error codes of {@link TemplateTag}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TemplateTagErrorCodes implements ErrorCodeProvider {

    TEMPLATE_TAG_NAME_ALREADY_EXISTS(0x1),
    TEMPLATE_TAG_NOT_FOUND(0x2),
    USER_RETRIEVAL_FAILED(0x3),
    TEMPLATE_TAG_LIMIT_EXCEEDED(0x4),
    ;

    private final int code;

    TemplateTagErrorCodes(int localCode) {
        this.code = TemplateTagErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class TemplateTagErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TemplateTagErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x19_0_0001;
        }
    }

}
