package com.tlc.crm.core.api.analytics.entity.config.fields;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMMEASUREFIELD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the crm field operation details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name= CRMMEASUREFIELD.TABLE )
public class FieldOperation extends CRMBaseModel
{
    @Column( name= CRMMEASUREFIELD.MEASURE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.measure" )
    private Integer measureId;

    @Column( name= CRMMEASUREFIELD.FIELD_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.field" )
    private Long fieldId;
}