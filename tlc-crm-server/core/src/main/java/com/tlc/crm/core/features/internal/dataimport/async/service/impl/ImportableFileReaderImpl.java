package com.tlc.crm.core.features.internal.dataimport.async.service.impl;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.async.registry.ReadTaskRegistry;
import com.tlc.crm.core.features.internal.dataimport.async.service.ImportableFileReader;
import com.tlc.crm.core.features.internal.dataimport.async.task.ImportableFileReadTask;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;

/**
 * Provides the implementation for the service provided by the {@link ImportableFileReader}
 *
 * @author sundar
 * @version 1.0
 * @see ImportableFileReader
 */
public class ImportableFileReaderImpl implements ImportableFileReader {

    private static final String READER_POOL_NAME = "importable-file-reader-pool";

    private final SchedulerService schedulerService;
    private final ReadTaskRegistry readTaskRegistry;

    public ImportableFileReaderImpl() {
        this.schedulerService = SchedulerServiceProvider.get();
        this.readTaskRegistry = ReadTaskRegistry.get();
    }

    private static class Instance {
        private static final ImportableFileReader INSTANCE = new ImportableFileReaderImpl();
    }

    /**
     * Gets the instance of the {@link ImportableFileReader}
     *
     * @return {@link ImportableFileReader}
     */
    public static ImportableFileReader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo Represents the import ID
     * @param task       Represents the task to be submitted for reading an Excel file
     * @return The {@link ImportableFileReadTask} that was submitted to process the file's read operation
     */
    @Override
    public ImportableFileReadTask read(final ImportInfo importInfo, final RunnableTask task) {
        final ImportableFileReadTask fileReadTask = new ImportableFileReadTask(task, importInfo, new ImportableFileReadTask.FileReadTaskController(READER_POOL_NAME));

        if (readTaskRegistry.add(importInfo.id(), fileReadTask)) {
            schedulerService.schedule(READER_POOL_NAME, fileReadTask);
            return fileReadTask;
        } else {
            return readTaskRegistry.get(importInfo.id());
        }
    }
}