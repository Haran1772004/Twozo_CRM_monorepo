package com.tlc.crm.core.features.action.dataimport;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.RevertRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.handler.ImportDataHandler;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Manages data revert related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/revert/mgmt")
public class RevertAction extends AuthorizationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevertAction.class);
    private final RevertRequestStatusHolder revertRequestStatusHolder;
    private final ImportManager importManager;
    private final ImportDataHandler importDataHandler;
    private final QueueService queueAccess;

    public RevertAction() {
        this.revertRequestStatusHolder = RevertRequestStatusHolder.getInstance();
        this.queueAccess = QueueServiceProvider.get();
        this.importManager = ImportManager.getInstance();
        this.importDataHandler = ImportDataHandler.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Refers the {@link CrmRequest}.
     * @param crmResponse Refers the response to be sent.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");

        switch (type) {
            case "revert" -> initRevert(request, data, crmResponse);
            case "checkRevertStatus" -> checkRevertStatus(data, crmResponse);
        }
    }

    /**
     * Initiates the data revert process.
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param data     Refers the request data.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    private void initRevert(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final Long importId = data.getLong(ImportFields.IMPORT_ID.getName());
        final Long orgId = request.orgId();
        final ImportInfo importInfo = importManager.get(orgId, importId);

        switch (importInfo.getStatus()) {

            case PENDING, IN_PROGRESS -> {
                LOGGER.info("Data import: Import is not finished yet. Import ID: {}, Org ID: {}", importId, orgId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_NOT_FINISHED, "i18n.import.not.finished");
            }
            case REVERTED -> {
                LOGGER.info("Data import: Import is already reverted. Import ID: {}, Org ID: {}", importId, orgId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_ALREADY_REVERTED, "i18n.import.already.reverted");
            }
            case STOPPED -> {
                LOGGER.info("Data import: Import is stopped. Org ID: {}, Import ID: {}", orgId, importId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_STOPPED, "i18n.import.stopped");
            }
        }

        final String requestId = importDataHandler.generateImportRequestId(orgId, request.userId(), importId);
        LOGGER.info("Data import: Revert process is going to start. Import ID: {}, Org ID: {}", importId, orgId);

        if (revertRequestStatusHolder.isRevertInProgress(requestId)) {
            response.put(ImportFields.IS_IN_PROGRESS.getName(), true);
            response.put(ImportFields.IS_DONE.getName(), false);
        } else {
            final JsonObject revertRequestJson = Json.object(data.getBytes());
            final SubscriptionTopic dataRevertTopic = CoreQueueTopics.getDataRevertTopic(QueueModel.Type.KAFKA);
            final KafkaProducerConfig dataRevertProducerConfig = new KafkaProducerConfig(dataRevertTopic);

            revertRequestJson.put(ImportFields.LOCALE.getName(), request.locale().toLanguageTag());

            try (final Producer<byte[]> dataRevertProducer = queueAccess.createProducer(dataRevertProducerConfig)) {
                revertRequestStatusHolder.setRevertStatus(requestId, RevertRequestStatusHolder.RevertStatus.NEW);
                dataRevertProducer.send(new StreamMessage<>(requestId, dataRevertTopic, revertRequestJson.getBytes()));
            }
            LOGGER.info("Data import: Revert request is published. Import ID: {}, Org ID: {}", importId, orgId);
        }
        response.put(ImportFields.ID.getName(), requestId);
    }

    /**
     * Checks the revert status.
     *
     * @param data     Refers the request json.
     * @param response The response to be sent.
     */
    private void checkRevertStatus(final JsonObject data, final CrmResponse response) {
        final RevertRequestStatusHolder.RevertStatus revertStatus = revertRequestStatusHolder
                .getRevertStatus(data.getString(ImportFields.ID.getName()));

        if (Objects.nonNull(revertStatus)) {
            switch (revertStatus) {
                case NEW -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), false);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
                case IN_PROGRESS -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), true);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
                case COMPLETED -> response.put(ImportFields.IS_DONE.getName(), true);
                case FAILED ->
                        response.setErrorCode(ErrorCode.get(ImportErrorCodes.REVERT_ACTION_FAILED, "i18n.revert.status.failed"));
                default ->
                        response.setErrorCode(ErrorCode.get(ImportErrorCodes.INVALID_REVERT_STATUS, "i18n.revert.status.invalid"));
            }
        }
    }
}
