package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.FileInfo;
import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.FileGenerateRequest;
import com.tlc.attachment.reader.DataReader;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.generator.RecordFileGenerator;

import com.tlc.commons.util.Env;
import com.tlc.commons.util.FileUtils;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.client.service.AttachmentService;

import com.tlc.file.common.models.ObjectAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Generates skip file which contains data that are skipped during the import process.
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class SkipFileGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkipFileGenerator.class);

    private static final String SEPARATOR = java.io.File.separator;
    private static final String UPLOADS = "uploads";
    private static final String SKIP_FILE = "skipFile";
    private static final String ORG = "org";

    private final ImportManager importManager;
    private final AttachmentService attachmentService;

    private SkipFileGenerator() {
        this.importManager = ImportManager.getInstance();
        this.attachmentService = AttachmentService.getInstance();
    }

    private static final class InstanceHolder {
        private static final SkipFileGenerator SKIP_FILE_GENERATOR = new SkipFileGenerator();
    }

    public static SkipFileGenerator getInstance() {
        return InstanceHolder.SKIP_FILE_GENERATOR;
    }

    /**
     * <p>
     * Generates a file that contains the reasons for data that were skipped during the import process.
     * This can help in identifying and addressing issues with the imported data.
     * </p>
     *
     * @param orgId          Refers the organization id.
     * @param importId       Refers the import id.
     * @param skippedReasons Refers the skipped reasons grouped in a map.
     */
    public void generate(final long orgId, final long importId, final Map<Integer, String> skippedReasons) {
        final ImportInfo importInfo = importManager.get(orgId, importId);
        final Long iamOrgId = OrganizationService.getInstance().getIamOrgId(orgId);

        try {
            LOGGER.info("Data import: Skip file write initiated. Import Id: {}, OrgId: {}", importId, orgId);
            final File file = attachmentService.getFile(iamOrgId, importInfo.getAttachment().id());
            final RecordHolder recordHolder = RecordHolder.create();
            final DataReader dataReader = DataReader.get(Extension.get(file.extension()));
            final Collection<Integer> indices = skippedReasons.keySet();

            recordHolder.addHeader("Index");
            recordHolder.addHeader("Skipped Reason");
            dataReader.read(file, indices, recordHolder);

            final Map<Integer, Record> skippedValueRows = recordHolder.getRecords();

            for (final int index : indices) {
                final Record record = skippedValueRows.get(index);

                record.add(0, index);
                record.add(1, skippedReasons.get(index));
            }

            final String skipFileBasePath = String.format("%s%s%s%s%s%s%s%s", Env.getWorkDirectory(), SEPARATOR, UPLOADS,
                    SEPARATOR, ORG, orgId, SEPARATOR, SKIP_FILE);
            final Extension extension = Extension.XLSX;
            final String skipFilePath = String.format("%s%s%s", skipFileBasePath, SEPARATOR,
                    String.format("skip_file_%s.%s", importId, extension.getName()));

            FileUtils.createDirectory(skipFileBasePath);

            final File skipFile = RecordFileGenerator.get(extension).generate(new FileGenerateRequest<>(new FileInfo(skipFilePath, extension), recordHolder));
            final Attachment attachment = new ObjectAttachment();

            attachment.setInputStream(skipFile.inputStream());
            attachment.setMediaType(skipFile.mediaType());
            attachment.setExtension(skipFile.extension());
            attachment.setFileName(skipFile.name());
            attachment.setFileSize(skipFile.size());

            final Collection<Attachment> attachments = attachmentService.upload(iamOrgId, List.of(attachment), FileCategory.IMPORT.getName());

            attachments.stream().findFirst().ifPresent(skipAttachment -> importManager.updateSkipFileId(orgId, importId, skipAttachment.id()));
            LOGGER.info("Data import: Skip file generated successfully with records: {}, Import Id: {}, OrgId: {}", recordHolder.totalRecords(), importId, orgId);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Data import: Skip file generation failed. Import Id: {}, OrgId: {}, Stack: ", importId, orgId);
        }
    }
}
