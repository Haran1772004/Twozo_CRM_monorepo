package com.tlc.crm.notifications.api.model.alert;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Pertains to new activity, reminder alert related fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Setter
@Getter
@NoArgsConstructor
public class ActivityAlert extends SingleUserAlert {

    private Long activityId;
}