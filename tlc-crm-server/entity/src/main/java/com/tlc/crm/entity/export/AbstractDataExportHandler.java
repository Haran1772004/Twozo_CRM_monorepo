package com.tlc.crm.entity.export;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.FileGenerateRequest;
import com.tlc.attachment.model.FileInfo;
import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.generator.RecordFileGenerator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Env;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.export.alert.publisher.ExportNotificationPublisher;
import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.model.ExportCategory;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.export.formatter.DataExportFormatter;
import com.tlc.crm.entity.export.internal.model.EmailExportRequest;
import com.tlc.crm.entity.export.internal.model.FileExportRequest;


import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;

import com.tlc.web.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * <p>
 * A converter that transforms the data and writes the result to a file. Generates the export file,
 * uploads it as an attachment, updates the export status and notifies the user once the export is completed.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractDataExportHandler implements DataExportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataExportHandler.class);

    private static final String DATE_FORMAT_PATTERN = "MM/dd/yyyy";
    private static final String TIME_FORMAT_PATTERN = "hh:mm:ss";
    private static final String DATE_TIME_FORMAT_PATTERN = String.format("%s %s", DATE_FORMAT_PATTERN, TIME_FORMAT_PATTERN);
    private static final String EXPORT = "export";
    private static final String SEPARATOR = java.io.File.separator;
    private static final String HYPHEN = " - ";
    private static final String CHARACTERS = "[^a-zA-Z0-9.-]";

    protected final DataExportFormatter dataExportFormatter;
    private final DataExportManager dataExportManager;
    private final AttachmentService attachmentService;
    private final EventDataHandler eventDataHandler;
    private final String exportFileDownloadUrl;
    private final ExportNotificationPublisher exportNotificationPublisher;

    /**
     * <p>
     * Creates the abstract data export handler and initializes the formatter, managers,
     * services and the file download url required to perform the export.
     * </p>
     */
    protected AbstractDataExportHandler() {
        this.dataExportFormatter = DataExportFormatter.getInstance();
        this.dataExportManager = DataExportManager.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.eventDataHandler = EventDataHandler.getInstance();
        this.exportFileDownloadUrl = ConfHolder.getInstance().getProperties("data-export").getConfig("redirect-url.fileDownload");
        this.exportNotificationPublisher = ExportNotificationPublisher.getInstance();
    }

    /**
     * <p>
     * Exports the provided data into a file. Uploads the generated file as an attachment, marks the export
     * as exported and notifies the user through email and app notification. If the file generation fails,
     * the export is marked as failed.
     * </p>
     *
     * @param sourceName        Represents the source name.
     * @param fileExportRequest Represents the {@link FileExportRequest}.
     * @throws ErrorCode if there is no data to export.
     */
    protected void export(final String sourceName, final FileExportRequest fileExportRequest) {
        final Collection<Map<String, Object>> data = fileExportRequest.getEntityRows();
        final long orgId = fileExportRequest.getOrgId();
        final long exportId = fileExportRequest.id();
        final Extension extension = fileExportRequest.getExtension();
        final String exportBasePath = String.format("%s%s%s%s%s%s%s%s", Env.getWorkDirectory(), SEPARATOR, "uploads",
                SEPARATOR, "org", orgId, SEPARATOR, EXPORT);
        try {
            Files.createDirectories(Path.of(exportBasePath));
            final String fileName = String.format("%s-%s.%s", sourceName, LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)).replaceAll(CHARACTERS, HYPHEN), extension.getName());
            final RecordHolder recordHolder = buildRecordHolder(data, fileExportRequest.getHeaders());
            final int totalRecords = data.size();
            final File exportFile = RecordFileGenerator.get(extension).generate(new FileGenerateRequest<>(
                    new FileInfo(String.format("%s%s%s", exportBasePath, SEPARATOR, fileName), extension), recordHolder));

            loadAttachment(orgId, exportId, totalRecords, exportFile);
            dataExportManager.updateStatus(orgId, exportId, ExportTracker.ExportStatus.EXPORTED);
            final long userId = fileExportRequest.getUserId();
            final String exportName = resolveExportName(sourceName.toLowerCase(), fileExportRequest.getExportProvider());
            final String fileDownloadUrl = HttpUtil.buildAbsoluteUrl(exportFileDownloadUrl);
            final EmailExportRequest emailExportRequest = new EmailExportRequest();

            emailExportRequest.setId(exportId);
            emailExportRequest.setExportName(exportName);
            emailExportRequest.setUserId(userId);
            emailExportRequest.setFileDownloadUrl(fileDownloadUrl);
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setAction(Action.CREATE);
            eventData.setValues(List.of(emailExportRequest));

            eventDataHandler.handleEvent(EMAIL_ALERT_EVENT, eventData, Event.Type.POST);
            exportNotificationPublisher.publish(orgId, userId, exportId, AppNotificationEventType.EXPORTED, exportName, fileDownloadUrl);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Data export: Export file generation failed. OrgId: {}, ExportId: {}", orgId, exportId);
            dataExportManager.updateStatus(orgId, exportId, ExportTracker.ExportStatus.EXPORT_FAILED);
        }
    }

    /**
     * <p>
     * Uploads the generated export file as an attachment and links the uploaded attachment
     * to the export.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param exportId     Represents the export id.
     * @param totalRecords Represents the total number of records exported.
     * @param exportFile   Represents the generated export {@link File}.
     */
    private void loadAttachment(final long orgId, final long exportId, final int totalRecords, final File exportFile) {
        final Long iamOrgId = OrganizationService.getInstance().getIamOrgId(orgId);
        final Attachment attachment = new ObjectAttachment();

        attachment.setInputStream(exportFile.inputStream());
        attachment.setMediaType(exportFile.mediaType());
        attachment.setExtension(exportFile.extension());
        attachment.setFileName(exportFile.name());
        attachment.setFileSize(exportFile.size());

        final Collection<Attachment> attachments = attachmentService.upload(iamOrgId, List.of(attachment), String.valueOf(ExportCategory.FILE.getType()));

        attachments.stream().findFirst().ifPresent(exportAttachment -> dataExportManager.updateExportFileId(orgId, exportId, exportAttachment.id()));
        LOGGER.info("Data export: Export file generated successfully with records: {}, ExportId: {}, OrgId: {}", totalRecords, exportId, orgId);
    }

    /**
     * <p>
     * Builds the {@link RecordHolder} containing the headers and the records
     * for the given export data.
     * </p>
     *
     * @param data    Represents the rows to be exported.
     * @param headers Represents the column headers of the export file.
     * @return The {@link RecordHolder} holding the headers and records.
     */
    private RecordHolder buildRecordHolder(final Collection<Map<String, Object>> data,
                                           final Collection<String> headers) {
        final Map<Integer, Map<String, Object>> rowMap = new LinkedHashMap<>();
        final RecordHolder recordHolder = RecordHolder.create();
        int index = 0;

        for (final Map<String, Object> map : data) {
            rowMap.put(++index, map);
        }

        recordHolder.addHeaders(headers);

        for (final Map.Entry<Integer, Map<String, Object>> row : rowMap.entrySet()) {
            final Integer key = row.getKey();
            final Map<String, Object> value = row.getValue();

            recordHolder.addRecord(key, buildRecord(value, headers, key));
        }

        return recordHolder;
    }

    /**
     * <p>
     * Builds a {@link Record} for the given row by picking the value of each header in order.
     * An empty value is used for the headers that have no value in the row.
     * </p>
     *
     * @param row     Represents the row data keyed by the column header.
     * @param headers Represents the column headers of the export file.
     * @param index   Represents the index of the record.
     * @return The {@link Record} built for the row.
     */
    private Record buildRecord(final Map<String, Object> row, final Collection<String> headers, final int index) {
        final Record exportRecord = new Record(index, headers.size());
        int columnIndex = 0;

        for (final String header : headers) {
            final Object value = row.get(header);

            exportRecord.add(columnIndex++, Objects.isNull(value) ? "" : value);
        }
        return exportRecord;
    }

    /**
     * <p>
     * Resolves the export name used in the notifications. For the list export provider,
     * the source name is resolved to its plural display name.
     * </p>
     *
     * @param exportName         Represents the lower-cased source name of the export.
     * @param exportProviderType Represents the {@link ExportProviderType}.
     * @return The resolved export name, or {@code null} if the source is not supported for the list export provider.
     */
    private String resolveExportName(final String exportName, final ExportProviderType exportProviderType) {
        if (Objects.equals(exportProviderType, ExportProviderType.LIST)) {
            return switch (exportName) {
                case "contact" -> "contacts";
                case "company" -> "companies";
                case "deal" -> "deals";
                case "product" -> "products";
                case "activity" -> "activities";
                case "segment" -> "segments";
                default -> null;
            };
        }
        return exportName;
    }
}