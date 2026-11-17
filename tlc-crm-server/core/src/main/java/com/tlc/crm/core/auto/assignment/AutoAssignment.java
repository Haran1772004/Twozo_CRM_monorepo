package com.tlc.crm.core.auto.assignment;

import com.tlc.crm.entity.fields.source.Source;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * Annotates the models to indicate models for use in auto-assignment operations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE_USE})
public @interface AutoAssignment {

    Source source();
}
