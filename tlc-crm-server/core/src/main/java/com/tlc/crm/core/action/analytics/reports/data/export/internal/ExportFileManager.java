package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.commons.util.Env;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reusable utility that manages export file naming, directory creation, and stream lifecycle.
 *
 * @author Saravanan
 * @version 1.0
 */
final class ExportFileManager {

    private static final String UPLOADS = "uploads";
    private static final String ORG = "org";

    private ExportFileManager() {
    }

    private static class Instance {
        private static final ExportFileManager INSTANCE = new ExportFileManager();
    }

    static @NonNull ExportFileManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates an export file and opens its output stream.
     *
     * @param orgId organization identifier used in folder hierarchy
     * @param exportType logical export group (for example analyticsExport)
     * @param sourceName logical source name used as the file name prefix
     * @param extension target file extension without leading dot
     * @return context containing the created file and its output stream
     * @throws IOException when directory creation or file stream opening fails
     */
    @NonNull ExportStreamContext openStream(final @NonNull Long orgId,
                                            final @NonNull String exportType,
                                            final @NonNull String sourceName,
                                            final @NonNull String extension) throws IOException {
        final String exportBasePath = getExportBasePath(orgId, exportType);
        final String fileName = getExportFileName(sourceName, extension);
        final File file = Path.of(exportBasePath, fileName).toFile();

        try {
            return new ExportStreamContext(file, new FileOutputStream(file));
        } catch (IOException exception) {
            CodeBlockExecutor.runSilently(() -> Files.deleteIfExists(file.toPath()));
            throw exception;
        }
    }

    /**
     * Resolves and creates the export base directory.
     *
     * @param orgId organization identifier used in folder hierarchy
     * @param exportType logical export group name
     * @return absolute export directory path
     * @throws IOException when the directory cannot be created
     */
    @NonNull String getExportBasePath(final @NonNull Long orgId,
                                      final @NonNull String exportType) throws IOException {
        final Path exportBasePath = Path.of(Env.getWorkDirectory(), UPLOADS, ORG + orgId, exportType);
        Files.createDirectories(exportBasePath);
        return exportBasePath.toString();
    }

    /**
     * Builds an export file name from the source name and extension.
     *
     * @param sourceName logical source or view name
     * @param extension file extension without leading dot
     * @return combined file name in {@code sourceName.extension} format
     */
    @NonNull String getExportFileName(final @NonNull String sourceName,
                                      final @NonNull String extension) {
        return String.join(".", sourceName, extension);
    }

    /**
     * Flushes and closes the stream.
     *
     * @param streamContext context holding the target stream and file
     * @return finalized export file
     * @throws IOException when flush or close fails
     */
    @NonNull File closeStream(final @NonNull ExportStreamContext streamContext) throws IOException {
        streamContext.getStream().flush();
        streamContext.getStream().close();
        return streamContext.getFile();
    }

    /**
     * Safely aborts export by closing the stream and deleting the partial file.
     *
     * @param streamContext context holding the stream and file to clean up
     */
    void abortStream(final @NonNull ExportStreamContext streamContext) {
        CodeBlockExecutor.runSilently(() -> streamContext.getStream().close());
        CodeBlockExecutor.runSilently(() -> Files.deleteIfExists(streamContext.getFile().toPath()));
    }

    /**
     * Immutable holder for an export file and its active output stream.
     */
    @Getter
    static final class ExportStreamContext {
        private final File file;
        private final OutputStream stream;

        /**
         * Creates a stream context wrapper.
         *
         * @param file target export file
         * @param stream open stream used to write the export file
         */
        private ExportStreamContext(final @NonNull File file,
                                    final @NonNull OutputStream stream) {
            this.file = file;
            this.stream = stream;
        }
    }
}

