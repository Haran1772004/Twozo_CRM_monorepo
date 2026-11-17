package com.tlc.crm.common.iam.api.preferences.model.builder.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Provides the annotation for declaring the {@link com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder} related action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface PreferenceModelBuilder {

    /**
     * <p>
     * Provides the type of the builder.
     * </p>
     *
     * @return Type of the builder.
     */
    String type();
}
