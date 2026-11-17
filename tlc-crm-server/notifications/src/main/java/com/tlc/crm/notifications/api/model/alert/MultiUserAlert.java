package com.tlc.crm.notifications.api.model.alert;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A notification alert which is sent to multiple users in the same org.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class MultiUserAlert extends NotificationAlert {

    private Collection<Long> assigneeIds;
    private Map<Long, Boolean> usersReadMap;
}