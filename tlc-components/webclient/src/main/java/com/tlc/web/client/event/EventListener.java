package com.tlc.web.client.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entry which is used to register the event listener
 *
 * @author Selvakumar
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

    /**
     * Describes the name of the event
     *
     * @return Name of the event
     */
    String name();

}
