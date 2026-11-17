package com.tlc.notification.model.calendar;

import com.tlc.notification.model.email.EmailAddress;

import java.util.Objects;

/**
 * <p>
 * Represents the request details required to perform a calendar synchronization.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class CalendarSyncRequest {

    private final long startTime;
    private final long endTime;
    private final EmailAddress emailAddress;
    private final long orgId;

    private CalendarSyncRequest(final Builder builder) {
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.emailAddress = builder.emailAddress;
        this.orgId = builder.orgId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    public long getOrgId() {
        return orgId;
    }

    public String getEmailId() {
        return Objects.isNull(emailAddress) ? null : emailAddress.getEmailId();
    }

    /**
     * <p>
     * Builder class for creating instances of {@link CalendarSyncRequest}.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static class Builder {
        private long startTime;
        private long endTime;
        private long orgId;
        private EmailAddress emailAddress;

        /**
         * <p>Sets the start time of the synchronization range.</p>
         *
         * @param startTime the start time in epoch milliseconds
         * @return the current builder instance
         */
        public Builder startTime(final long startTime) {
            this.startTime = startTime;

            return this;
        }

        /**
         * <p>Sets the end time of the synchronization range.</p>
         *
         * @param endTime the end time in epoch milliseconds
         * @return the current builder instance
         */
        public Builder endTime(final long endTime) {
            this.endTime = endTime;

            return this;
        }

        /**
         * <p>
         * Sets the email address associated with this synchronization request.
         * </p>
         *
         * @param emailAddress the {@link EmailAddress} instance
         * @return the current builder instance
         */
        public Builder emailAddress(final EmailAddress emailAddress) {
            this.emailAddress = emailAddress;

            return this;
        }

        /**
         * <p>
         * Sets the organization ID for the synchronization request.
         * </p>
         *
         * @param orgId the organization ID
         * @return the current builder instance
         */
        public Builder orgId(final long orgId) {
            this.orgId = orgId;

            return this;
        }

        /**
         * <p>
         * Builds a new {@link CalendarSyncRequest} instance.
         * </p>
         *
         * @return a fully constructed {@link CalendarSyncRequest} instance
         */
        public CalendarSyncRequest build() {
            return new CalendarSyncRequest(this);
        }
    }
}