package com.tlc.crm.core.api.analytics.entity.integ.reports;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELDCOLUMNMAPPING;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents the column mapping for custom modular fields.
 * Maps custom modular fields to their corresponding analytics columns.
 *
 * @author karthick-v
 * @version 2.0
 */
@Getter
@SuperBuilder
@Table(name = CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE)
public class CRMCustomModularFieldColumnMapping extends CRMBaseModel {

    @Column(name = CRMCUSTOMMODULARFIELDCOLUMNMAPPING.MODULE_ID)
    private Integer moduleId;

    @Column(name = CRMCUSTOMMODULARFIELDCOLUMNMAPPING.FIELD_TYPE_ID)
    private Long fieldTypeId;

    @Column(name = CRMCUSTOMMODULARFIELDCOLUMNMAPPING.COLUMN_ID)
    private Long columnId;
}

