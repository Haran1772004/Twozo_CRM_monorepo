package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting the authorizer for the email
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailAuthorizer {

    /**
     * <p>
     * Gets the array of subcategories.
     * </p>
     *
     * @return array of subcategories.
     */
    SubCategory.Type[] subCategories();

    /**
     * <p>
     * Gets a flag that indicates whether the access needs unique API handling or not.
     * </p>
     *
     * @return Unique case API handling requirement flag.
     */
    boolean needUniqueApiHandling() default false;
}
