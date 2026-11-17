package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting the authorizer for the source followers.
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FollowerAuthorizer {

    /**
     * <p>
     * Gets the {@link SubCategory.Type}
     * </p>
     *
     * @return {@link SubCategory.Type}
     */
    SubCategory.Type subCategory() default SubCategory.Type.COMMON;

    /**
     * <p>
     * Gets the {@link Action.Type}
     * </p>
     *
     * @return {@link Action.Type}
     */
    Action.Type action() default Action.Type.COMMON;

    /**
     * <p>
     * Gets a flag that indicates whether the access needs common API handling or not.
     * </p>
     *
     * @return Common API handling requirement flag.
     */
    boolean needCommonApiHandling() default false;
}
