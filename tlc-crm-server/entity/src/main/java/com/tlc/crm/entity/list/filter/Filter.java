package com.tlc.crm.entity.list.filter;

import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;
import com.tlc.crm.entity.list.model.Provider;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Provides the annotation for declaring the filter related action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface Filter {

    /**
     * <p>
     * Provides the name of the source.
     * </p>
     *
     * @return name of the source.
     */
    FilterSourceType source();

    /**
     * <p>
     * Provides the name of the provider.
     * </p>
     *
     * @return name of the provider.
     */
    Provider provider() default Provider.DEFAULT;

    /**
     * <p>
     * Provides the array of name of the irrelevant sources.
     * </p>
     *
     * @return array of name of the irrelevant sources.
     */
    SourceLinkGroup[] sourceLinkGroups() default {};
}
