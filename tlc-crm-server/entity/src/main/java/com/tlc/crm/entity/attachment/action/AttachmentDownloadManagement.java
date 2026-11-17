package com.tlc.crm.entity.attachment.action;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages the {@link Attachment} download related web actions.
 *
 * @author karthick.V
 * @version 1.0
 */
@WebAction(path = "/file/download", method = HttpMethod.GET)
public class AttachmentDownloadManagement extends AuthorizationController {

    public AttachmentDownloadManagement() {
    }

    /**
     * {@inheritDoc}
     *
     * @param request     the received request
     * @param crmResponse the response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Stream<String> fileIdStream = request.getParam(AttachmentFields.FILE_ID.getName());
        final Long fileId = Objects.nonNull(fileIdStream) ? Long.valueOf(fileIdStream.findAny().get()) : null;

        downloadFile(fileId, crmResponse);
    }

    /**
     * Downloads file using fileId and filePath.
     *
     * @param fileId   It represents the file id.
     * @param response Response to be sent
     */
    private void downloadFile(final Long fileId, final CrmResponse response) {
        final String presignedUrl = AttachmentService.getInstance().getPresignedUrl(fileId, 30, null);

        response.setTemporaryRedirectUrl(presignedUrl);
        response.send();
    }
}
