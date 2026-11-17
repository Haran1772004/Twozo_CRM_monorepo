package com.tlc.crm.common.iam;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation for annotating the classes used to process organization initialization activities
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface OrgInitiator {

    /**
     * <p>
     * Holds the name of the org initiator.
     * </p>
     *
     * @return The org initiator name
     */
    String name();
}
