package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;

import com.tlc.validator.type.FutureTime;
import com.tlc.validator.type.Group;

import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;

/**
 * <p>
 * Represents the scheduled mail.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EntityScheduledEmail extends EntityOutboundEmail {

    @FutureTime(groups = Group.Create.class, message = "i18n.notifications.email.invalid.scheduled.time")
    private Long scheduledTime;

    private Boolean sendStatus;

    public Long getScheduledTime() {
        return scheduledTime;
    }

    public Boolean sendStatus() {
        return sendStatus;
    }

    public void setSendStatus(final Boolean sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setScheduledTime(final Long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * <p>
     * Checks whether the mail instance is scheduled or ordinary email
     * </p>
     *
     * @return true.
     */
    @Override
    public Boolean isScheduled() {
        return true;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return SCHEDULED;
    }

    /**
     * Represents the scheduled state.
     *
     * @author Kowsalya SP
     * @version 1.0
     */
    public enum State {

        NEW((short) 1),
        QUEUE((short) 2),
        FAILED((short) 3),
        ;

        final short id;

        State(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * Gets the scheduled state.
         *
         * @param id Represents the id of state
         * @return {@link State}
         */
        public static State get(final Short id) {
            return switch (id) {
                case 1 -> NEW;
                case 2 -> QUEUE;
                case 3 -> FAILED;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SCHEDULED_STATE);
            };
        }
    }
}
