package com.tlc.crm.entity.dropdown.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation that is used to register the static dropdown field
 * </p>
 *
 * @author pandiaraj
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface StaticDropdown {

    String name();
}
