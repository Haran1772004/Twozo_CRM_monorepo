package com.tlc.crm.core.api.deal.model;

import com.tlc.l10n.temporal.model.DateTime;

/**
 * Model which represents the stage progress
 *
 * @author pandiaraj
 */
public class StageProgress {

    private DateTime startTime;

    private DateTime endTime;

    private PipelineStage pipelineStage;

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final DateTime endTime) {
        this.endTime = endTime;
    }

    public PipelineStage getPipelineStage() {
        return pipelineStage;
    }

    public void setPipelineStage(final PipelineStage pipelineStage) {
        this.pipelineStage = pipelineStage;
    }
}
