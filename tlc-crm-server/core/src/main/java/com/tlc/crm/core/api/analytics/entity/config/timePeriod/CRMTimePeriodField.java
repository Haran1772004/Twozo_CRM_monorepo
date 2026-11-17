package com.tlc.crm.core.api.analytics.entity.config.timePeriod;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTIMEPERIODFIELD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.i18n.I18nKey;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the time period fields details
 * </p>
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMTIMEPERIODFIELD.TABLE )
public class CRMTimePeriodField extends CRMBaseModel
{

    @Column( name = CRMTIMEPERIODFIELD.ALIAS_NAME )
    private String aliasName;

    @Column( name = CRMTIMEPERIODFIELD.DISPLAY_NAME )
    private I18nKey displayName;

    @Column( name = CRMTIMEPERIODFIELD.FUNCTION_ID )
    private Integer functionId;
}
