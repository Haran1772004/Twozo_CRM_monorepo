package com.tlc.crm.core.notification.email.tracking.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Represents the email tracking fields.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum EmailTrackingFields implements FieldNameHolder {

    TRACKER_TYPE("trackerType"),
    TRACKER_ID("trackerId"),
    EMAIL_TRACKING("emailTracking"),
    LINK("link"),
    SYNC_SERVICE("sync-service"),
    TRACKING_PIXEL("trackingPixel"),
    TRACKING("tracking"),
    ;

    private final String fieldName;

    EmailTrackingFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * {@inheritDoc}
     *
     * @return The field name
     */
    @Override
    public String getName() {
        return fieldName;
    }
}
