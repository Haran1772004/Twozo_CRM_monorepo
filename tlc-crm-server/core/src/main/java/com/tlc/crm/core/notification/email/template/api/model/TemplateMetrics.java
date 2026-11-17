package com.tlc.crm.core.notification.email.template.api.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * An entity that encapsulates the statistical data of a template
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class TemplateMetrics implements TlcModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private long sentCount;
    private long repliedCount;
    private long openedCount;
    private long clickedCount;
    private long unsubscribedCount;

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

    @Override
    public Object identity() {
        return null;
    }

    public long getSentCount() {
        return sentCount;
    }

    public void setSentCount(final long sentCount) {
        this.sentCount = sentCount;
    }

    public long getRepliedCount() {
        return repliedCount;
    }

    public void setRepliedCount(final long repliedCount) {
        this.repliedCount = repliedCount;
    }

    public long getOpenedCount() {
        return openedCount;
    }

    public void setOpenedCount(final long openedCount) {
        this.openedCount = openedCount;
    }

    public long getClickedCount() {
        return clickedCount;
    }

    public void setClickedCount(final long clickedCount) {
        this.clickedCount = clickedCount;
    }

    public long getUnsubscribedCount() {
        return unsubscribedCount;
    }

    public void setUnsubscribedCount(final long unsubscribed) {
        this.unsubscribedCount = unsubscribed;
    }

    /**
     * <p>
     * Calculates the percentage of template statistics
     * </p>
     *
     * @param value     Represents the different field count in statistics
     * @param sentCount Represents the total sent count of template
     * @return {@link Double} percentage of fields in statistics
     */
    public String calculateStatisticsInPercentage(final long value, final long sentCount) {
        final float percentage = (value * 100) / (float) sentCount;
        final double roundedPercentage = Math.round(percentage * 100.0) / 100.0;

        if (roundedPercentage == (long) roundedPercentage) {
            return String.format("%d%%", (long) roundedPercentage);
        } else {
            return String.format("%.2f%%", roundedPercentage);
        }
    }

    /**
     * Represents the {@link MetricsType}
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum MetricsType {

        SENT,
        OPENED,
        REPLIED,
        CLICKED,
        UNSUBSCRIBED,
        ;
    }
}
