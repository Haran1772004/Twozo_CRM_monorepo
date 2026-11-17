package com.tlc.crm.core.features.quote.action;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the quote company logo preview web action
 *
 * @author Sathish S
 * @version 1.0
 */
@WebAction(path = "/quote/image/preview/{data}", method = HttpMethod.GET)
public class QuoteImagePreviewAction extends CrmPublicAction implements SecureUserTaskProcessor {

    private final QuotationManager quotationManager;
    private final AttachmentService attachmentService;

    public QuoteImagePreviewAction() {
        quotationManager = QuotationManager.getInstance();
        attachmentService = AttachmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the  request to be handled
     * @param response Represents the response
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = Json.object(Base64.getDecoder().decode(request.getParam(QuotationFields.DATA).findFirst().orElse("{}")));
        final Long orgId = data.getNullableValue(QuotationFields.ORG_ID);
        final Long userId = data.getNullableValue(QuotationFields.USER_ID);
        final Long quoteId = data.getNullableValue(QuotationFields.QUOTE_ID);
        final Long imageId = data.getNullableValue(QuotationFields.IMAGE_ID);
        final String field = data.getString(QuotationFields.FIELD);

        if (Objects.nonNull(orgId) && Objects.nonNull(quoteId) && Objects.nonNull(imageId) && Objects.nonNull(field)) {

            secureProcess(orgId, userId, () -> {
                if (quotationManager.exists(orgId, quoteId)) {

                    if (!attachmentService.exists(imageId)) {
                        throw ErrorCode.get(FileErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.entity.attachment.not.found");
                    }

                    attachmentService.getAttachments(List.of(imageId)).stream().findFirst().ifPresentOrElse(attachment -> {
                        final String presignedUrl = attachmentService.getPresignedUrl(imageId, 10,
                                Map.of(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION, String.format("inline; filename=%s", attachment.getFileName()),
                                        PreSignedUrlHeader.RESPONSE_CONTENT_TYPE, Extension.get(attachment.getExtension()).getMimeType()));

                        response.setHeader(WebExchange.Header.CACHE_CONTROL.getName(), "private, max-age=3600");
                        response.setTemporaryRedirectUrl(presignedUrl);
                        response.send();
                    }, () -> {
                        throw ErrorCode.get(FileErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.entity.attachment.not.found");
                    });
                } else {
                    throw ErrorCode.get(QuotationErrorCodes.QUOTATION_NOT_FOUND, "i18n.quotation.not.found");
                }
            });
        }
    }
}