package com.tlc.crm.email.notification.email.tracking.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.email.notification.email.tracking.status.TrackingErrorCodes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * A model that represents the {@link EmailTrackingDetail}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class EmailTrackingDetail {

    private Long id;
    private String trackingId;
    private Long time;
    private Long count;
    private Type type;

    /**
     * <p>
     * Represents the tracking type
     * </p>
     */
    public enum Type {

        OPENED((short) 1, "i18n.notifications.tracking.type.opened"),
        CLICKED((short) 2, "i18n.notifications.tracking.type.clicked"),
        OPENED_AND_CLICKED((short) 3, "i18n.notifications.tracking.type.opened.and.clicked"),
        NONE((short) 4, "i18n.notifications.tracking.type.none"),
        UNSUBSCRIBED((short) 5, "i18n.notifications.tracking.type.unsubscribed"),
        ;

        public final Short id;
        public final String name;

        Type(final Short id, final String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * <p>
         * Gets the tracking type
         * </p>
         *
         * @param emailTrackingDetails Which contains the collection of tracking details
         * @return The tracking {@link Type}
         */
        public static Type getType(final Collection<EmailTrackingDetail> emailTrackingDetails) {
            final Collection<Type> trackingTypes = emailTrackingDetails.stream()
                    .map(EmailTrackingDetail::getType).collect(Collectors.toSet());

            return 2 == trackingTypes.size() ? OPENED_AND_CLICKED :
                    Type.CLICKED == trackingTypes.stream().findFirst().get() ? CLICKED : OPENED;
        }

        public Short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static Type getType(final short id) {
            return switch (id) {
                case 1 -> OPENED;
                case 2 -> CLICKED;
                case 3 -> OPENED_AND_CLICKED;
                case 4 -> NONE;
                case 5 -> UNSUBSCRIBED;
                default -> throw ErrorCode.get(TrackingErrorCodes.INVALID_TRACKING_TYPE, "i18n.invalid.tracking.type");
            };
        }

        /**
         * <p>
         * Gets the relative tracking type ids based on the given type
         * </p>
         *
         * @param type Represents the {@link Type}
         * @return The collection of tracking ids
         */
        public static Collection<Short> getRelativeTypeIds(final Type type) {
            return switch (type) {
                case CLICKED -> List.of(CLICKED.getId(), OPENED_AND_CLICKED.getId());
                case OPENED -> List.of(OPENED.getId(), OPENED_AND_CLICKED.getId());
                default -> throw ErrorCode.get(TrackingErrorCodes.INVALID_TRACKING_TYPE, "i18n.invalid.tracking.type");
            };
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(final String trackingId) {
        this.trackingId = trackingId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(final Long time) {
        this.time = time;
    }

    public Long getCount() {
        return count;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setCount(final Long count) {
        this.count = count;
    }
}
