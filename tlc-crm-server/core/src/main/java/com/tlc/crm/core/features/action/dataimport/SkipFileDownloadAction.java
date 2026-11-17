package com.tlc.crm.core.features.action.dataimport;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages the skip file download related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/import/skipfile/download", method = HttpMethod.GET)
public class SkipFileDownloadAction extends AuthorizationController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String RESPONSE_CONTENT_DISPOSITION_KEY = "response-content-disposition";
    private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s\"";
    private static final String SKIP_FILE_NAME_FORMAT = "skip_file_%s.xlsx";

    public SkipFileDownloadAction() {
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Refers the {@link CrmRequest}.
     * @param crmResponse Refers the {@link CrmResponse} to be sent.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Stream<String> idStream = request.getParam(ImportFields.ID.getName());
        final String importId = Objects.nonNull(idStream) ? idStream.findAny().get() : null;

        downloadSkipFile(request, Long.parseLong(importId), crmResponse);
    }

    /**
     * Downloads the skipped file.
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param importId Refers the skip file identifier.
     * @param response The response to be sent.
     */
    private void downloadSkipFile(final CrmRequest request, final Long importId, final CrmResponse response) {
        if (Objects.nonNull(importId)) {
            final Long orgId = request.orgId();
            final Long skipFileId = ImportManager.getInstance().getSkipFileId(orgId, importId);

            if (Objects.nonNull(skipFileId)) {
                final String formattedDateTime = TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.now(), request.timeZone().getTimeZone().toZoneId()));
                final String fileName = String.format(SKIP_FILE_NAME_FORMAT, formattedDateTime);
                final String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                final String encodedContentDisposition = String.format(CONTENT_DISPOSITION_FORMAT, encodedFileName);
                final String presignedUrl = AttachmentService.getInstance().getPresignedUrl(skipFileId, 1,
                        Map.of(RESPONSE_CONTENT_DISPOSITION_KEY, encodedContentDisposition));

                response.setTemporaryRedirectUrl(presignedUrl);
                response.send();
            } else {
                response.setErrorCode(ErrorCode.get(ImportErrorCodes.SKIP_FILE_NOT_EXIST, "i18n.import.skipFile.not.exist"));
                response.send();
            }
        } else {
            throw ErrorCode.get(ImportErrorCodes.INVALID_RESPONSE_TYPE, "i18n.skipFile.download.invalid.response");
        }
    }
}