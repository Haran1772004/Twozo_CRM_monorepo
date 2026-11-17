package com.tlc.crm.core.api.analytics.entity.integ.reports;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.crm.core.sql.resource.CRMFIELDCOLUMNMAPPING;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Manages CRM Filed
 * </p>
 *
 * @author James
 * @version 1.0
 */


@Getter
@SuperBuilder
@Table( name = CRMFIELDCOLUMNMAPPING.TABLE )
public class FieldColumnMapping extends CRMBaseModel
{
    @Column( name = CRMFIELDCOLUMNMAPPING.MODULAR_FIELD_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.modular.id" )
    private Long modularFieldId;

    @Column( name = CRMFIELDCOLUMNMAPPING.GROUP_BY_FIELD_ID )
    private Long groupByFieldId;

    @Column( name = CRMFIELDCOLUMNMAPPING.JOIN_FIELD_ID )
    private Long JoinFieldId;

    @Column( name = CRMFIELDCOLUMNMAPPING.SELECTOR_FIELD_ID )
    private Long selectorFieldId;

    @Column( name = CRMFIELDCOLUMNMAPPING.FILTER_FIELD_ID )
    private Long filterFieldId;

    @Column( name = CRMFIELDCOLUMNMAPPING.FILTER_VALUE )
    private String filterValue;

    @Column( name = CRMFIELDCOLUMNMAPPING.FIELD_CATEGORY )
    @NotNull( message = "i18n.crm.analytics.invalid.field.category" )
    @Getter( AccessLevel.NONE )
    private Long fieldCategory;

    @Column( name = CRMFIELDCOLUMNMAPPING.CATEGORY_LEVEL )
    @NotNull( message = "i18n.crm.analytics.invalid.category.level" )
    private Long categoryLevel;

    @Column( name = CRMFIELDCOLUMNMAPPING.CRM_FIELD_VALUE )
    private Long crmFieldValue;

    @Column( name = "IS_COLUMN_FILTER" )
    private boolean isColumnFilter;

    @Column( name = CRMFIELDCOLUMNMAPPING.DROPDOWN_FILTER_ID )
    private Long dropdownFilterId;

    @Column( name = CRMFIELDCOLUMNMAPPING.OPERATION_TYPE )
    private Long operationTypeId;

    @Column( name = CRMFIELDCOLUMNMAPPING.OPERATOR_TYPE )
    private Long operatorTypeId;

    @Column( name = CRMFIELDCOLUMNMAPPING.HAVING_OPERATION_TYPE )
    private Integer havingOperationType;

    @Column( name = CRMFIELDCOLUMNMAPPING.CASE_WHEN_CONFIG )
    private String configAsString;

}
