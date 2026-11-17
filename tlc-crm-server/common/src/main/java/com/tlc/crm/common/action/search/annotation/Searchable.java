package com.tlc.crm.common.action.search.annotation;

import com.tlc.crm.common.models.SearchIndex;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotates the models to indicate that it's being used for search operations.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
public @interface Searchable {

    /**
     * <p>
     * Represents {@link SearchIndex}.
     * </p>
     *
     * @return {@link SearchIndex}.
     */
    SearchIndex index();
}