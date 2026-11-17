package com.tlc.crm.core.features.internal.dataimport.async.service;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.async.service.impl.ImportableFileReaderImpl;
import com.tlc.crm.core.features.internal.dataimport.async.task.ImportableFileReadTask;

/**
 * Offers a service for asynchronously processing importable file read operations
 *
 * @author sundar
 * @version 1.0
 */
@FunctionalInterface
public interface ImportableFileReader {

    /**
     * Gets the instance of the {@link ImportableFileReader}
     *
     * @return {@link ImportableFileReader}
     */
    static ImportableFileReader get() {
        return ImportableFileReaderImpl.getInstance();
    }

    /**
     * Asynchronously submits a task to the executor for processing the read operation of the Excel file
     *
     * @param importId Represents the import ID
     * @param task     Represents the task to be submitted for reading an Excel file
     * @return The {@link ImportableFileReadTask} that was submitted to process the file's read operation
     */
    ImportableFileReadTask read(final ImportInfo importId, final RunnableTask task);
}
