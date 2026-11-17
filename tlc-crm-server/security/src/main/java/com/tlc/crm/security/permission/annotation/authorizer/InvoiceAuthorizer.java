package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation denoting the authorizer for the invoice.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InvoiceAuthorizer {

    /**
     * <p>
     * Gets the {@link SubCategory.Type}
     * </p>
     *
     * @return {@link SubCategory.Type}
     */
    SubCategory.Type[] subCategories() default SubCategory.Type.COMMON;
}
