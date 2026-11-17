package com.tlc.crm.core.features.internal.dataimport.validator.annotation;

import com.tlc.crm.core.features.internal.dataimport.validator.internal.SourceNoteValidatorImpl;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that is used to validate the {@link com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE_USE})
@Documented
@Constraint(validatedBy = {SourceNoteValidatorImpl.class})
public @interface SourceNoteValidator {

    String message() default "i18n.import.note.invalid.source";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
