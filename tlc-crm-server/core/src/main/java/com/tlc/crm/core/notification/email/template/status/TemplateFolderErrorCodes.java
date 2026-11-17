package com.tlc.crm.core.notification.email.template.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;

/**
 * <p>
 * Provides the error codes of {@link TemplateFolder}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TemplateFolderErrorCodes implements ErrorCodeProvider {

    TEMPLATE_FOLDER_NAME_ALREADY_EXISTS(0x1),
    TEMPLATE_FOLDER_NOT_FOUND(0x2),
    INVALID_ACCESS_TYPE(0x3),
    TEMPLATES_EXISTS_IN_FOLDER(0x4),
    INVALID_FILTER_TYPE(0x5),
    USERS_NOT_FOUND(0x6),
    TEAMS_NOT_FOUND(0x7),
    TERRITORIES_NOT_FOUND(0x8),
    INVALID_VISIBILITY_TYPE(0x9),
    PERMISSION_DENIED(0x10),
    VISIBILITY_TYPE_SHOULD_NOT_BE_NULL(0x11),
    ACCESS_TYPE_SHOULD_NOT_BE_NULL(0x12),
    ;

    private final int code;

    TemplateFolderErrorCodes(final int localCode) {
        this.code = TemplateFolderErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class TemplateFolderErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TemplateFolderErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x18_0_0001;
        }
    }

}
