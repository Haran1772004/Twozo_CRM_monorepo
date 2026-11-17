package com.tlc.crm.core.api.analytics.entity.config.widget;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMKPICOMPAREFIELD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm widget comparator units
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMKPICOMPAREFIELD.TABLE )
public class WidgetCompareUnit extends CRMBaseModel
{
    @Column( name = CRMKPICOMPAREFIELD.COMPARE_UNIT_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.compare.unit.name" )
    @Size( max = 50, message = "i18n.crm.analytics.compare.unit.name.max.length" )
    private String compareUnitName;

    @Column( name = CRMKPICOMPAREFIELD.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.compare.unit.alias.name")
    @Size( max = 50, message = "i18n.crm.analytics.compare.unit.alias.name.max.length" )
    private String aliasName;
}