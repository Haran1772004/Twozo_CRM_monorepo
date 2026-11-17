package com.tlc.crm.core.notification.email.tracking.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.tracking.service.impl.EmailTrackingServiceImpl;
/**
 * <p>
 * Sets the email tracking related details into {@link EntityEmail}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface EmailTrackingService {

    static EmailTrackingService getInstance() {
        return EmailTrackingServiceImpl.getInstance();
    }

    /**
     * <p>
     * Sets the tracking details into {@link EntityEmail}
     * </p>
     *
     * @param entityEmail Represents the mail details
     * @param requestData Represents the request json
     */
    void setTrackingDetails(final EntityEmail entityEmail, final JsonObject requestData);

    /**
     * <p>
     * Removes the tracking details into {@link EntityEmail}
     * </p>
     *
     * @param entityEmail Represents the email details
     */
    void removeTrackingDetails(final EntityEmail entityEmail);

    /**
     * <p>
     * Removes tracking anchors and tracking pixel images from the provided HTML content.
     * This targets the tracker endpoints used by the system (e.g. /api/notifications/email/tracker)
     * and class/id based tracking wrappers.
     * </p>
     *
     * @param content raw HTML content
     * @return cleaned HTML content with tracking anchors/images removed (returns original if null)
     */
    String removeTrackingDetails(final String content);
}
