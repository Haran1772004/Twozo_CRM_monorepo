package com.tlc.crm.common.export.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.export.field.DataExportFields;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;
import com.tlc.crm.common.queue.CommonQueueTopics;
import com.tlc.crm.common.service.ExportTrackerService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the async data export requests
 *
 * @author Selvakumar
 * @version 1.0
 */
@SourceAuthorizer(actions = Action.Type.EXPORT_RECORDS, needCommonApiHandling = true)
@WebAction(path = "/data/export/mgmt/async")
public class AsyncDataExportManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDataExportManagement.class);
    public static final String ALLOWED_EXPORT_FILE_PATTERN = "^(?i)(xls|xlsx|csv)$";

    private final ExportTrackerService exportTrackerService;
    private final DataExportManager dataExportManager;
    private final ProducerStore producerStore;

    public AsyncDataExportManagement() {
        this.exportTrackerService = ExportTrackerService.get();
        this.dataExportManager = DataExportManager.getInstance();
        this.producerStore = ProducerStore.getInstance();
    }

    /**
     * Handles the secured web request
     *
     * @param crmRequest  Holds the request parameters
     * @param crmResponse Represents the response parameters
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(DataExportFields.TYPE);

        switch (type) {
            case DataExportFields.INIT_EXPORT -> initExport(crmRequest, crmResponse);
            case DataExportFields.CHECK_EXPORT_STATUS -> checkExportStatus(crmRequest, crmResponse);
            default -> throw ErrorCode.get(DataExportErrorCodes.UNSUPPORTED_ACTION);
        }
    }

    /**
     * Initializes the export action for the given request
     *
     * @param crmRequest  Holds the request parameters
     * @param crmResponse Holds the response parameters
     */
    private void initExport(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(DataExportFields.DATA);
        final String exportFileType = data.optString(DataExportFields.FILE_TYPE, DataExportFields.DEFAULT_FILE_TYPE);
        final String exportName = data.getString(DataExportFields.EXPORT_NAME);
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();

        if (Objects.isNull(exportName) || exportName.isBlank()) {
            crmResponse.setErrorCode(ErrorCode.get(DataExportErrorCodes.INVALID_DATA_EXPORTER_NAME,
                    crmResponse.resolveI18nString("i18n.data.export.name.invalid")));
        } else if (exportFileType.matches(ALLOWED_EXPORT_FILE_PATTERN)) {
            final ExportTracker dataExport = new ExportTracker();

            dataExport.setUserId(userId);
            dataExport.setOrgId(orgId);
            dataExport.setExportStatus(ExportTracker.ExportStatus.NEW);
            dataExportManager.create(dataExport);
            final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%s:%s", dataExport.id(), exportName,
                    exportFileType, orgId, userId).getBytes());
            final SubscriptionTopic dataExporterTopic = CommonQueueTopics.getDataExporterTopic(QueueModel.Type.KAFKA);
            final Optional<Producer<byte[]>> dataExporterProducer = producerStore.getProducer(dataExporterTopic);

            dataExporterProducer.ifPresent(producer -> producer.send(new StreamMessage<>(requestId, dataExporterTopic, data.getBytes())));

            LOGGER.info("Data export: Export action request published, Request ID: {}", requestId);

            crmResponse.put(DataExportFields.ID, dataExport.id());
        } else {
            throw ErrorCode.get(DataExportErrorCodes.INVALID_DATA_EXPORTER_NAME, "i18n.data.export.name.invalid");
        }
    }

    /**
     * <p>
     * Checks the export status for an export identifier
     * </p>
     *
     * @param crmRequest  Holds the request parameters
     * @param crmResponse Holds the response parameters
     */
    private void checkExportStatus(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final long exportId = requestJson.getLong(DataExportFields.ID);
        final Long orgId = crmRequest.orgId();
        final Optional<ExportTracker> dataExportOptional = exportTrackerService.get(orgId, exportId);

        if (dataExportOptional.isPresent()) {
            final ExportTracker.ExportStatus exportStatus = dataExportOptional.get().getExportStatus();

            switch (exportStatus) {
                case NEW, EXPORTING, EXPORTED -> crmResponse.put(DataExportFields.MESSAGE, exportStatus.name());
                case EXPORT_FAILED -> {
                    crmResponse.put(DataExportFields.MESSAGE, exportStatus.name());
                    crmResponse.put(DataExportFields.ERROR, crmResponse.resolveI18nString("i18n.data.export.failed"));
                }
                default ->
                        crmResponse.setErrorCode(ErrorCode.get(DataExportErrorCodes.INVALID_EXPORT_STATUS, "i18n.data.export.invalid.status"));
            }
        } else {
            crmResponse.setErrorCode(ErrorCode.get(DataExportErrorCodes.INVALID_EXPORT_ID, "i18n.data.export.invalid.id"));
        }
    }
}
