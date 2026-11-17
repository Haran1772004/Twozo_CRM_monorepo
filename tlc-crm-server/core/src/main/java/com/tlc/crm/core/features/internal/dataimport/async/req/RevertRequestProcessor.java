package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.commons.json.Json;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.dataimport.manager.RevertManager;
import com.tlc.crm.core.features.field.dataimport.ImportFields;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.RevertRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.RevertRequestStatusHolder.RevertStatus;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * A consumer that consumes the revert requests and triggers the revert actions based on the request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RevertRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevertRequestProcessor.class);

    private final RevertRequestStatusHolder revertRequestStatusHolder;
    private final RevertManager revertManager;
    private final ImportManager importManager;

    public RevertRequestProcessor() {
        this.revertRequestStatusHolder = RevertRequestStatusHolder.getInstance();
        this.revertManager = RevertManager.getInstance();
        this.importManager = ImportManager.getInstance();
    }

    /**
     * Reverts the imported records.
     *
     * @param revertRequest Holds the revert request parameters.
     */
    private void revert(final RevertRequest revertRequest) {
        final Long orgId = revertRequest.orgId();
        final long importId = revertRequest.importId();
        final long userId = revertRequest.userId();
        final String revertRequestId = revertRequest.id();

        if (revertManager.getImportedRecordsCount(orgId, importId) > 0) {
            importManager.updateStatus(orgId, importId, ImportInfo.Status.REVERTING.getType());
            final long revertStartTime = System.currentTimeMillis();

            try {
                revertManager.revert(new DataHolder(orgId, userId, revertRequest.locale()), importId);
                LOGGER.info("Data revert: Revert completed for orgId : {}, userId : {}, revertRequestId : {}, importId : {} in {} ms", orgId, userId, revertRequestId, importId, System.currentTimeMillis() - revertStartTime);
            } catch (Throwable throwable) {
                revertRequestStatusHolder.setRevertStatus(revertRequestId, RevertStatus.FAILED);
                LoggerUtil.logError(LOGGER, throwable, "Data revert: Data revert failed, orgId: {}, userId: {}, revertRequestId : {}, importId: {}", orgId, userId, revertRequestId, importId);
                importManager.updateStatus(orgId, importId, ImportInfo.Status.REVERT_FAILED.getType());
            }
        }
        importManager.updateStatus(orgId, importId, ImportInfo.Status.REVERTED.getType());
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Refers the {@link StreamMessage} which holds the request parameters.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final String revertRequestId = streamMessage.id();
        final String[] decodedKeyParts = new String(Base64.getDecoder().decode(revertRequestId.getBytes())).split(":");
        final long importId = NumberUtils.parseNumber(decodedKeyParts[0]).longValue();
        final long orgId = NumberUtils.parseNumber(decodedKeyParts[1]).longValue();
        final long userId = NumberUtils.parseNumber(decodedKeyParts[2]).longValue();
        final RevertStatus revertStatus = revertRequestStatusHolder.getRevertStatus(revertRequestId);

        try {
            LOGGER.info("Data revert: Revert request received. orgId : {} userId : {} requestId : {} importId : {}", orgId, userId, revertRequestId, importId);

            if (revertRequestStatusHolder.isNewRevert(revertStatus)) {
                final JsonObject dataJson = Json.object(streamMessage.data());
                final RevertRequest revertRequest = new RevertRequest(revertRequestId, importId, orgId, userId,
                        Locale.forLanguageTag(dataJson.getString(ImportFields.LOCALE.getName())), dataJson, clean -> streamMessage.clean());

                revertRequestStatusHolder.setRevertStatus(revertRequestId, RevertStatus.IN_PROGRESS);
                revert(revertRequest);
                revertRequestStatusHolder.setRevertStatus(revertRequestId, RevertStatus.COMPLETED);
            } else if (Objects.isNull(revertStatus)) {
                importManager.updateStatus(orgId, importId, ImportInfo.Status.REVERT_FAILED.getType());
                streamMessage.clean();
            }
        } catch (Throwable throwable) {
            revertRequestStatusHolder.setRevertStatus(revertRequestId, RevertStatus.FAILED);
            importManager.updateStatus(orgId, importId, ImportInfo.Status.REVERT_FAILED.getType());
            LoggerUtil.logError(LOGGER, throwable, "Data revert: Revert request processing failed. orgId : {} userId : {} requestId: {} importId : {}, Error Message: {}",
                    orgId, userId, revertRequestId, importId, throwable.getMessage());
        }
    }
}