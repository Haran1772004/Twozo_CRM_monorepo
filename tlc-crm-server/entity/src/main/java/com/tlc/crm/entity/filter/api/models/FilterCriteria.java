package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A model that represents the criteria data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterCriteria {

    @NotNull(groups = Group.Delete.class, message = "i18n.filter.criteria.invalid.id")
    private Long id;
    @Valid
    private Comparator comparator;
    @Valid
    private CriteriaField criteriaField;
    @Valid
    private FieldSource fieldSource;
    private List<CriteriaValue> criteriaValues;
    private FilterConnector filterConnector;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public List<CriteriaValue> getCriteriaValues() {
        return criteriaValues;
    }

    public void setCriteriaValues(final List<CriteriaValue> criteriaValues) {
        this.criteriaValues = criteriaValues;
    }

    public void addCriteriaValue(final CriteriaValue criteriaValue) {
        if (Objects.isNull(criteriaValues)) {
            criteriaValues = new ArrayList<>();
        }
        criteriaValues.add(criteriaValue);
    }

    public FieldSource getFieldSource() {
        return fieldSource;
    }

    public void setFieldSource(final FieldSource fieldSource) {
        this.fieldSource = fieldSource;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setComparator(final Comparator comparator) {
        this.comparator = comparator;
    }

    public CriteriaField getCriteriaField() {
        return criteriaField;
    }

    public void setCriteriaField(final CriteriaField criteriaField) {
        this.criteriaField = criteriaField;
    }

    public FilterConnector getConnector() {
        return filterConnector;
    }

    public void setConnector(final FilterConnector filterConnector) {
        this.filterConnector = filterConnector;
    }

}
