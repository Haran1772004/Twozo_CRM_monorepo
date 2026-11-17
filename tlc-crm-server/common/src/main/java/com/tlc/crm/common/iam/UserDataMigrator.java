package com.tlc.crm.common.iam;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Uses for annotating the class to process the user data transfer related activities
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface UserDataMigrator {

    /**
     * <p>
     * Holds the name of the user data migrator
     * </p>
     *
     * @return the user data migrator name
     */
    String name();
}
