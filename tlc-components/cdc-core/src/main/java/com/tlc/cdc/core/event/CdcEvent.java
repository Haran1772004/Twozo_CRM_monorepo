package com.tlc.cdc.core.event;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation for annotating the classes used to capture the CDC event.
 *
 * @author Thalamalai Pandiyan T
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface CdcEvent {


    String event();
}
