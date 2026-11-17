package com.tlc.crm.entity.models.app;

import com.tlc.notification.model.app.AppNotifiableModel;

/**
 * <p>
 * A model the represents the crm app notifiable option
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface CrmAppNotifiableModel extends AppNotifiableModel {

    boolean notifiable();

    void setNotifiable(final boolean notifiable);
}
