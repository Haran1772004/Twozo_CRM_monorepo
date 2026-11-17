package com.tlc.crm.notifications.api.model.alert;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * A notification alert which is sent to a particular user.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Setter
@Getter
@NoArgsConstructor
public abstract class SingleUserAlert extends NotificationAlert {

    private Long assigneeId;
    private boolean isRead;
}
