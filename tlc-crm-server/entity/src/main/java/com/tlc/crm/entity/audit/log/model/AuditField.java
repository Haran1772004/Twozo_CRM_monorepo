package com.tlc.crm.entity.audit.log.model;

import com.tlc.i18n.I18nKey;

/**
 * <p>
 * A model that represents the audit field details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record AuditField(Long id, I18nKey displayName, String aliasName, boolean isNameAttribute) {
}
