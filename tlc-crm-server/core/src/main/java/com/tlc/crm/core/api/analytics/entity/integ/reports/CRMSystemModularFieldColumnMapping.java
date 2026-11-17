package com.tlc.crm.core.api.analytics.entity.integ.reports;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELDCOLUMNMAPPING;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Entity model for system modular-field column mappings used by analytics.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
@SuperBuilder
@Table(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE)
public class CRMSystemModularFieldColumnMapping extends CRMBaseModel {

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.MODULAR_FIELD_ID)
    private Long modularFieldId;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.JOIN_FIELD_ID)
    private Long joinFieldId;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.SELECTOR_FIELD_ID)
    private Long selectorFieldId;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FIELD_CATEGORY)
    private Integer fieldCategory;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.HAVING_OPERATION_TYPE)
    private Integer havingOperationType;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.CASE_WHEN_CONFIG)
    private String caseWhenConfig;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FILTER_FIELD_ID)
    private Long filterFieldId;

    private Long groupByFieldId;

    private String configAsString;

    private Long dropdownFilterId;

    private Integer operationTypeId;

    @Column(name = CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FILTER_VALUE)
    private String filterValue;

    private Long crmFieldValue;

    /**
     * Returns field category enum from persisted category identifier.
     *
     * @return mapped {@link FieldCategory}
     */
    public FieldCategory getFieldCategory()
    {
        return FieldCategory.getType(fieldCategory);
    }
}