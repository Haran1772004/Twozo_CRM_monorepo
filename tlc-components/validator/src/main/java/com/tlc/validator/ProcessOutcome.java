package com.tlc.validator;

import com.tlc.validator.model.ErrorSummary;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * This class represents the outcome of record processing, containing both successfully processed records
 *  and records that failed during processing.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ProcessOutcome {

    private Collection<Object> processedRecords;
    private ErrorSummary errorSummary;

    public ProcessOutcome() {
        this.processedRecords = new ArrayList<>();
    }

    public Collection<Object> getProcessedRecords() {
        return processedRecords;
    }

    public ErrorSummary getErrorSummary() {
        return errorSummary;
    }

    public void addProcessedRecord(final Object processedRecord) {
        processedRecords.add(processedRecord);
    }

    public void setProcessedRecords(final Collection<Object> processedRecords) {
        this.processedRecords = processedRecords;
    }

    public void setErrorSummary(final ErrorSummary errorSummary) {
        this.errorSummary = errorSummary;
    }
}