package com.tlc.crm.entity.attachment.action;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.status.FileErrorCodes;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the attachment preview web action
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/file/preview/{fileId}", method = HttpMethod.GET)
public class AttachmentPreviewAction extends AuthorizationController {

    private final AttachmentService attachmentService;

    public AttachmentPreviewAction() {
        attachmentService = AttachmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the  request to be handled
     * @param response Represents the response
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) {
        final String attachmentIdAsString = request.getParam(AttachmentFields.FILE_ID.getName()).findFirst().orElse(null);

        if (Objects.nonNull(attachmentIdAsString)) {
            final long attachmentId = NumberUtils.parseNumber(attachmentIdAsString).longValue();

            if (!attachmentService.exists(attachmentId)) {
                throw ErrorCode.get(FileErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.entity.attachment.not.found");
            }

            attachmentService.getAttachments(List.of(attachmentId)).stream().findFirst().ifPresentOrElse(attachment -> {
                final String presignedUrl = attachmentService.getPresignedUrl(attachmentId, 10,
                        Map.of(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION, String.format("inline; filename=%s", attachment.getFileName()),
                                PreSignedUrlHeader.RESPONSE_CONTENT_TYPE, Extension.get(attachment.getExtension()).getMimeType()));

                response.setHeader(WebExchange.Header.CACHE_CONTROL.getName(), "private, max-age=3600");
                response.setTemporaryRedirectUrl(presignedUrl);
                response.send();
            }, () -> {
                throw ErrorCode.get(FileErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.entity.attachment.not.found");
            });
        }
    }
}