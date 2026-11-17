package com.tlc.crm.entity.validator.annotation.auditlog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation that is used the audit field
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditField {

    String aliasName();

    String displayName();
}
