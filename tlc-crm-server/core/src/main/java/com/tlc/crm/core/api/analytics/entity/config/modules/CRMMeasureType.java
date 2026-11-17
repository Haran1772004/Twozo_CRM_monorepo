package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMMEASURETYPE;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm measure type details
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMMEASURETYPE.TABLE )
public class CRMMeasureType extends CRMBaseModel
{
    @Column( name = CRMMEASURETYPE.MEASURE_TYPE )
    @NotNull( message = "i18n.crm.analytics.invalid.measure.type" )
    @Size( max = 50, message = "i18n.crm.analytics.measure.type.name.max.length" )
    private String measureType;

    @Column( name =  CRMMEASURETYPE.IS_KPI )
    private boolean isKPI;

    @Column( name = CRMMEASURETYPE.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.period.alias.name" )
    @Size( max = 50, message = "i18n.crm.analytics.period.alias.name.max.length" )
    private String aliasName;

    @Column( name = CRMMEASURETYPE.IS_ENABLED )
    private boolean isEnabled;
}