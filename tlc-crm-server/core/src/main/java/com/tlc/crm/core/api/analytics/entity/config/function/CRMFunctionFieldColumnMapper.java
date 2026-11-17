package com.tlc.crm.core.api.analytics.entity.config.function;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMFUNCTIONFIELDCOLUMNMAPPER;

import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the FunctionFieldColumnMapper
 * </p>
 *
 * @author saravanan
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table(name = CRMFUNCTIONFIELDCOLUMNMAPPER.TABLE)
public class CRMFunctionFieldColumnMapper extends CRMBaseModel {

    @Column(name = CRMFUNCTIONFIELDCOLUMNMAPPER.MODULAR_FIELD_ID)
    private Long fieldId;

    @Column(name = CRMFUNCTIONFIELDCOLUMNMAPPER.COLUMN_ID)
    private Long columnId;

    @Column(name = CRMFUNCTIONFIELDCOLUMNMAPPER.ORDER)
    private Long order;
}
