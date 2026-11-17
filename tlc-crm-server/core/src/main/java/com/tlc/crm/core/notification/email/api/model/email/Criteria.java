package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.sql.common.api.dml.Column;

import java.util.Objects;

/**
 * <p>
 * Represents a dynamic filter criteria used for building SQL conditions.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class Criteria {

    private Object value;
    private CriteriaType criteriaType;

    /**
     * <p>
     * Constructs a {@link Criteria} object from the given JSON payload.
     * </p>
     *
     * @param jsonObject Represents the incoming filter definition.
     */
    public Criteria(final JsonObject jsonObject) {
        this.criteriaType = CriteriaType.getCriteriaType(jsonObject.getString(FieldKeys.TYPE.getName()));
        this.value = jsonObject.getString(FieldKeys.VALUE.getName());
    }

    /**
     * <p>
     * Builds a SQL {@link com.tlc.sql.common.api.dml.Criteria} object for the given column.
     * </p>
     *
     * @param column Represents the target database column.
     * @return Generated SQL {@link com.tlc.sql.common.api.dml.Criteria}.
     */
    public com.tlc.sql.common.api.dml.Criteria build(final Column column) {
        if (Objects.isNull(value)) {
            return com.tlc.sql.common.api.dml.Criteria.notEq(column, null);
        }
        final String trimmedValue = value.toString().trim();

        return switch (Objects.requireNonNullElse(criteriaType, CriteriaType.NOT_EQUAL)) {
            case CONTAINS -> com.tlc.sql.common.api.dml.Criteria.contains(column, trimmedValue);
            case STARTS_WITH -> com.tlc.sql.common.api.dml.Criteria.startsWith(column, trimmedValue);
            default -> com.tlc.sql.common.api.dml.Criteria.notEq(column, trimmedValue);
        };
    }

    /**
     * Returns criteria value.
     *
     * @return configured criteria value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Updates criteria value.
     *
     * @param value criteria value
     */
    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * Returns criteria type.
     *
     * @return configured {@link CriteriaType}
     */
    public CriteriaType getCriteriaType() {
        return criteriaType;
    }

    /**
     * Updates criteria type.
     *
     * @param criteriaType criteria type
     */
    public void setCriteriaType(final CriteriaType criteriaType) {
        this.criteriaType = criteriaType;
    }
}