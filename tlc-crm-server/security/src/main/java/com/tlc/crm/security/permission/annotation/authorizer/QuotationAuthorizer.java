package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting the authorizer for the quotations
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuotationAuthorizer {

    /**
     * <p>
     * Gets the {@link SubCategory.Type}
     * </p>
     *
     * @return {@link SubCategory.Type}
     */
    SubCategory.Type[] subCategories() default SubCategory.Type.COMMON;
}
