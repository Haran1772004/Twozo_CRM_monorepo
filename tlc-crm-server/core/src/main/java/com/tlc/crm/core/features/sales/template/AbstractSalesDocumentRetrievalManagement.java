package com.tlc.crm.core.features.sales.template;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.web.WebExchange;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tlc.attachment.storage.model.PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION;

/**
 * <p>
 * Base action for public access to sales document retrieval files.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentRetrievalManagement extends CrmPublicAction {

    private static final String CACHE_CONTROL_VALUE = "private, max-age=3600";
    private static final String RESPONSE_CONTENT_DISPOSITION_FOR_PREVIEW = "inline; filename=%s";
    private static final String RESPONSE_CONTENT_DISPOSITION_FOR_DOWNLOAD = "attachment; filename=%s";

    protected final AttachmentService attachmentService;

    protected AbstractSalesDocumentRetrievalManagement() {
        this.attachmentService = AttachmentService.getInstance();
    }

    /**
     * <p>
     * Processes the sales document retrieval request and redirects the client
     * to the generated attachment URL.
     * </p>
     *
     * @param request  Incoming request containing the encoded document identifier.
     * @param response Response object used to redirect the client to the attachment URL.
     * @throws Exception if any error occurs while processing the request.
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final String encodedId = request.getParameter(getIdParameter());
        final String actionType = request.getParameter(getActionParameter());

        if (Objects.isNull(encodedId) || Objects.isNull(actionType)) {
            throw getAttachmentNotFoundError();
        }
        final String decodedId = new String(Base64.getDecoder().decode(encodedId), StandardCharsets.UTF_8);
        final String[] documentIds = StringUtil.splitAndTrim(decodedId, StringUtil.Delimiter.FORWARD_SLASH);
        final long orgId = NumberUtils.parseNumber(documentIds[0]).longValue();
        final long userId = NumberUtils.parseNumber(documentIds[1]).longValue();
        final long documentId = NumberUtils.parseNumber(documentIds[2]).longValue();

        SecureTaskHandler.getInstance().secureProcess(orgId, userId, () -> processDocument(orgId, documentId, actionType, response));
    }

    /**
     * <p>
     * Processes the document-specific retrieval logic.
     * </p>
     *
     * @param orgId      Organization identifier.
     * @param documentId Sales document identifier.
     * @param actionType Requested action type.
     * @param response   Response object used to redirect the client.
     */
    protected abstract void processDocument(long orgId, long documentId, String actionType, CrmResponse response);

    /**
     * <p>
     * Returns the request parameter used to identify the document.
     * </p>
     *
     * @return document identifier parameter.
     */
    protected abstract String getIdParameter();

    /**
     * <p>
     * Returns the request parameter used to identify the requested action.
     * </p>
     *
     * @return action parameter.
     */
    protected abstract String getActionParameter();

    /**
     * <p>
     * Redirects the response to the specified attachment.
     * </p>
     *
     * @param attachmentId     Attachment identifier.
     * @param isDownloadAction Whether the request is for download.
     * @param response         Response object.
     */
    protected void handleRedirection(final Long attachmentId, final boolean isDownloadAction, final CrmResponse response) {
        if (!attachmentService.exists(attachmentId)) {
            throw getAttachmentNotFoundError();
        }

        attachmentService.getAttachments(List.of(attachmentId))
                .stream()
                .findFirst()
                .ifPresentOrElse(attachment -> {
                    final String responseContentDisposition = String.format(
                            isDownloadAction ? RESPONSE_CONTENT_DISPOSITION_FOR_DOWNLOAD : RESPONSE_CONTENT_DISPOSITION_FOR_PREVIEW,
                            attachment.getFileName()
                    );

                    final String presignedUrl = attachmentService.getPresignedUrl(attachmentId, NumericalConstant.TEN,
                            Map.of(
                                    RESPONSE_CONTENT_DISPOSITION,
                                    responseContentDisposition,
                                    PreSignedUrlHeader.RESPONSE_CONTENT_TYPE,
                                    Extension.get(attachment.getExtension()).getMimeType()
                            )
                    );

                    response.setHeader(WebExchange.Header.CACHE_CONTROL.getName(), CACHE_CONTROL_VALUE);
                    response.setTemporaryRedirectUrl(presignedUrl);
                    response.send();
                }, () -> {
                    throw getAttachmentNotFoundError();
                });
    }

    /**
     * <p>
     * Returns the attachment not found error.
     * </p>
     *
     * @return attachment not found error.
     */
    private static ErrorCode getAttachmentNotFoundError() {
        return ErrorCode.get(FileErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.entity.attachment.not.found");
    }

    /**
     * <p>
     * To ensure secure processing of user tasks. It manages the execution of tasks in a secure context.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     * @see SecureUserTaskProcessor
     */
    private static class SecureTaskHandler implements SecureUserTaskProcessor {

        private SecureTaskHandler() {
        }

        private static class Instance {
            private static final SecureTaskHandler INSTANCE = new SecureTaskHandler();
        }

        /**
         * <p>
         * Gets the {@link SecureTaskHandler} Instance
         * </p>
         *
         * @return {@link SecureTaskHandler}
         */
        public static SecureTaskHandler getInstance() {
            return SecureTaskHandler.Instance.INSTANCE;
        }
    }
}