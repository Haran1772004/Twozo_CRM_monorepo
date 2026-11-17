package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting the authorizer for the settings
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingsAuthorizer {

    /**
     * <p>
     * Gets the {@link Category.Type}
     * </p>
     *
     * @return {@link Category.Type}
     */
    Category.Type category();

    /**
     * <p>
     * Gets the {@link SubCategory.Type}
     * </p>
     *
     * @return {@link SubCategory.Type}
     */
    SubCategory.Type subCategory();

    /**
     * <p>
     * Gets a flag that indicates whether the sub-category needs action(CRUD) handling or not.
     * </p>
     *
     * @return Action(CRUD) handling requirement flag.
     */
    boolean needActionHandling() default false;
}
