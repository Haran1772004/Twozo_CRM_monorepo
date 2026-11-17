package com.tlc.crm.core.features.internal.dataimport.async.task;

import com.tlc.attachment.model.RecordHolder;
import com.tlc.commons.executor.ExecutorEntry;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

/**
 * A model representing the task of reading an importable file
 *
 * @author sundar
 * @version 1.0
 */
public class ImportableFileReadTask extends Task {

    private final ImportInfo importInfo;
    private final RunnableTask task;

    private RecordHolder recordHolder;

    public ImportableFileReadTask(final RunnableTask task, final ImportInfo importInfo, final FileReadTaskController taskController) {
        super(taskController, 0);
        this.task = task;
        this.importInfo = importInfo;
    }

    public RecordHolder getRecordHolder() {
        return recordHolder;
    }

    public ImportInfo getImportInfo() {
        return importInfo;
    }

    public void setRecordHolder(final RecordHolder recordHolder) {
        this.recordHolder = recordHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param taskController Represents the task controller
     */
    @Override
    public void execute(final TaskController taskController) {
        task.run();
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the read request is completed successfully or not
     */
    public boolean isCompleted() {
        return isSuccess() || getStatus() == ExecutorEntry.ExecutorStatus.FAILED;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the read action is successfully completed
     */
    public boolean isSuccess() {
        return getStatus() == ExecutorEntry.ExecutorStatus.COMPLETED;
    }

    /**
     * A controller system responsible for managing the execution schedule of importable file read task
     *
     * @author sundar
     * @version 1.0
     */
    public static class FileReadTaskController extends TaskController {

        public FileReadTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}