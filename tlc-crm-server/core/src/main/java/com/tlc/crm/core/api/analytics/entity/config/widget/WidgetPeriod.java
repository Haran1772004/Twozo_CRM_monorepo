package com.tlc.crm.core.api.analytics.entity.config.widget;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMKPIPERIOD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm widget period details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMKPIPERIOD.TABLE )
public class WidgetPeriod extends CRMBaseModel
{
    @Column( name = CRMKPIPERIOD.PERIOD_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.period.name" )
    @Size( max = 50, message = "i18n.crm.analytics.period.name.max.length" )
    private String periodName;

    @Column( name = CRMKPIPERIOD.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.period.alias.name" )
    @Size( max = 50, message = "i18n.crm.analytics.period.alias.name.max.length" )
    private String aliasName;
}
