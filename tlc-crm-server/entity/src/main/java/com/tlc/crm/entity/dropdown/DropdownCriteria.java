package com.tlc.crm.entity.dropdown;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;

import java.util.Objects;

/**
 * Manages the {@link Criteria} related actions for dropdown.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class DropdownCriteria {

    private Object value;
    private CriteriaType criteriaType;

    public DropdownCriteria(final JsonObject jsonObject) {
        criteriaType = CriteriaType.getCriteriaType(jsonObject.getString(FieldKeys.TYPE.getName()));
        value = jsonObject.getString(FieldKeys.VALUE.getName());
    }

    public DropdownCriteria () {
    }

    /**
     * Sets the criteria type for dropdown actions.
     *
     * @param criteriaType criteriaType.
     */
    public void setCriteriaType(final String criteriaType) {
        this.criteriaType = CriteriaType.getCriteriaType(criteriaType);
    }

    /**
     * Sets the criteria value for dropdown actions.
     *
     * @param criteriaValue criteriaType.
     */
    public void setCriteriaValue(final Object criteriaValue) {
        this.value = criteriaValue;
    }

    /**
     * Returns the {@link Criteria} to the given column.
     *
     * @param column Which holds the column name.
     * @return {@link Criteria}.
     */
    public Criteria build(final Column column) {
        if (Objects.isNull(value)) {
            return Criteria.notEq(column, null);
        }
        final String trimmedValue = value.toString().trim();

        return switch (Objects.requireNonNullElse(criteriaType, CriteriaType.NOT_EQUAL)) {
            case CONTAINS -> Criteria.contains(column, trimmedValue);
            case STARTS_WITH -> Criteria.startsWith(column, trimmedValue);
            default -> Criteria.notEq(column, trimmedValue);
        };
    }

    /**
     * Gets the value
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets the {@link CriteriaType}
     *
     * @return {@link CriteriaType}
     */
    public CriteriaType getCriteriaType() {
        return criteriaType;
    }
}
