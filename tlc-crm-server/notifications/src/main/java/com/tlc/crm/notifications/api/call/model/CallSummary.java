package com.tlc.crm.notifications.api.call.model;

import com.tlc.crm.entity.models.type.Monetary;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;

/**
 * Represents a summary of a call, including details such as start time,
 * end time, duration, billing duration, and cost.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallSummary implements TlcModel {

    private Long id;
    private Long orgId;
    private DateTime startTime;
    private DateTime endTime;
    private String duration;
    private String billingDuration;
    private Monetary cost;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

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

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getBillingDuration() {
        return billingDuration;
    }

    public void setBillingDuration(final String billingDuration) {
        this.billingDuration = billingDuration;
    }

    public Monetary getCost() {
        return cost;
    }

    public void setCost(final Monetary cost) {
        this.cost = cost;
    }

    @Override
    public Object identity() {
        return null;
    }
}