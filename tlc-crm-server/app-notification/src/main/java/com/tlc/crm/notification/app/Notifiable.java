package com.tlc.crm.notification.app;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotates the class to process the notification related functions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
public @interface Notifiable {

    AppNotificationEventType[] types();

    CrmModelType modelName();

    boolean needHandling() default false;
}