package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting the authorizer for the source
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SourceAuthorizer {

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
     * Gets the action type.
     * </p>
     *
     * @return {@link Action.Type}
     */
    Action.Type[] actions() default {};

    /**
     * <p>
     * Gets the related API type.
     * </p>
     *
     * @return {@link RelatedApiType}
     */
    RelatedApiType[] relatedApiTypes() default RelatedApiType.NONE;

    /**
     * <p>
     * Provides a flag that indicates whether the API needs to validate field permission or not.
     * </p>
     *
     * @return Field permission validation requirement flag.
     */
    boolean validateFieldPermission() default false;

    /**
     * <p>
     * Gets a flag that indicates whether the access needs common API handling or not.
     * </p>
     *
     * @return Common API handling requirement flag.
     */
    boolean needCommonApiHandling() default false;

    /**
     * <p>
     * Gets a flag that indicates whether the api needs dependent API handling or not.
     * </p>
     *
     * @return Dependent API handling requirement flag.
     */
    boolean needRelatedApiHandling() default false;

    /**
     * <p>
     * Provides the types of the dependent APIs.
     * </p>
     */
    enum RelatedApiType {

        NONE,
        ACTIVITY,
        TIMELINE,
        EMAIL,
        NOTE,
        FIELD,
        SOURCE_SETTINGS,
        SEARCH,
        HOME_PAGE
    }
}
