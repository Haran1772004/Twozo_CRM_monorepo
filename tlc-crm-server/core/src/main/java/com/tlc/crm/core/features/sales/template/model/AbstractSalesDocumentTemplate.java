package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents the common template information shared by quotation, invoice,
 * and subscription templates.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentTemplate implements TlcModel {

    private Long id;
    private Long orgId;
    private String name;
    private String schemaJsonAsString;
    private JsonObject schemaJson;
    private CrmUser createdBy;
    private CrmUser updatedBy;
    private DateTime createdTime;
    private DateTime updatedTime;
    private boolean isDefault;

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSchemaJsonAsString() {
        return schemaJsonAsString;
    }

    public void setSchemaJsonAsString(final String schemaJsonAsString) {
        this.schemaJson = Json.object(schemaJsonAsString);
        this.schemaJsonAsString = schemaJsonAsString;
    }

    public JsonObject getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(final JsonObject schemaJson) {
        this.schemaJson = schemaJson;
        this.schemaJsonAsString = schemaJson.toString();
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean aDefault) {
        isDefault = aDefault;
    }

    @Override
    public Object identity() {
        return null;
    }

    public abstract Type getType();

    /**
     * <p>
     * Represents the type of sales template.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Type {

        QUOTATION(1),
        INVOICE(2),
        SUBSCRIPTION(3);

        private final int id;

        Type(final int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Type get(final int id) {

            for (final Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid sales template type: " + id);
        }
    }
}