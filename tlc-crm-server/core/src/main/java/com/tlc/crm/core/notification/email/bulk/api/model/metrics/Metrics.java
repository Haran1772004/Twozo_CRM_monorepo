package com.tlc.crm.core.notification.email.bulk.api.model.metrics;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * Represents the {@link Metrics} that encapsulates the information about bulk
 * email metrics details and their associated data.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class Metrics implements TlcModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.notifications.bulk.mail.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.bulk.mail.invalid.user.id")
    private Long senderId;

    private Long createdTime;

    private int clickedCount;
    private int openedCount;
    private int bouncedCount;
    private int repliedCount;
    private int sentCount;
    private int unsubscribedCount;
    private int openedNotClickedCount;
    private int openedNotRepliedCount;
    private int notOpenedCount;
    private int notSentCount;

    public int getNotSentCount() {
        return notSentCount;
    }

    public void setNotSentCount(final int notSentCount) {
        this.notSentCount = notSentCount;
    }

    public int getNotOpenedCount() {
        return notOpenedCount;
    }

    public void setNotOpenedCount(final int notOpenedCount) {
        this.notOpenedCount = notOpenedCount;
    }

    public int getOpenedNotClickedCount() {
        return openedNotClickedCount;
    }

    public void setOpenedNotClickedCount(final int openedNotClickedCount) {
        this.openedNotClickedCount = openedNotClickedCount;
    }

    public int getOpenedNotRepliedCount() {
        return openedNotRepliedCount;
    }

    public void setOpenedNotRepliedCount(final int openedNotRepliedCount) {
        this.openedNotRepliedCount = openedNotRepliedCount;
    }

    public int getUnsubscribedCount() {
        return unsubscribedCount;
    }

    public void setUnsubscribedCount(final int unsubscribedCount) {
        this.unsubscribedCount = unsubscribedCount;
    }

    public int getClickedCount() {
        return clickedCount;
    }

    public void setClickedCount(final int clickedCount) {
        this.clickedCount = clickedCount;
    }

    public int getOpenedCount() {
        return openedCount;
    }

    public void setOpenedCount(final int openedCount) {
        this.openedCount = openedCount;
    }

    public int getBouncedCount() {
        return bouncedCount;
    }

    public void setBouncedCount(final int bouncedCount) {
        this.bouncedCount = bouncedCount;
    }

    public int getRepliedCount() {
        return repliedCount;
    }

    public void setRepliedCount(final int repliedCount) {
        this.repliedCount = repliedCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(final int sentCount) {
        this.sentCount = sentCount;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Represents the {@link MetricsType}
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum MetricsType {

        OPENED(1, "i18n.bulk.metrics.opened"),
        CLICKED(2, "i18n.bulk.metrics.clicked"),
        BOUNCED(3, "i18n.bulk.metrics.bounced"),
        REPLIED(4, "i18n.bulk.metrics.replied"),
        SENT(5, "i18n.bulk.metrics.sent"),
        UNSUBSCRIBED(6, "i18n.bulk.metrics.unsubscribed"),
        NO_PRIMARY_EMAIL(7, "i18n.bulk.metrics.noPrimaryEmail"),
        OPENED_NOT_CLICKED(8, "i18n.bulk.metrics.openedNotClicked"),
        OPENED_NOT_REPLIED(9, "i18n.bulk.metrics..openedNotReplied"),
        NOT_SENT(10, "i18n.bulk.metrics.notSent"),
        NOT_OPENED(11, "i18n.bulk.metrics.notOpened"),
        ;

        private final int id;
        private final String name;

        MetricsType(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * Gets the {@link MetricsType} for given id.
         *
         * @param id Represents the metrics type id
         * @return {@link MetricsType}
         */
        public static MetricsType get(final int id) {
            return switch (id) {
                case 1 -> OPENED;
                case 2 -> CLICKED;
                case 3 -> BOUNCED;
                case 4 -> REPLIED;
                case 5 -> SENT;
                case 6 -> UNSUBSCRIBED;
                case 7 -> NO_PRIMARY_EMAIL;
                default -> throw ErrorCode.get(BulkMailErrorCodes.INVALID_METRICS_TYPE, "i18n.notifications.bulk.mail.invalid.metrics.type");
            };
        }

        /**
         * Gets the {@link MetricsType} of given type.
         *
         * @param type Represents the type of metrics
         * @return {@link MetricsType} of metrics
         */
        public static MetricsType getMetricType(final String type) {
            return switch (type) {
                case "opened" -> OPENED;
                case "clicked" -> CLICKED;
                case "openedNotClicked" -> OPENED_NOT_CLICKED;
                case "openedNotReplied" -> OPENED_NOT_REPLIED;
                case "sent" -> SENT;
                case "replied" -> REPLIED;
                case "notSent" -> NOT_SENT;
                case "notOpened" -> NOT_OPENED;
                default -> throw ErrorCode.get(BulkMailErrorCodes.INVALID_METRICS_TYPE, "i18n.notifications.bulk.mail.invalid.metrics.type");
            };
        }

        /**
         * Checks whether the type is depends on the common types.
         *
         * @param type Represents the {@link MetricsType}
         * @return true, if the type which is common
         */
        public static boolean hasCommonMetrics(final MetricsType type) {
            return switch (type) {
                case OPENED, CLICKED, SENT, REPLIED, UNSUBSCRIBED, BOUNCED -> true;
                default -> false;
            };
        }

        /**
         * Checks whether the type is depends on the outbound related metrics type.
         *
         * @param type Represents the {@link MetricsType}
         * @return true, if the type which is outbound dependent
         */
        public static boolean hasOutboundMetrics(final MetricsType type) {
            return switch (type) {
                case OPENED, CLICKED, SENT, REPLIED, UNSUBSCRIBED, BOUNCED, NO_PRIMARY_EMAIL -> true;
                default -> false;
            };
        }
    }

    /**
     * Computes the metrics count in percentage
     *
     * @param value     Represents the value of metrics type
     * @param sentCount Represents the count of sent metrics type
     * @return the value of type in percentage
     */
    public String computePercentage(final long value, final long sentCount) {
        final double roundedPercentage = Math.round((value * 100) / (float) sentCount * 100.0) / 100.0;

        if (roundedPercentage == (long) roundedPercentage) {
            return String.format("%d%%", (long) roundedPercentage);
        } else {
            return String.format("%.2f%%", roundedPercentage);
        }
    }
}
