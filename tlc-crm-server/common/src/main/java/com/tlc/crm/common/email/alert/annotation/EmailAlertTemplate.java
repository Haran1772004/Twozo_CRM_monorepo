package com.tlc.crm.common.email.alert.annotation;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotation to designate a class as containing email alert template types
 * </p>
 *
 * <ul>
 *     <li>
 *         Specify all the template types required for the annotated model class
 *     </li>
 * </ul>
 *
 * <b>Example usage:</b>
 * <ul>
 *     <li>
 *         For a company model:
 *         <span style="color:red;">
 *         EmailNotificationTemplate(templates = {EmailNotificationTemplate.COMPANY_ASSIGNED, EmailNotificationTemplate.COMPANY_TRANSFERRED})
 *         </span>
 *     </li>
 * </ul>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailNotificationTemplate
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
public @interface EmailAlertTemplate {

    /**
     * <p>
     * Holds the collections of {@link EmailNotificationTemplate}
     * </p>
     *
     * @return The collections of {@link EmailNotificationTemplate}
     */
    EmailNotificationTemplate[] templates();
}