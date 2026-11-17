package com.tlc.crm.common.email.alert.annotation;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Annotation to designate a field for email alerts, enabling automatic inclusion of field values in email notification templates
 * and specifying their role in various email notification contexts.
 * </p>
 * <span style="color:red"><b>Note:</b></span
 *
 * <ul>
 *      <li>
 *          Use this annotation in the template fields that require it,
 *          Certain fields will not have the mentioned attribute, and all fields used in the class will come with this annotation.
 *      </li>
 * <ul>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailNotificationTemplate
 */
@Retention(RUNTIME)
@Target({FIELD})
public @interface EmailAlertField {

    /**
     * <p>
     * Specifies the name of the field to be used in the email template
     * </p>
     * <ul>
     *     <li> This attribute to compare the old value and the new value.Whenever there is a need to compare values for any field,
     *         this attribute should be utilized
     *     </li>
     * </ul>
     *
     * @return the name of the field
     */
    String name() default "";

    /**
     * <p>
     * Defines the types of email templates where this field's value is used for recipient information
     * </p>
     * <ul>
     *     <li>
     *         Attribute should be applied to any field that requires a recipient.
     *         Additionally, for any template that needs this attribute, the template name should be mentioned.
     *     </li>
     * </ul>
     *
     * @return an array of email notification templates
     */
    EmailNotificationTemplate[] recipientTemplates() default {};

    /**
     * <p>
     * Specifies the types of email templates where this field's value is used as a placeholder
     * </p>
     * <ul>
     *     <li>
     *         Attribute should be applied to any field that requires a placeholder,
     *         Only fields with this attribute will be considered as placeholder fields.
     *     </li>
     * </ul>
     *
     * @return an array of email notification templates
     */
    EmailNotificationTemplate[] placeholderTemplates() default {};

    /**
     * <p>
     * A flag indicating whether this field represents a user related attribute.
     * </p>
     * <ul>
     *     <li>
     *       If the 'isUserField' flag is set to true, indicate whether the field is for a user field,
     *       If not provided, default it to false, indicating that it is not a user field.
     *     </li>
     * </ul>
     *
     * @return true if the field represents a user attribute, false otherwise
     */
    boolean isUserField() default false;

    /**
     * <p>
     * Determines if the previous value is required to check whether a field modification
     * triggers an email notification.
     * </p>
     *
     * <ul>
     *      <li>
     *          If 'requirePreviousValue' is true, an email will be sent when the field changes.
     *          If false, no email will be triggered.
     *      </li>
     * </ul>
     *
     * @return true if the previous value is required to trigger an email, otherwise false
     */
    boolean requirePreviousValue() default false;

    /**
     * <p>
     * Determines the templates that require the previous value for processing
     * </p>
     *
     * @return an array of email notification templates
     */
    EmailNotificationTemplate[] requirePreviousValueFor() default {};

    /**
     * <p>
     * Determines the type of email alert field
     * </p>
     *
     *  <ul>
     *       <li>
     *  For example, if the field is a string or a number, it identifies the corresponding {@link  EmailAlertFieldType}
     *  as STRING
     *        </li>
     * </ul>
     *
     * @return Represents {@link EmailAlertFieldType}
     */
    EmailAlertFieldType fieldType();
}
