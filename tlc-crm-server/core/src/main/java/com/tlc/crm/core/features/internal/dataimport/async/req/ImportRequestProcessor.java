package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.async.impl.DataImporter;
import com.tlc.crm.core.features.internal.dataimport.async.service.ImportServiceHolder;

import com.tlc.crm.core.features.internal.dataimport.async.status.holder.ImportRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.async.status.holder.ImportRequestStatusHolder.ImportStatus;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Locale;

/**
 * A consumer that consumes the data import requests and triggers the data import actions
 * based on the request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ImportRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportRequestProcessor.class);

    private final ImportRequestStatusHolder importRequestStatusHolder;
    private final ImportRequestCache importRequestCache;
    private final DataImporter dataImporter;
    private final Executor dataImportExecutor;

    public ImportRequestProcessor(final Executor dataImportExecutor) {
        this.importRequestStatusHolder = ImportRequestStatusHolder.getInstance();
        this.importRequestCache = ImportRequestCache.getInstance();
        this.dataImporter = ImportServiceHolder.getDataImporter();
        this.dataImportExecutor = dataImportExecutor;
    }

    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final String importRequestId = streamMessage.id();
        final String[] decodedKeyParts = new String(Base64.getDecoder().decode(importRequestId.getBytes())).split(":");

        try {
            LOGGER.info("Data import: Request received. Id: {}", importRequestId);
            final JsonObject dataJson = Json.object(streamMessage.data());
            final ImportRequest importRequest = new ImportRequest(importRequestId, Long.parseLong(decodedKeyParts[0]), Long.parseLong(decodedKeyParts[1]),
                    Long.parseLong(decodedKeyParts[2]), Locale.forLanguageTag(dataJson.getString(ImportFields.LOCALE.getName())), dataJson, unused -> streamMessage.clean());

            importRequestCache.put(importRequestId, importRequest);

            if (importRequestStatusHolder.isImported(importRequestId)) {
                LOGGER.info("Data import: Import is already done. Request ID: {}", importRequestId);
                return;
            }

            if (importRequestStatusHolder.isNewImport(importRequestId)) {
                importRequestStatusHolder.setImportStatus(importRequestId, ImportStatus.IMPORTING);
            }
            LOGGER.info("Data import: Import process is starting for the request. Id: {}", importRequestId);
            dataImportExecutor.execute(() -> dataImporter.importData(importRequest));
        } catch (Throwable throwable) {
            importRequestStatusHolder.setImportStatus(importRequestId, ImportStatus.IMPORT_FAILED);
            ImportManager.getInstance().updateStatus(NumberUtils.parseNumber(decodedKeyParts[1]).longValue(), NumberUtils.parseNumber(decodedKeyParts[0]).longValue(), ImportInfo.Status.FAILED.getType());
            streamMessage.clean();
            LoggerUtil.logError(LOGGER, throwable, "Data import: Import request processing failed, RequestId: {}, Error Message: {}", importRequestId, throwable.getMessage());
        }
    }
}