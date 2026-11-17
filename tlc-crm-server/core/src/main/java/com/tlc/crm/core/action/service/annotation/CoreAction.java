package com.tlc.crm.core.action.service.annotation;

import com.tlc.crm.core.action.service.CoreActionType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation for annotating core action
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
public @interface CoreAction {

    /**
     * <p>
     * Holds the name of email action type
     * </p>
     *
     * @return The {@link CoreActionType}
     */
    CoreActionType name();
}
