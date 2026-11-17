package com.tlc.crm.entity.audit.log.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.audit.log.fields.AuditEntryFields;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nKey;

import java.util.Objects;

/**
 * <p>
 * A model that represents the audit attribute.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public record AuditAttribute(I18nKey name, Object oldValue, Object newValue, AuditAction subAction, StoreType storeType) {

    public AuditAttribute(final I18nKey name, final Object oldValue, final Object newValue, final StoreType storeType) {
        this(name, oldValue, newValue, null, storeType);
    }

    /**
     * <p>
     * Builds {@link AuditAttribute} into {@link JsonObject}
     * </p>
     *
     * @return The {@link JsonObject}
     */
    public JsonObject buildJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AuditEntryFields.FIELD_NAME.getName(), name.getKey());
        jsonObject.put(AuditEntryFields.SUB_ACTION.getName(), Objects.isNull(subAction) ? Objects.isNull(oldValue)
                ? AuditAction.ADDED : Objects.isNull(newValue) ? AuditAction.REMOVED : AuditAction.UPDATED : subAction);
        jsonObject.put(AuditEntryFields.OLD_VALUE.getName(), oldValue);
        jsonObject.put(AuditEntryFields.NEW_VALUE.getName(), newValue);
        jsonObject.put(AuditEntryFields.STORE_TYPE.getName(), storeType.getName());

        return jsonObject;
    }
}
