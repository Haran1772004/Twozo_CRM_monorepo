package com.tlc.crm.core.api.deal.model;

import com.tlc.l10n.temporal.model.Date;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Model which represents the deal progress
 *
 * @author pandiaraj
 */
public class DealProgress {

    private Long dealId;

    private Long pipelineId;

    private String pipelineName;

    private Date expectedCloseDate;

    private Double probability;

    private Collection<StageProgress> stageProgresses;

    public enum Status {
        ACTIVE,
        COMPLETED,
        PENDING;
    }

    public void addStageProgress(final StageProgress stageProgress) {
        this.stageProgresses = this.stageProgresses == null ? new ArrayList<>() : this.stageProgresses;

        this.stageProgresses.add(stageProgress);
    }

    public void setDealId(final Long dealId) {
        this.dealId = dealId;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(final Long pipelineId) {
        this.pipelineId = pipelineId;
    }

    public void setPipelineName(final String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public void setExpectedCloseDate(final Date expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public Long getDealId() {
        return dealId;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public Date getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public Collection<StageProgress> getStageProgresses() {
        return stageProgresses;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(final Double probability) {
        this.probability = probability;
    }
}
