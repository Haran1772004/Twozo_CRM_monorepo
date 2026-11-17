package com.tlc.crm.core.features.api.dataimport.model;

/**
 * A model that contains the imported record statistics of inserted, updated, merged and skipped records count
 *
 * @author sundar
 * @version 1.0
 */
public class ImportStatistics {

    private int mergedCount;
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;

    public int getMergedCount() {
        return mergedCount;
    }

    public void setMergedCount(final int mergedCount) {
        this.mergedCount = mergedCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(final int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(final int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(final int skippedCount) {
        this.skippedCount = skippedCount;
    }
}
