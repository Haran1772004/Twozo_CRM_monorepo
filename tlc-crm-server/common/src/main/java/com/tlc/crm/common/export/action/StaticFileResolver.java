package com.tlc.crm.common.export.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.export.field.DataExportFields;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves the file download requests
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/static/file/download/mgmt", method = HttpMethod.GET)
public class StaticFileResolver extends AuthorizationController {

    private final DataExportManager dataExportManager;

    public StaticFileResolver() {
        this.dataExportManager = DataExportManager.getInstance();
    }

    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String idAsString = request.getParameter(DataExportFields.ID);

        if (Objects.nonNull(idAsString)) {
            final Long id = Long.parseLong(idAsString);
            final Stream<String> fileSourceStream = request.getParam(DataExportFields.SOURCE);
            final Long orgId = request.orgId();
            final FileSource fileSource;

            try {
                final String fileSourceName = fileSourceStream.findAny().orElseGet(() -> null);
                fileSource = Objects.nonNull(fileSourceName) ? FileSource.valueOf(fileSourceName.toUpperCase()) : FileSource.DEFAULT;
            } catch (final IllegalArgumentException exception) {
                throw ErrorCode.get(CommonErrorCodes.INVALID_FILE_SOURCE);
            }

            switch (fileSource) {
                case EXPORT -> downloadExportedData(orgId, id, crmResponse);
                case DEFAULT -> downloadStaticFileData(id, crmResponse);
                default -> throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
            }
        }
    }

    /**
     * Represents the source of the file
     */
    private enum FileSource {
        EXPORT,
        DEFAULT
    }

    /**
     * Downloads the exported file
     *
     * @param orgId       Refers the organization id.
     * @param exportId    Refers the export Id for which the file to be downloaded.
     * @param crmResponse Refers the {@code CrmResponse} to be sent.
     */
    private void downloadExportedData(final Long orgId, final Long exportId, final CrmResponse crmResponse) {
        final Optional<ExportTracker> optionalExportStatusInfo = dataExportManager.getDataExport(orgId, exportId);

        optionalExportStatusInfo.ifPresentOrElse(exportStatusInfo -> {
            final Long exportFileId = exportStatusInfo.getExportFileId();

            if (Objects.isNull(exportFileId)) {
                crmResponse.setErrorCode(ErrorCode.get(DataExportErrorCodes.EXPORT_FILE_NOT_FOUND));
                crmResponse.put(DataExportFields.ERROR, crmResponse.resolveI18nString("i18n.export.file.not.found"));
            } else {
                final String presignedUrl = AttachmentService.getInstance().getPresignedUrl(exportStatusInfo.getExportFileId(), 30, null);

                crmResponse.setTemporaryRedirectUrl(presignedUrl);
            }
            crmResponse.send();
        }, () -> {
            crmResponse.setErrorCode(ErrorCode.get(DataExportErrorCodes.INVALID_EXPORT_STATUS));
            crmResponse.put(DataExportFields.ERROR, crmResponse.resolveI18nString("i18n.data.export.invalid.status"));
            crmResponse.send();
        });
    }

    /**
     * Downloads the static file
     *
     * @param fileId      ID of the file to be downloaded
     * @param crmResponse Holds the response parameters
     */
    private void downloadStaticFileData(final Long fileId, final CrmResponse crmResponse) {
        //TODO If file detail stored on db and exists on the path, download it otherwise throw an error
    }
}
