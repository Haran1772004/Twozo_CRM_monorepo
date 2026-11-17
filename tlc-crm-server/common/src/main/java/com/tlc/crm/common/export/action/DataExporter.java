package com.tlc.crm.common.export.action;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A registrar that is used to register the data export actions
 *
 * @author Selvakumar
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface DataExporter {

    /**
     * Holds the name of the data exporter.
     * This name has to be unique
     *
     * @return Data exporter name
     */
    String name();

}
