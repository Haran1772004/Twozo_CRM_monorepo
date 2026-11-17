package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation denoting the authorizer for social chat.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SocialChatAuthorizer {

    /**
     * <p>
     * Gets the {@link Category.Type}
     * </p>
     *
     * @return {@link Category.Type}
     */
    Category.Type category() default Category.Type.ADMIN_ACCESS;

    /**
     * <p>
     * Gets the {@link SubCategory.Type}
     * </p>
     *
     * @return {@link SubCategory.Type}
     */
    SubCategory.Type subCategory() default SubCategory.Type.MANAGE_CONVERSATIONAL_CHANNELS;

    /**
     * <p>
     * Gets the {@link Action.Type}
     * </p>
     *
     * @return {@link Action.Type}
     */
    Action.Type action() default Action.Type.ACCESS;

    /**
     * <p>
     * Gets a flag that indicates whether the access needs common API handling or not.
     * </p>
     *
     * @return Common API handling requirement flag.
     */
    boolean needCommonApiHandling() default false;
}